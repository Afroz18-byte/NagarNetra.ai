package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class Base64IssueAnalysis(
    val category: String,
    val severity: Int,
    val description: String,
    val confidence: Int = 0,       // 0–100 how confident Gemini is
    val source: String = "gemini"  // "gemini" | "fallback"
)

class GeminiImageService {

    companion object {
        // ✅ FIX 1: Use latest model — gemini-1.5-flash is deprecated for vision on some keys
        private const val MODEL = "gemini-3.5-flash"
        private const val TAG = "GeminiImageService"

        private val VALID_CATEGORIES = setOf(
            "Pothole", "Water", "Lights", "Garbage", "Sewage", "Pedestrian", "Other"
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeBase64Image(base64Image: String): Base64IssueAnalysis = withContext(Dispatchers.IO) {

        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isNullOrEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder", ignoreCase = true)) {
            Log.w(TAG, "No valid API key found — using local fallback.")
            return@withContext getLocalFallback("Missing API key")
        }

        // ✅ FIX 2: Strip data URI prefix if present (e.g. "data:image/jpeg;base64,...")
        val rawBase64 = base64Image
            .substringAfter("base64,", base64Image)
            .replace("\\s".toRegex(), "")
            .trim()

        if (rawBase64.length < 100) {
            Log.e(TAG, "Base64 string too short (${rawBase64.length} chars) — image likely corrupt.")
            return@withContext getLocalFallback("Corrupt or empty image data")
        }

        // ✅ FIX 3: Auto-detect MIME type from base64 magic bytes
        val mimeType = detectMimeType(rawBase64)
        Log.d(TAG, "Detected MIME type: $mimeType | Base64 length: ${rawBase64.length}")

        // ✅ FIX 4: Rewritten prompt — explicit, structured, no ambiguity
        val prompt = """
You are an expert municipal infrastructure analyst for Bengaluru, India.
Carefully examine this image and identify the civic issue shown.

IMPORTANT RULES:
- Look for physical defects, hazards, or problems in the image
- Consider context: roads, drains, street lights, garbage, water pipes, footpaths
- Be specific in your description about what you actually see
- If the image shows a road crater, asphalt pit, pothole, or road surface damage, category must be Pothole

Respond ONLY with a raw JSON object — no markdown, no code blocks, no extra text:
{
  "category": "<one of: Pothole, Water, Lights, Garbage, Sewage, Pedestrian, Other>",
  "severity": <integer 1-10 where 1=minor cosmetic, 10=life-threatening emergency>,
  "description": "<2-3 sentence factual description of the exact problem visible>",
  "confidence": <integer 0-100 representing your confidence in this classification>
}

Severity guide:
1-3 = Minor (cosmetic damage, small pothole)
4-6 = Moderate (safety risk, service disruption)  
7-8 = Severe (immediate hazard, road impassable)
9-10 = Critical (flooding, exposed wires, gas leak, structural collapse)
        """.trimIndent()

        try {
            val partsArray = JSONArray().apply {
                put(JSONObject().put("text", prompt))
                // ✅ FIX 5: Use detected MIME type, not hardcoded image/jpeg
                put(JSONObject().put("inlineData", JSONObject().apply {
                    put("mimeType", mimeType)
                    put("data", rawBase64)
                }))
            }

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().put("parts", partsArray))
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    // ✅ FIX 6: temperature 0.0 for maximum determinism on structured output
                    put("temperature", 0.0)
                    put("maxOutputTokens", 256)
                })
                // ✅ FIX 7: Safety settings — allow civic content without triggering false blocks
                put("safetySettings", JSONArray().apply {
                    put(JSONObject().apply {
                        put("category", "HARM_CATEGORY_DANGEROUS_CONTENT")
                        put("threshold", "BLOCK_ONLY_HIGH")
                    })
                    put(JSONObject().apply {
                        put("category", "HARM_CATEGORY_HARASSMENT")
                        put("threshold", "BLOCK_ONLY_HIGH")
                    })
                })
            }.toString()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d(TAG, "Sending request to Gemini ($MODEL)...")
            val response = client.newCall(request).execute()
            val responseBodyStr = response.body?.string() ?: ""

            Log.d(TAG, "Response code: ${response.code}")

            if (!response.isSuccessful) {
                Log.e(TAG, "API error ${response.code}: $responseBodyStr")
                Log.w(TAG, "Model $MODEL failed — retrying with fallback model")
                return@withContext retryWithFallbackModel(rawBase64, mimeType, prompt, apiKey)
            }

            parseGeminiResponse(responseBodyStr)

        } catch (e: Exception) {
            Log.e(TAG, "Network/parse error: ${e.message}", e)
            getLocalFallback(e.message ?: "Unknown error")
        }
    }

    // ✅ FIX 9: Retry with older model if new model unavailable on this API key tier
    private suspend fun retryWithFallbackModel(
        rawBase64: String,
        mimeType: String,
        prompt: String,
        apiKey: String
    ): Base64IssueAnalysis = withContext(Dispatchers.IO) {
        val fallbackModel = "gemini-2.5-flash"
        Log.d(TAG, "Retrying with $fallbackModel...")

        try {
            val partsArray = JSONArray().apply {
                put(JSONObject().put("text", prompt))
                put(JSONObject().put("inlineData", JSONObject().apply {
                    put("mimeType", mimeType)
                    put("data", rawBase64)
                }))
            }
            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().put("parts", partsArray))
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.0)
                    put("maxOutputTokens", 256)
                })
            }.toString()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$fallbackModel:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful) parseGeminiResponse(body)
            else getLocalFallback("Fallback model also failed: ${response.code}")
        } catch (e: Exception) {
            getLocalFallback("Retry failed: ${e.message}")
        }
    }

    // ✅ FIX 10: Robust multi-strategy JSON extraction
    private fun parseGeminiResponse(responseBodyStr: String): Base64IssueAnalysis {
        return try {
            val jsonResponse = JSONObject(responseBodyStr)

            // Check for API-level error block
            val errorObj = jsonResponse.optJSONObject("error")
            if (errorObj != null) {
                Log.e(TAG, "Gemini returned error: ${errorObj.optString("message")}")
                return getLocalFallback("Gemini error: ${errorObj.optString("message")}")
            }

            val rawText = jsonResponse
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            if (rawText.isEmpty()) {
                Log.e(TAG, "Empty text in Gemini response. Full body: $responseBodyStr")
                return getLocalFallback("Empty Gemini response")
            }

            Log.d(TAG, "Raw Gemini text: $rawText")

            // Strategy 1: Direct JSON parse after stripping markdown
            val cleaned = rawText.trim()
                .replace("```json", "")
                .replace("```", "")
                .trim()

            // Strategy 2: Extract JSON object with regex if direct parse fails
            val jsonStr = if (cleaned.startsWith("{")) {
                cleaned
            } else {
                val match = Regex("\\{[^{}]*\\}", RegexOption.DOT_MATCHES_ALL).find(cleaned)
                match?.value ?: cleaned
            }

            val parsed = JSONObject(jsonStr)

            val rawCategory = parsed.optString("category", "Other").trim()
            val description = parsed.optString("description", "Issue identified by Gemini Vision AI.")
                .ifEmpty { "Issue identified by Gemini Vision AI." }
            val category = inferCivicCategory(rawCategory, description)

            val severity = parsed.optInt("severity", 5).coerceIn(1, 10)
            val confidence = parsed.optInt("confidence", 70).coerceIn(0, 100)
            Log.i(TAG, "✅ Parsed: category=$category | severity=$severity | confidence=$confidence%")

            Base64IssueAnalysis(
                category = category,
                severity = severity,
                description = description,
                confidence = confidence,
                source = "gemini"
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse failed: ${e.message} | Body: $responseBodyStr")
            getLocalFallback("Parse error: ${e.message}")
        }
    }

    // ✅ FIX 12: Detect actual image type from base64 magic bytes (not always JPEG)
    private fun detectMimeType(base64: String): String {
        return try {
            val sample = base64.take(16)
            val bytes = android.util.Base64.decode(sample, android.util.Base64.DEFAULT)
            when {
                bytes.size >= 3 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte()          -> "image/jpeg"

                bytes.size >= 8 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte()          -> "image/png"

                bytes.size >= 4 &&
                bytes[0] == 0x52.toByte() &&
                bytes[1] == 0x49.toByte() &&
                bytes[2] == 0x46.toByte() &&
                bytes[3] == 0x46.toByte()          -> "image/webp"

                else -> "image/jpeg" // safe default
            }
        } catch (e: Exception) {
            "image/jpeg"
        }
    }

    private fun getLocalFallback(reason: String = "Unknown"): Base64IssueAnalysis {
        Log.w(TAG, "Using local fallback. Reason: $reason")
        return Base64IssueAnalysis(
            category = "Other",
            severity = 5,
            description = "Could not analyze image automatically. Please select the category manually.",
            confidence = 0,
            source = "fallback"
        )
    }
}