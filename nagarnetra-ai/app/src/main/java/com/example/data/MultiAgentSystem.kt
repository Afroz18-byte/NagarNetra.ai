package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
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
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.*

// ─────────────────────────────────────────────
// AI HELPERS & MAPPERS
// ─────────────────────────────────────────────

fun inferCivicCategory(rawCategory: String, description: String = "", fallbackHint: String? = null): String {
    val visualText = "$rawCategory $description".uppercase().trim()
    val fallbackText = fallbackHint.orEmpty().uppercase()

    fun hasAny(text: String, terms: List<String>): Boolean = terms.any { text.contains(it) }
    fun hasRoadDamagePattern(text: String): Boolean {
        val roadTerms = listOf("ROAD", "ASPHALT", "TARMAC", "PAVEMENT", "CARRIAGEWAY", "LANE", "STREET")
        val damageTerms = listOf(
            "POTHOLE", "CRATER", "PIT", "ROAD DAMAGE", "ROAD SURFACE", "SURFACE DAMAGE",
            "BROKEN ROAD", "DAMAGED ROAD", "UNEVEN ROAD", "ROAD HOLE", "HOLE IN THE ROAD",
            "DEPRESSION", "SUNKEN", "CAVE-IN", "CAVE IN", "RUT", "ERODED", "CRACKED",
            "BROKEN ASPHALT", "DAMAGED ASPHALT", "SURFACE BREAK", "ROUGH PATCH"
        )
        return hasAny(text, damageTerms) || (hasAny(text, roadTerms) && hasAny(text, listOf("HOLE", "BROKEN", "DAMAGED", "UNEVEN", "CRACK", "CRATER", "PIT", "DEPRESSION", "SUNKEN", "ERODED")))
    }

    fun fromText(text: String): String? = when {
        text.isBlank() -> null
        hasAny(text, listOf("SEWAGE", "SEWER", "DRAIN", "MANHOLE", "OPEN MANHOLE", "WASTEWATER")) -> "Sewage"
        hasAny(text, listOf("GARBAGE", "DUMP", "TRASH", "WASTE", "LITTER", "RUBBISH", "DEBRIS", "BIN OVERFLOW")) -> "Garbage"
        hasAny(text, listOf("STREETLIGHT", "STREET LIGHT", "LIGHT POLE", "LAMP", "ELECTRIC", "WIRE", "POWER LINE")) -> "Lights"
        hasAny(text, listOf("WATER", "LEAK", "FLOOD", "PIPE", "POOLING", "STAGNANT WATER", "BURST PIPE")) -> "Water"
        hasAny(text, listOf("FOOTPATH", "PEDESTRIAN", "SIDEWALK", "ENCROACH", "WALKWAY")) -> "Pedestrian"
        hasRoadDamagePattern(text) -> "Pothole"
        else -> null
    }

    return fromText(visualText) ?: fromText(fallbackText) ?: "Other"
}

fun mapClaudeCategoryToStandard(claudeCat: String): String {
    return inferCivicCategory(claudeCat)
}

fun buildLocalTitle(category: String, riskLevel: String): String {
    return when (category.lowercase()) {
        "pothole"    -> "$riskLevel Pothole Hazard on Road Surface"
        "water"      -> "Water Supply Leakage Reported"
        "lights"     -> "Streetlight Outage Affecting Public Safety"
        "garbage"    -> "Uncleared Garbage Accumulation at Site"
        "sewage"     -> "Sewage Overflow Blocking Public Pathway"
        "pedestrian" -> "Pedestrian Infrastructure Damage Reported"
        else         -> "Civic Infrastructure Issue Requires Attention"
    }
}

// ─────────────────────────────────────────────
// AGENT COMMUNICATION CONTEXT & OUTPUTS
// ─────────────────────────────────────────────

data class AgentContext(
    val imageBase64: String? = null,
    val mimeType: String? = "image/jpeg",
    val manualCategory: String? = null,
    val latitude: Double,
    val longitude: Double,
    val locationNotes: String? = null,
    val existingIssues: List<Issue> = emptyList(),
    
    // Outputs accumulated step-by-step
    var locationResult: LocationResult? = null,
    var visionResult: VisionResult? = null,
    var categoryResult: CategoryResult? = null,
    var severityResult: SeverityResult? = null,
    var duplicateResult: DuplicateResult? = null,
    var routingResult: RoutingResult? = null,
    var emergencyResult: EmergencyResult? = null,
    
    // Human verification and flow control
    var requiresHumanVerification: Boolean = false,
    val verificationReasons: MutableList<String> = mutableListOf(),
    val traceLogs: MutableList<String> = mutableListOf()
)

data class LocationResult(
    val ward: String,
    val isValidLocation: Boolean,
    val locationContext: String,
    val confidence: Int,
    val fallbackUsed: Boolean
)

data class VisionResult(
    val detectedObjects: List<String>,
    val visualAnomalies: List<String>,
    val description: String,
    val confidence: Int,
    val fallbackUsed: Boolean
)

data class CategoryResult(
    val category: String,
    val confidence: Int,
    val fallbackUsed: Boolean
)

data class SeverityResult(
    val severity: Int,
    val riskLevel: String,
    val riskFactors: List<String>,
    val confidence: Int,
    val fallbackUsed: Boolean
)

data class DuplicateResult(
    val isDuplicate: Boolean,
    val duplicateTicketIds: List<String>,
    val clusterIds: List<String>,
    val reason: String,
    val confidence: Int,
    val fallbackUsed: Boolean
)

data class RoutingResult(
    val department: String,
    val expectedSlaDays: Int,
    val priorityScore: Int,
    val confidence: Int,
    val fallbackUsed: Boolean
)

data class EmergencyResult(
    val isEmergency: Boolean,
    val emergencyType: String,
    val escalationRequired: Boolean,
    val emergencyActionPlan: String,
    val confidence: Int,
    val fallbackUsed: Boolean
)

// ─────────────────────────────────────────────
// COGNITIVE GEMINI CLIENT FOR AGENTS
// ─────────────────────────────────────────────

class GeminiAgentClient {
    companion object {
        private const val TAG = "GeminiAgentClient"
        private const val PRIMARY_MODEL = "gemini-3.5-flash"
        private const val FALLBACK_MODEL = "gemini-2.5-flash"
        private const val FAST_MODEL = "gemini-2.5-flash-lite"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val INTERACTIONS_URL = "https://generativelanguage.googleapis.com/v1beta/interactions"

        val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    private fun isKeyValid(key: String?): Boolean {
        return !key.isNullOrEmpty() &&
            key != "MY_GEMINI_API_KEY" &&
            !key.contains("placeholder", ignoreCase = true) &&
            !key.contains("your-", ignoreCase = true)
    }

    suspend fun callAgent(
        prompt: String,
        systemInstruction: String,
        imageBase64: String? = null,
        mimeType: String? = null,
        responseJson: Boolean = true,
        fastMode: Boolean = false,
        maxOutputTokens: Int = if (fastMode) 256 else 512
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyValid(apiKey)) {
            Log.w(TAG, "No valid API key. Bypassing and triggering local agent fallback.")
            return@withContext null
        }

        val partsArray = JSONArray().apply {
            put(JSONObject().put("text", prompt))
            if (imageBase64 != null) {
                put(JSONObject().put("inlineData", JSONObject().apply {
                    put("mimeType", mimeType ?: "image/jpeg")
                    put("data", imageBase64)
                }))
            }
        }

        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().put("parts", partsArray))
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.0) // 0.0 for maximum determinism & structured JSON compliance
                put("maxOutputTokens", maxOutputTokens)
                if (responseJson) {
                    put("responseMimeType", "application/json")
                }
            })
            // Relaxed safety settings for civic visual analysis
            put("safetySettings", JSONArray().apply {
                listOf(
                    "HARM_CATEGORY_DANGEROUS_CONTENT",
                    "HARM_CATEGORY_HARASSMENT",
                    "HARM_CATEGORY_HATE_SPEECH",
                    "HARM_CATEGORY_SEXUALLY_EXPLICIT"
                ).forEach { cat ->
                    put(JSONObject().apply {
                        put("category", cat)
                        put("threshold", "BLOCK_ONLY_HIGH")
                    })
                }
            })
        }

        val requestBody = requestBodyJson.toString()

        val primaryModel = if (fastMode) FAST_MODEL else PRIMARY_MODEL
        val fallbackModel = FALLBACK_MODEL
        var result = executeHttpCall(primaryModel, requestBody, apiKey)
        if (result == null) {
            Log.w(TAG, "Primary model failed. Retrying with fallback model...")
            result = executeHttpCall(fallbackModel, requestBody, apiKey)
        }
        return@withContext result
    }

    suspend fun callVisionAgent(
        prompt: String,
        systemInstruction: String,
        imageBase64: String?,
        mimeType: String? = "image/jpeg",
        maxOutputTokens: Int = 768
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyValid(apiKey)) {
            Log.w(TAG, "No valid API key. Vision agent will use local image fallback.")
            return@withContext null
        }

        val cleanImage = imageBase64
            ?.substringAfter("base64,", imageBase64)
            ?.replace("\\s".toRegex(), "")
            ?.trim()
            ?.takeIf { it.length > 100 }

        if (cleanImage == null) {
            Log.w(TAG, "Vision image payload is missing or too small.")
            return@withContext null
        }

        val primaryInteractionBody = buildVisionInteractionRequest(
            model = PRIMARY_MODEL,
            prompt = prompt,
            systemInstruction = systemInstruction,
            imageBase64 = cleanImage,
            mimeType = mimeType ?: "image/jpeg",
            maxOutputTokens = maxOutputTokens
        )
        var result = executeInteractionVisionCall(primaryInteractionBody, apiKey)

        if (result == null) {
            val fallbackInteractionBody = buildVisionInteractionRequest(
                model = FALLBACK_MODEL,
                prompt = prompt,
                systemInstruction = systemInstruction,
                imageBase64 = cleanImage,
                mimeType = mimeType ?: "image/jpeg",
                maxOutputTokens = maxOutputTokens
            )
            result = executeInteractionVisionCall(fallbackInteractionBody, apiKey)
        }

        result ?: callAgent(
            prompt = prompt,
            systemInstruction = systemInstruction,
            imageBase64 = cleanImage,
            mimeType = mimeType,
            responseJson = true,
            fastMode = false,
            maxOutputTokens = maxOutputTokens
        )
    }

    private fun buildVisionInteractionRequest(
        model: String,
        prompt: String,
        systemInstruction: String,
        imageBase64: String,
        mimeType: String,
        maxOutputTokens: Int
    ): String {
        return JSONObject().apply {
            put("model", model)
            put("input", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", "$systemInstruction\n\n$prompt")
                })
                put(JSONObject().apply {
                    put("type", "image")
                    put("mime_type", mimeType)
                    put("data", imageBase64)
                })
            })
            put("generation_config", JSONObject().apply {
                put("temperature", 0.0)
                put("max_output_tokens", maxOutputTokens)
            })
            put("response_format", JSONObject().apply {
                put("type", "json_schema")
                put("json_schema", JSONObject().apply {
                    put("name", "civic_issue_analysis")
                    put("schema", buildVisionJsonSchema())
                })
            })
        }.toString()
    }

    private fun buildVisionJsonSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "object")
            put("additionalProperties", false)
            put("properties", JSONObject().apply {
                put("category", JSONObject().apply {
                    put("type", "string")
                    put("enum", JSONArray(listOf("Pothole", "Garbage", "Lights", "Water", "Sewage", "Pedestrian", "Other")))
                })
                put("severity", JSONObject().apply { put("type", "integer"); put("minimum", 1); put("maximum", 10) })
                put("description", JSONObject().apply { put("type", "string") })
                put("department", JSONObject().apply {
                    put("type", "string")
                    put("enum", JSONArray(listOf("PWD", "BWSSB", "BESCOM", "BBMP")))
                })
                put("riskLevel", JSONObject().apply {
                    put("type", "string")
                    put("enum", JSONArray(listOf("Low", "Medium", "High", "Critical")))
                })
                put("estimatedDays", JSONObject().apply { put("type", "string") })
                put("confidence", JSONObject().apply { put("type", "integer"); put("minimum", 0); put("maximum", 100) })
            })
            put("required", JSONArray(listOf("category", "severity", "description", "department", "riskLevel", "estimatedDays", "confidence")))
        }
    }

    private fun executeInteractionVisionCall(requestBody: String, apiKey: String): String? {
        return try {
            val request = Request.Builder()
                .url(INTERACTIONS_URL)
                .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", apiKey)
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (response.isSuccessful && body.isNotEmpty()) {
                extractInteractionText(body)
            } else {
                Log.w(TAG, "Interactions vision call failed ${response.code}: $body")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Interactions vision call exception: ${e.message}")
            null
        }
    }

    private fun extractInteractionText(body: String): String? {
        return try {
            val json = JSONObject(body)
            val direct = listOf("output_text", "outputText", "text")
                .firstNotNullOfOrNull { key -> json.optString(key).takeIf { it.isNotBlank() } }
            direct
                ?: json.optJSONArray("output")?.let { findTextInJson(it) }
                ?: json.optJSONObject("response")?.let { findTextInJson(it) }
                ?: json.optJSONArray("candidates")?.let { findTextInJson(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to parse interaction response text: ${e.message}")
            null
        }?.trim()
            ?.replace("```json", "")
            ?.replace("```", "")
            ?.trim()
    }

    private fun findTextInJson(value: Any?): String? {
        return when (value) {
            is JSONObject -> {
                listOf("output_text", "outputText", "text").firstNotNullOfOrNull { key ->
                    value.optString(key).takeIf { it.isNotBlank() }
                } ?: value.keys().asSequence().firstNotNullOfOrNull { key -> findTextInJson(value.opt(key)) }
            }
            is JSONArray -> (0 until value.length()).asSequence().firstNotNullOfOrNull { index -> findTextInJson(value.opt(index)) }
            else -> null
        }
    }
    private fun executeHttpCall(model: String, requestBody: String, apiKey: String): String? {
        return try {
            val url = "$BASE_URL/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful && body.isNotEmpty()) {
                val jsonResponse = JSONObject(body)
                val rawText = jsonResponse
                    .optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: ""

                if (rawText.isNotEmpty()) {
                    rawText.trim()
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()
                } else {
                    null
                }
            } else {
                Log.e(TAG, "API error ${response.code}: $body")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "executeHttpCall exception: ${e.message}", e)
            null
        }
    }
}

// ─────────────────────────────────────────────
// COGNITIVE CLAUDE CLIENT FOR VISION
// ─────────────────────────────────────────────

class ClaudeParseException(message: String) : Exception(message)

class ClaudeAgentClient {
    companion object {
        private const val TAG = "ClaudeAgentClient"
        private const val BASE_URL = "https://api.anthropic.com/v1/messages"
        private const val DEFAULT_MODEL = "claude-3-5-sonnet-20241022"

        val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.CLAUDE_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    private fun isKeyValid(key: String?): Boolean {
        return !key.isNullOrEmpty() && key != "MY_CLAUDE_API_KEY" && !key.contains("placeholder", ignoreCase = true)
    }

    suspend fun callAgent(
        prompt: String,
        imageBase64: String? = null,
        mimeType: String? = "image/jpeg"
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyValid(apiKey)) {
            Log.w(TAG, "No valid Claude API key. Falling back to Gemini Client.")
            return@withContext fallbackToGemini(prompt, imageBase64, mimeType)
        }

        try {
            val contentArray = JSONArray()

            if (imageBase64 != null) {
                // Wait! Let's build the correct image block structure:
                // imageBlock = { "type": "image", "source": { "type": "base64", "media_type": "image/jpeg", "data": "..." } }
                val imageBlock = JSONObject().apply {
                    put("type", "image")
                    val sourceObj = JSONObject().apply {
                        put("type", "base64")
                        put("media_type", mimeType ?: "image/jpeg")
                        val cleanB64 = imageBase64.trim().replace("\n", "").replace("\r", "")
                        put("data", cleanB64)
                    }
                    put("source", sourceObj)
                }
                contentArray.put(imageBlock)
            }

            val textBlock = JSONObject().apply {
                put("type", "text")
                put("text", prompt)
            }
            contentArray.put(textBlock)

            val messageObj = JSONObject().apply {
                put("role", "user")
                put("content", contentArray)
            }

            val messagesArray = JSONArray().put(messageObj)

            val requestBodyJson = JSONObject().apply {
                put("model", DEFAULT_MODEL)
                put("max_tokens", 1024)
                put("messages", messagesArray)
            }

            val request = Request.Builder()
                .url(BASE_URL)
                .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful && body.isNotEmpty()) {
                val jsonResponse = JSONObject(body)
                val contentArrayJson = jsonResponse.optJSONArray("content")
                val text = contentArrayJson?.optJSONObject(0)?.optString("text") ?: ""
                
                if (text.isNotEmpty()) {
                    text.trim()
                } else {
                    Log.e(TAG, "Empty text content from Claude. Response: $body")
                    null
                }
            } else {
                Log.e(TAG, "Claude API error ${response.code}: $body")
                fallbackToGemini(prompt, imageBase64, mimeType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Claude API exception: ${e.message}", e)
            fallbackToGemini(prompt, imageBase64, mimeType)
        }
    }

    private suspend fun fallbackToGemini(
        prompt: String,
        imageBase64: String?,
        mimeType: String?
    ): String? {
        Log.i(TAG, "Executing fallback Gemini call...")
        val geminiClient = GeminiAgentClient()
        return geminiClient.callAgent(
            prompt = prompt,
            systemInstruction = "You are VisionBot. Respond strictly in JSON format as requested.",
            imageBase64 = imageBase64,
            mimeType = mimeType
        )
    }
}

// ─────────────────────────────────────────────
// UNIFIED VISION ANALYSIS AGENT (Vision + Category + Severity + Emergency in ONE call)
// ─────────────────────────────────────────────
data class UnifiedAnalysisResult(
    val vision: VisionResult,
    val category: CategoryResult,
    val severity: SeverityResult,
    val emergency: EmergencyResult,
    val reportTitle: String
)

private data class LocalImageClue(
    val category: String,
    val confidence: Int,
    val description: String,
    val riskFactors: List<String>
)

class UnifiedVisionAgent(private val client: GeminiAgentClient) {
    suspend fun run(context: AgentContext): UnifiedAnalysisResult {
        val notes = context.locationNotes ?: ""

        val systemInstruction = """
            You are NagarNetra VisionBot, a strict municipal image classifier for authority reports.
            The uploaded image is the source of truth. Citizen text is context only, never category truth.

            Inspect visible evidence before choosing the category:
            - Pothole: pothole, road crater, asphalt pit, broken/eroded road, uneven road surface, road hole, sunken patch, cracked carriageway.
            - Garbage: dumped waste, trash pile, overflowing bin, litter, rubbish, debris pile.
            - Lights: broken street light, lamp outage, light pole damage, exposed electrical wires.
            - Water: water leakage, burst pipe, flooding, active water flow, stagnant water from supply leak.
            - Sewage: sewage overflow, drain/manhole issue, wastewater, open manhole.
            - Pedestrian: broken footpath, damaged sidewalk, walkway obstruction.
            - Other only when no specific civic issue is visible.

            Important: if a road surface defect is visible, category must be Pothole even if the defect is described as road damage or a crater.
            Do not return low confidence for a clearly visible pothole/road crater.
            Return only JSON with these exact keys: category, severity, description, department, riskLevel, estimatedDays, confidence.
        """.trimIndent()
        val prompt = "Citizen text notes only, not category truth: '${notes.ifBlank { "none" }}'. Classify from the image."

        context.traceLogs.add("VisionBot: Sending request to Gemini API...")
        val response = client.callVisionAgent(prompt, systemInstruction, context.imageBase64, context.mimeType)

        if (response == null) {
            context.traceLogs.add("VisionBot: AI unavailable. Using local fallback analysis.")
            return getFallback(context, "AI service unavailable")
        }

        context.traceLogs.add("VisionBot: Gemini response received.")

        return try {
            val cleaned = response.trim()
                .replace("```json", "")
                .replace("```", "")
                .trim()
            val jsonText = Regex("\\{.*}", RegexOption.DOT_MATCHES_ALL).find(cleaned)?.value ?: cleaned
            val json = JSONObject(jsonText)

            val rawIssueType = json.optString("category")
                .ifBlank { json.optString("issue_type") }
                .ifBlank { json.optString("civic_issue_type") }
                .ifBlank { "Other" }
            val rawConfidence = json.optInt("confidence", 75).coerceIn(0, 100)
            val rawDescription = json.optString("description")
                .ifBlank { json.optString("analysis") }
                .ifBlank { "Civic infrastructure issue detected from the submitted report evidence." }

            val initialCategory = inferCivicCategory(rawIssueType, rawDescription)
            val imageClue = if (initialCategory == "Other" || rawConfidence < 60 || rawDescription.isGenericVisionText()) {
                detectLocalImageClue(context.imageBase64)
            } else null
            val mappedCat = when {
                imageClue != null && (initialCategory == "Other" || rawConfidence < 50 || rawDescription.isGenericVisionText()) -> imageClue.category
                else -> initialCategory
            }
            val confidence = when {
                imageClue != null && mappedCat == imageClue.category -> maxOf(rawConfidence, imageClue.confidence)
                mappedCat != "Other" && rawConfidence < 70 -> 72
                mappedCat == "Other" -> rawConfidence.coerceAtMost(55)
                else -> rawConfidence
            }.coerceIn(0, 100)
            val description = if (imageClue != null && (rawDescription.isGenericVisionText() || initialCategory == "Other")) {
                imageClue.description
            } else {
                rawDescription
            }
            
            val severityInt = json.optInt("severity", if (mappedCat == "Pothole") 7 else 5).coerceIn(1, 10)
            val riskLevel = json.optString("riskLevel")
                .ifBlank {
                    when (severityInt) {
                        in 1..3 -> "Low"
                        in 4..6 -> "Medium"
                        in 7..8 -> "High"
                        else -> "Critical"
                    }
                }


            val vision = VisionResult(
                detectedObjects = listOf(mappedCat, rawIssueType).filter { it.isNotBlank() }.distinct(),
                visualAnomalies = listOf(description),
                description = description,
                confidence = confidence,
                fallbackUsed = false
            )
            val category = CategoryResult(mappedCat, confidence, fallbackUsed = false)
            val severity = SeverityResult(
                severity = severityInt,
                riskLevel = riskLevel,
                riskFactors = imageClue?.riskFactors ?: emptyList(),
                confidence = confidence,
                fallbackUsed = false
            )
            val isEmergency = severityInt >= 9
            val emergency = EmergencyResult(
                isEmergency = isEmergency,
                emergencyType = if (isEmergency) "Critical" else "None",
                escalationRequired = isEmergency,
                emergencyActionPlan = if (isEmergency) "Immediate emergency response required" else "Standard processing queue",
                confidence = confidence,
                fallbackUsed = false
            )
            val rawTitle = buildLocalTitle(mappedCat, riskLevel)

            context.traceLogs.add("[REPORT_TITLE]:$rawTitle")
            context.traceLogs.add("VisionBot: Visual analysis completed (Conf: $confidence%)")
            context.traceLogs.add("CategoryBot: Classified as $mappedCat")
            context.traceLogs.add("PriorityBot: Severity $severityInt/10 ($riskLevel)")
            context.traceLogs.add("EscalationBot: Emergency=${emergency.isEmergency}")

            UnifiedAnalysisResult(vision, category, severity, emergency, rawTitle)
        } catch (e: Exception) {
            context.traceLogs.add("VisionBot: JSON parse failed. Using local fallback analysis: ${e.message}")
            Log.e("UnifiedVisionAgent", "Vision JSON parse failed. Raw response: $response", e)
            getFallback(context, "AI response parse failed")
        }
    }

    private fun String.isGenericVisionText(): Boolean {
        val text = trim().lowercase()
        return text.length < 45 ||
            text.contains("civic infrastructure issue detected") ||
            text.contains("submitted report evidence") ||
            text.contains("unable to determine") ||
            text.contains("not enough information")
    }

    private fun detectLocalImageClue(imageBase64: String?): LocalImageClue? {
        val cleanImage = imageBase64
            ?.substringAfter("base64,", imageBase64)
            ?.replace("\\s".toRegex(), "")
            ?.trim()
            ?: return null

        return try {
            val bytes = Base64.decode(cleanImage, Base64.DEFAULT)
            val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val targetW = 96
            val targetH = max(72, (original.height * (targetW.toFloat() / original.width)).roundToInt())
            val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)

            val xStart = targetW / 10
            val xEnd = targetW - xStart
            val yStart = targetH / 4
            val yEnd = (targetH * 0.92f).roundToInt().coerceAtMost(targetH)
            val gridW = xEnd - xStart
            val gridH = yEnd - yStart
            if (gridW <= 4 || gridH <= 4) return null

            val luma = IntArray(gridW * gridH)
            var total = 0.0
            var neutralCount = 0
            var pixelCount = 0

            for (y in yStart until yEnd) {
                for (x in xStart until xEnd) {
                    val color = scaled.getPixel(x, y)
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    val lum = (0.299 * r + 0.587 * g + 0.114 * b).roundToInt()
                    luma[(y - yStart) * gridW + (x - xStart)] = lum
                    total += lum
                    if (abs(r - g) + abs(g - b) + abs(r - b) < 95) neutralCount++
                    pixelCount++
                }
            }

            val avg = total / pixelCount
            val darkThreshold = min(115.0, avg - 28.0)
            var darkCount = 0
            var veryDarkCount = 0
            var edgeCount = 0
            for (y in 0 until gridH) {
                for (x in 0 until gridW) {
                    val lum = luma[y * gridW + x]
                    if (lum < darkThreshold) darkCount++
                    if (lum < avg - 45.0 && lum < 95) veryDarkCount++
                    if (x + 1 < gridW && abs(lum - luma[y * gridW + x + 1]) > 34) edgeCount++
                    if (y + 1 < gridH && abs(lum - luma[(y + 1) * gridW + x]) > 34) edgeCount++
                }
            }

            val darkRatio = darkCount.toDouble() / pixelCount
            val veryDarkRatio = veryDarkCount.toDouble() / pixelCount
            val neutralRatio = neutralCount.toDouble() / pixelCount
            val edgeRatio = edgeCount.toDouble() / (pixelCount * 2.0)
            val roadLikeSurface = neutralRatio > 0.38 && avg in 45.0..210.0
            val hasIrregularDarkPatch = darkRatio in 0.035..0.38 && veryDarkRatio > 0.01 && edgeRatio > 0.035

            if (roadLikeSurface && hasIrregularDarkPatch) {
                LocalImageClue(
                    category = "Pothole",
                    confidence = 68,
                    description = "The uploaded photo contains a dark, irregular road-surface depression consistent with a pothole or asphalt crater. This should be routed to PWD for roadway repair and field verification.",
                    riskFactors = listOf("Local image check detected road-like surface", "Dark irregular depression pattern", "Gemini low-confidence guardrail")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("UnifiedVisionAgent", "Local image clue failed: ${e.message}")
            null
        }
    }

    private fun getFallback(context: AgentContext, reason: String): UnifiedAnalysisResult {
        val visualClue = detectLocalImageClue(context.imageBase64)
        val text = context.locationNotes.orEmpty().lowercase()
        val textCategory = when {
            text.contains("pothole") || text.contains("road") -> "Pothole"
            text.contains("water") || text.contains("leak") -> "Water"
            text.contains("light") || text.contains("electric") || text.contains("wire") -> "Lights"
            text.contains("garbage") || text.contains("trash") || text.contains("waste") -> "Garbage"
            text.contains("sewage") || text.contains("drain") || text.contains("manhole") -> "Sewage"
            text.contains("footpath") || text.contains("sidewalk") || text.contains("pedestrian") -> "Pedestrian"
            else -> "Other"
        }
        val category = visualClue?.category ?: textCategory
        val severityInt = when (category) {
            "Sewage" -> 9
            "Pothole", "Water", "Garbage" -> 8
            "Lights", "Pedestrian" -> 6
            else -> 5
        }
        val riskLevel = when (severityInt) {
            in 1..3 -> "Low"
            in 4..6 -> "Medium"
            in 7..8 -> "High"
            else -> "Critical"
        }
        val description = visualClue?.description ?: when (category) {
            "Pothole" -> "Road-surface damage was reported at the captured location. The issue can create vehicle swerving, traffic slowdown, and safety risk for two-wheelers."
            "Water" -> "A water leakage or flooding issue was reported at the captured location. The area should be inspected for pipe damage, pooling, and public access risk."
            "Lights" -> "A streetlight or electrical safety issue was reported at the captured location. The location should be inspected for outage, exposed wiring, or night-time safety risk."
            "Garbage" -> "Uncollected waste or dumping was reported at the captured location. The issue may affect sanitation, smell, and pedestrian access."
            "Sewage" -> "A sewage, drain, or manhole hazard was reported at the captured location. This requires fast inspection because it can create health and fall risks."
            "Pedestrian" -> "Pedestrian infrastructure damage was reported at the captured location. The issue may affect walking safety and accessibility."
            else -> "A civic infrastructure issue was reported at the captured location. The report has enough context for municipal triage and field verification."
        }
        val confidence = visualClue?.confidence ?: if (category == "Other") 35 else 58
        val isEmergency = category == "Sewage" && severityInt >= 9
        val vision = VisionResult(
            detectedObjects = listOf(category),
            visualAnomalies = listOf(reason),
            description = description,
            confidence = confidence,
            fallbackUsed = true
        )
        val categoryResult = CategoryResult(category, confidence, fallbackUsed = true)
        val severity = SeverityResult(
            severity = severityInt,
            riskLevel = riskLevel,
            riskFactors = visualClue?.riskFactors ?: listOf(reason, "Local deterministic civic triage"),
            confidence = confidence,
            fallbackUsed = true
        )
        val emergency = EmergencyResult(
            isEmergency = isEmergency,
            emergencyType = if (isEmergency) "Open Manhole/Sewage Hazard" else "None",
            escalationRequired = isEmergency,
            emergencyActionPlan = if (isEmergency) "Immediate BWSSB/ward engineer inspection and barricading required" else "Standard municipal routing",
            confidence = confidence,
            fallbackUsed = true
        )
        val title = buildLocalTitle(category, riskLevel)
        context.traceLogs.add("[REPORT_TITLE]:$title")
        context.traceLogs.add("VisionBot: Fallback analysis completed ($reason)")
        context.traceLogs.add("CategoryBot: Classified as $category")
        context.traceLogs.add("PriorityBot: Severity $severityInt/10 ($riskLevel)")
        context.traceLogs.add("EscalationBot: Emergency=${emergency.isEmergency}")
        return UnifiedAnalysisResult(vision, categoryResult, severity, emergency, title)
    }
}

class LocalLocationResolver {
    fun resolve(context: AgentContext): LocationResult {
        val lat = context.latitude
        val lng = context.longitude
        val notes = context.locationNotes?.lowercase() ?: ""

        val inBengaluru = lat in 12.0..14.0 && lng in 76.0..79.0
        val hasBengaluruKeyword = notes.contains("indiranagar") || notes.contains("koramangala") ||
            notes.contains("hsr") || notes.contains("whitefield") || notes.contains("ward") ||
            notes.contains("bengaluru") || notes.contains("bangalore") || notes.contains("jayanagar") ||
            notes.contains("mg road") || notes.contains("electronic city")

        // Compute ward number deterministically from lat/lng
        val wardNum = if (inBengaluru) ((Math.abs(lat * 1000).toInt() + Math.abs(lng * 1000).toInt()) % 198) + 1 else 80
        val wardName = resolveWardName(wardNum)

        return LocationResult(
            ward = "$wardName (Ward $wardNum)",
            isValidLocation = inBengaluru || hasBengaluruKeyword,
            locationContext = notes.ifBlank { "GPS coordinates processed" },
            confidence = if (inBengaluru) 95 else if (hasBengaluruKeyword) 80 else 60,
            fallbackUsed = true
        ).also {
            context.locationResult = it
            context.traceLogs.add("LocalLocationResolver: Resolved to ${it.ward} (Conf: ${it.confidence}%)")
        }
    }

    private fun resolveWardName(ward: Int): String {
        val knownWards = mapOf(
            80 to "Indiranagar", 68 to "Koramangala", 150 to "Whitefield",
            175 to "Electronic City", 19 to "Rajajinagar", 33 to "Malleshwaram",
            65 to "Jayanagar", 85 to "Banaswadi", 76 to "Shivajinagar"
        )
        return knownWards.entries.minByOrNull { Math.abs(it.key - ward) }?.value ?: "Bengaluru"
    }
}

// ─────────────────────────────────────────────
// LOCAL ROUTING RESOLVER (deterministic — no API)
// ─────────────────────────────────────────────
object LocalRoutingResolver {
    fun resolve(category: String, severity: Int): RoutingResult {
        val dept = when (category.lowercase()) {
            "water", "sewage"        -> "BWSSB"
            "pothole"                -> "PWD"
            "lights"                 -> "BESCOM"
            "gas"                    -> "GAIL"
            else                     -> "BBMP"
        }
        val slaDays = when (dept) {
            "BESCOM", "GAIL" -> 1
            "BWSSB", "BBMP"  -> if (severity >= 7) 1 else 2
            else             -> if (severity >= 7) 2 else 4
        }
        val priority = minOf(100, (severity * 9) + 10)
        return RoutingResult(department = dept, expectedSlaDays = slaDays, priorityScore = priority, confidence = 95, fallbackUsed = true)
    }
}

// ─────────────────────────────────────────────
// LOCAL DUPLICATE CHECKER (proximity math — no API)
// ─────────────────────────────────────────────
object LocalDuplicateChecker {
    fun check(context: AgentContext): DuplicateResult {
        val category = context.categoryResult?.category ?: context.manualCategory ?: "Other"
        val closest = context.existingIssues
            .filter { it.category.lowercase() == category.lowercase() && it.status != "Resolved" && !it.status.startsWith("Merged") }
            .map { it to haversine(context.latitude, context.longitude, it.latitude, it.longitude) }
            .filter { it.second <= 300.0 }
            .minByOrNull { it.second }

        return if (closest != null) {
            DuplicateResult(
                isDuplicate = true,
                duplicateTicketIds = listOf(closest.first.id),
                clusterIds = emptyList(),
                reason = "Existing $category report within ${closest.second.toInt()}m — Ticket ${closest.first.id}",
                confidence = 90,
                fallbackUsed = true
            )
        } else {
            DuplicateResult(
                isDuplicate = false,
                duplicateTicketIds = emptyList(),
                clusterIds = emptyList(),
                reason = "No matching active $category reports within 300m.",
                confidence = 95,
                fallbackUsed = true
            )
        }.also { context.duplicateResult = it }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3
        val phi1 = Math.toRadians(lat1); val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1); val dLam = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dPhi / 2).pow(2.0) + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLam / 2).pow(2.0)
        return r * 2 * Math.asin(Math.sqrt(a))
    }
}

// ─────────────────────────────────────────────
// LEGACY AGENT STUBS (kept for compatibility)
// ─────────────────────────────────────────────
class VisionAgent(private val client: GeminiAgentClient) {
    suspend fun run(context: AgentContext): VisionResult {
        if (context.imageBase64.isNullOrEmpty()) {
            return VisionResult(
                detectedObjects = emptyList(),
                visualAnomalies = emptyList(),
                description = "No image attached to report.",
                confidence = 100,
                fallbackUsed = false
            ).also { context.visionResult = it }
        }

        val systemInstruction = """
            You are VisionAgent of NagarNetra AI, an expert municipal image analyzer.
            Examine the photo representing a civic issue in Bengaluru.
            List visible objects and describe the physical defects, hazards, or infrastructure failures.
            You must return a raw JSON object matching this schema exactly:
            {
              "detectedObjects": ["string"],
              "visualAnomalies": ["string"],
              "description": "Factual 2-3 sentence description",
              "confidence": <integer 0-100>
            }
        """.trimIndent()

        val prompt = "Analyze the image. What civic anomalies or hazards are present?"
        val response = client.callVisionAgent(prompt, systemInstruction, context.imageBase64, context.mimeType)

        return if (response != null) {
            try {
                val json = JSONObject(response)
                val objects = json.getJSONArray("detectedObjects")
                val anomalies = json.getJSONArray("visualAnomalies")
                val desc = json.getString("description")
                val conf = json.getInt("confidence")

                VisionResult(
                    detectedObjects = List(objects.length()) { objects.getString(it) },
                    visualAnomalies = List(anomalies.length()) { anomalies.getString(it) },
                    description = desc,
                    confidence = conf,
                    fallbackUsed = false
                )
            } catch (e: Exception) {
                Log.e("VisionAgent", "Failed to parse JSON: $response", e)
                getFallback(context)
            }
        } else {
            getFallback(context)
        }.also {
            context.visionResult = it
            context.traceLogs.add("VisionAgent: Scanned image. Objects: ${it.detectedObjects.take(3)}. Description: ${it.description} (Conf: ${it.confidence}%)")
            if (it.confidence < 70) {
                context.requiresHumanVerification = true
                context.verificationReasons.add("VisionAgent confidence low (${it.confidence}%)")
            }
        }
    }

    private fun getFallback(context: AgentContext): VisionResult {
        val category = context.manualCategory ?: "Other"
        return VisionResult(
            detectedObjects = listOf("Civic site structure"),
            visualAnomalies = listOf("Local municipal issue"),
            description = "Factual details analyzed locally based on selected category: $category.",
            confidence = 65,
            fallbackUsed = true
        )
    }
}

// ─────────────────────────────────────────────
// 2. SEVERITY AGENT
// ─────────────────────────────────────────────
class SeverityAgent(private val client: GeminiAgentClient) {
    suspend fun run(context: AgentContext): SeverityResult {
        val category = context.categoryResult?.category ?: context.manualCategory ?: "Other"
        val visionDesc = context.visionResult?.description ?: "No image description."
        val notes = context.locationNotes ?: ""

        val systemInstruction = """
            You are SeverityAgent of NagarNetra AI. Your job is to assign a severity score (1-10) and threat risk level.
            Evaluate based on these rules:
            - 1-3 (Low): Minor cosmetic issues, small road cracks, standard trash.
            - 4-6 (Medium): Safety concerns, service interruption, large potholes, streetlight outages at night.
            - 7-8 (High): Major blockages, flooding, high road hazards, open drains.
            - 9-10 (Critical/Emergency): Open manhole, exposed live wire on footpath, gas leak, building collapse risk.
            Return a raw JSON object:
            {
              "severity": <integer 1-10>,
              "riskLevel": "<Low|Medium|High|Critical>",
              "riskFactors": ["string"],
              "confidence": <integer 0-100>
            }
        """.trimIndent()

        val prompt = "Category: $category. Vision Description: $visionDesc. Location Notes: $notes."
        val response = client.callAgent(prompt, systemInstruction)

        return if (response != null) {
            try {
                val json = JSONObject(response)
                val factors = json.getJSONArray("riskFactors")
                SeverityResult(
                    severity = json.getInt("severity").coerceIn(1, 10),
                    riskLevel = json.getString("riskLevel"),
                    riskFactors = List(factors.length()) { factors.getString(it) },
                    confidence = json.getInt("confidence"),
                    fallbackUsed = false
                )
            } catch (e: Exception) {
                Log.e("SeverityAgent", "JSON parse error: $response", e)
                getFallback(context)
            }
        } else {
            getFallback(context)
        }.also {
            context.severityResult = it
            context.traceLogs.add("SeverityAgent: Assigned severity ${it.severity}/10, Risk: ${it.riskLevel} (Conf: ${it.confidence}%)")
            if (it.confidence < 70) {
                context.requiresHumanVerification = true
                context.verificationReasons.add("SeverityAgent confidence low (${it.confidence}%)")
            }
        }
    }

    private fun getFallback(context: AgentContext): SeverityResult {
        val category = context.categoryResult?.category ?: context.manualCategory ?: "Other"
        val manualSeverity = if (category.lowercase() in listOf("sewage", "water")) 8 else 5
        return SeverityResult(
            severity = manualSeverity,
            riskLevel = if (manualSeverity >= 8) "High" else "Medium",
            riskFactors = listOf("Standard community safety hazard"),
            confidence = 70,
            fallbackUsed = true
        )
    }
}

// ─────────────────────────────────────────────
// 3. CATEGORY AGENT
// ─────────────────────────────────────────────
class CategoryAgent(private val client: GeminiAgentClient) {
    suspend fun run(context: AgentContext): CategoryResult {
        val manualCat = context.manualCategory ?: ""
        val visionDesc = context.visionResult?.description ?: ""

        val systemInstruction = """
            You are CategoryAgent of NagarNetra AI. Categorize this civic issue into exactly one of:
            - Pothole
            - Water
            - Lights
            - Garbage
            - Sewage
            - Pedestrian
            - Other
            Look at the user's manual choice and VisionAgent's visual description. Resolve conflicts intelligently.
            Return a raw JSON object:
            {
              "category": "<one of the valid categories listed above>",
              "confidence": <integer 0-100>
            }
        """.trimIndent()

        val prompt = "User Choice: $manualCat. Vision Description: $visionDesc."
        val response = client.callAgent(prompt, systemInstruction)

        return if (response != null) {
            try {
                val json = JSONObject(response)
                CategoryResult(
                    category = json.getString("category"),
                    confidence = json.getInt("confidence"),
                    fallbackUsed = false
                )
            } catch (e: Exception) {
                Log.e("CategoryAgent", "JSON parse error: $response", e)
                getFallback(context)
            }
        } else {
            getFallback(context)
        }.also {
            context.categoryResult = it
            context.traceLogs.add("CategoryAgent: Classified as category: ${it.category} (Conf: ${it.confidence}%)")
            
            // Check for conflict between manual input and AI classification
            if (manualCat.isNotEmpty() && !manualCat.equals(it.category, ignoreCase = true)) {
                context.requiresHumanVerification = true
                context.verificationReasons.add("Category conflict: user selected '$manualCat', AI classified as '${it.category}'")
            }
            if (it.confidence < 70) {
                context.requiresHumanVerification = true
                context.verificationReasons.add("CategoryAgent confidence low (${it.confidence}%)")
            }
        }
    }

    private fun getFallback(context: AgentContext): CategoryResult {
        val manual = context.manualCategory ?: ""
        val category = when {
            manual.isNotEmpty() -> manual
            context.visionResult?.description?.lowercase()?.contains("pothole") == true -> "Pothole"
            context.visionResult?.description?.lowercase()?.contains("garbage") == true -> "Garbage"
            context.visionResult?.description?.lowercase()?.contains("light") == true -> "Lights"
            context.visionResult?.description?.lowercase()?.contains("water") == true -> "Water"
            context.visionResult?.description?.lowercase()?.contains("sewage") == true -> "Sewage"
            else -> "Other"
        }
        return CategoryResult(
            category = category,
            confidence = 80,
            fallbackUsed = true
        )
    }
}

// ─────────────────────────────────────────────
// 4. LOCATION AGENT
// ─────────────────────────────────────────────
class LocationAgent(private val client: GeminiAgentClient) {
    suspend fun run(context: AgentContext): LocationResult {
        val systemInstruction = """
            You are LocationAgent of NagarNetra AI.
            Evaluate coordinates and location notes. Determine the ward in Bengaluru.
            Default ward to 'Indiranagar (Ward 80)' if it fits or is unspecified.
            Check if coordinates (lat, lng) are valid numbers. If the coordinates are near Bengaluru (approx 12.0 to 14.0 Lat, 76.0 to 79.0 Lng) OR the location notes/context mentions a Bengaluru neighborhood/ward (like Indiranagar, Koramangala, HSR Layout, MG Road, Whitefield, BBMP, Ward 80, Jayanagar, etc.), set isValidLocation to true.
            Return a raw JSON object:
            {
              "ward": "string",
              "isValidLocation": true|false,
              "locationContext": "string description",
              "confidence": <integer 0-100>
            }
        """.trimIndent()

        val prompt = "Latitude: ${context.latitude}, Longitude: ${context.longitude}. Notes: '${context.locationNotes}'."
        val response = client.callAgent(prompt, systemInstruction)

        return if (response != null) {
            try {
                val json = JSONObject(response)
                LocationResult(
                    ward = json.getString("ward"),
                    isValidLocation = json.getBoolean("isValidLocation"),
                    locationContext = json.getString("locationContext"),
                    confidence = json.optInt("confidence", 90),
                    fallbackUsed = false
                )
            } catch (e: Exception) {
                Log.e("LocationAgent", "JSON parse error: $response", e)
                getFallback(context)
            }
        } else {
            getFallback(context)
        }.also {
            context.locationResult = it
            context.traceLogs.add("LocationAgent: Resolved ward: ${it.ward}, Valid: ${it.isValidLocation} (Conf: ${it.confidence}%)")
            if (!it.isValidLocation) {
                context.requiresHumanVerification = true
                context.verificationReasons.add("Location is reported as invalid or outside Bengaluru boundaries")
            }
            if (it.confidence < 70) {
                context.requiresHumanVerification = true
                context.verificationReasons.add("LocationAgent confidence low (${it.confidence}%)")
            }
        }
    }

    private fun getFallback(context: AgentContext): LocationResult {
        val notes = context.locationNotes?.lowercase() ?: ""
        val hasBengaluruKeyword = notes.contains("indiranagar") || notes.contains("koramangala") ||
                notes.contains("hsr") || notes.contains("mg road") || notes.contains("ward") ||
                notes.contains("bengaluru") || notes.contains("bangalore") || notes.contains("jayanagar")
        
        val latOk = context.latitude in 12.0..14.0
        val lngOk = context.longitude in 76.0..79.0
        return LocationResult(
            ward = "Indiranagar (Ward 80)",
            isValidLocation = (latOk && lngOk) || hasBengaluruKeyword || (context.latitude == 0.0 && context.longitude == 0.0),
            locationContext = context.locationNotes ?: "Processed using device GPS coordinates",
            confidence = 95,
            fallbackUsed = true
        )
    }
}

// ─────────────────────────────────────────────
// 5. DUPLICATE DETECTION AGENT
// ─────────────────────────────────────────────
class DuplicateDetectionAgent(private val client: GeminiAgentClient) {
    suspend fun run(context: AgentContext): DuplicateResult {
        val category = context.categoryResult?.category ?: context.manualCategory ?: "Other"
        val candidates = context.existingIssues.filter { issue ->
            if (issue.status == "Resolved" || issue.status.startsWith("Merged")) return@filter false
            if (issue.category.lowercase() != category.lowercase()) return@filter false
            
            // Proximity filter: within 500 meters
            val distance = haversine(context.latitude, context.longitude, issue.latitude, issue.longitude)
            distance <= 500.0
        }

        if (candidates.isEmpty()) {
            return DuplicateResult(
                isDuplicate = false,
                duplicateTicketIds = emptyList(),
                clusterIds = emptyList(),
                reason = "No active reports of category $category within 500 meters.",
                confidence = 100,
                fallbackUsed = false
            ).also { context.duplicateResult = it }
        }

        val candidatesListStr = candidates.joinToString("\n") {
            "- ID: ${it.id}, Description: ${it.description}, Lat/Lng: (${it.latitude}, ${it.longitude})"
        }

        val systemInstruction = """
            You are DuplicateDetectionAgent of NagarNetra AI.
            Your job is to compare a new report to a list of nearby unresolved candidates.
            Decide if the new issue is a duplicate of one candidate, or if it should be clustered together.
            Return a raw JSON object:
            {
              "isDuplicate": true|false,
              "duplicateTicketIds": ["string"],
              "clusterIds": ["string"],
              "reason": "Clear explanation of duplicate matching or clustering",
              "confidence": <integer 0-100>
            }
        """.trimIndent()

        val prompt = """
            New Issue Category: $category
            New Lat/Lng: (${context.latitude}, ${context.longitude})
            New Description: ${context.visionResult?.description}
            
            Nearby Candidates:
            $candidatesListStr
        """.trimIndent()

        val response = client.callAgent(prompt, systemInstruction)

        return if (response != null) {
            try {
                val json = JSONObject(response)
                val dupIds = json.getJSONArray("duplicateTicketIds")
                val clusIds = json.getJSONArray("clusterIds")
                DuplicateResult(
                    isDuplicate = json.getBoolean("isDuplicate"),
                    duplicateTicketIds = List(dupIds.length()) { dupIds.getString(it) },
                    clusterIds = List(clusIds.length()) { clusIds.getString(it) },
                    reason = json.getString("reason"),
                    confidence = json.getInt("confidence"),
                    fallbackUsed = false
                )
            } catch (e: Exception) {
                Log.e("DuplicateAgent", "JSON parse error: $response", e)
                getFallback(context, candidates)
            }
        } else {
            getFallback(context, candidates)
        }.also {
            context.duplicateResult = it
            context.traceLogs.add("DuplicateDetectionAgent: Duplicate found: ${it.isDuplicate}. Merge ID: ${it.duplicateTicketIds.firstOrNull()} (Conf: ${it.confidence}%)")
            if (it.confidence < 70) {
                context.requiresHumanVerification = true
                context.verificationReasons.add("DuplicateDetectionAgent confidence low (${it.confidence}%)")
            }
        }
    }

    private fun getFallback(context: AgentContext, candidates: List<Issue>): DuplicateResult {
        val closest = candidates.minByOrNull {
            haversine(context.latitude, context.longitude, it.latitude, it.longitude)
        }
        return if (closest != null) {
            DuplicateResult(
                isDuplicate = true,
                duplicateTicketIds = listOf(closest.id),
                clusterIds = emptyList(),
                reason = "Rule-based fallback: Category matches and distance is under 500m to ticket ${closest.id}.",
                confidence = 80,
                fallbackUsed = true
            )
        } else {
            DuplicateResult(
                isDuplicate = false,
                duplicateTicketIds = emptyList(),
                clusterIds = emptyList(),
                reason = "No candidates found within rule boundary.",
                confidence = 90,
                fallbackUsed = true
            )
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLam = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dPhi / 2).pow(2.0) + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLam / 2).pow(2.0)
        return r * 2 * asin(sqrt(a))
    }
}

// ─────────────────────────────────────────────
// LEGACY DUPLICATE AGENT (kept for compat, uses local logic)
// ─────────────────────────────────────────────

// ─────────────────────────────────────────────
// 6. ROUTING AGENT
// ─────────────────────────────────────────────
class RoutingAgent(private val client: GeminiAgentClient) {
    suspend fun run(context: AgentContext): RoutingResult {
        val category = context.categoryResult?.category ?: context.manualCategory ?: "Other"
        val severity = context.severityResult?.severity ?: 5
        val dupMsg = context.duplicateResult?.reason ?: ""

        val systemInstruction = """
            You are RoutingAgent of NagarNetra AI.
            Route this civic issue to the correct department (PWD, BWSSB, BESCOM, BBMP, or GAIL).
            Assign standard SLA expected days:
            - BESCOM/GAIL: 1 day
            - BWSSB/BBMP: 1-3 days
            - PWD: 3-5 days
            Compute priorityScore (0-100) based on severity and community impact.
            Return a raw JSON object:
            {
              "department": "<PWD|BWSSB|BESCOM|BBMP|GAIL>",
              "expectedSlaDays": <integer>,
              "priorityScore": <integer 0-100>,
              "confidence": <integer 0-100>
            }
        """.trimIndent()

        val prompt = "Category: $category, Severity: $severity, Duplicate Context: $dupMsg"
        val response = client.callAgent(prompt, systemInstruction)

        return if (response != null) {
            try {
                val json = JSONObject(response)
                RoutingResult(
                    department = json.getString("department"),
                    expectedSlaDays = json.getInt("expectedSlaDays"),
                    priorityScore = json.getInt("priorityScore"),
                    confidence = json.getInt("confidence"),
                    fallbackUsed = false
                )
            } catch (e: Exception) {
                Log.e("RoutingAgent", "JSON parse error: $response", e)
                getFallback(context)
            }
        } else {
            getFallback(context)
        }.also {
            context.routingResult = it
            context.traceLogs.add("RoutingAgent: Assigned to ${it.department} (SLA: ${it.expectedSlaDays}d, Priority: ${it.priorityScore}/100, Conf: ${it.confidence}%)")
            if (it.confidence < 70) {
                context.requiresHumanVerification = true
                context.verificationReasons.add("RoutingAgent confidence low (${it.confidence}%)")
            }
        }
    }

    private fun getFallback(context: AgentContext): RoutingResult {
        val category = context.categoryResult?.category ?: context.manualCategory ?: "Other"
        val severity = context.severityResult?.severity ?: 5
        val dept = when (category.lowercase()) {
            "water", "sewage" -> "BWSSB"
            "pothole", "roads" -> "PWD"
            "lights", "electricity" -> "BESCOM"
            "gas" -> "GAIL"
            else -> "BBMP"
        }
        val priority = (severity * 8) + 10
        return RoutingResult(
            department = dept,
            expectedSlaDays = if (dept in listOf("BESCOM", "GAIL")) 1 else 3,
            priorityScore = min(100, priority),
            confidence = 80,
            fallbackUsed = true
        )
    }
}

// ─────────────────────────────────────────────
// 7. EMERGENCY AGENT
// ─────────────────────────────────────────────
class EmergencyAgent(private val client: GeminiAgentClient) {
    suspend fun run(context: AgentContext): EmergencyResult {
        val category = context.categoryResult?.category ?: context.manualCategory ?: "Other"
        val severity = context.severityResult?.severity ?: 5
        val description = context.visionResult?.description ?: ""

        val systemInstruction = """
            You are EmergencyAgent of NagarNetra AI.
            Evaluate whether this report represents a life-threatening municipal emergency.
            Examples of emergencies:
            - Exposed live hanging electric wires (BESCOM)
            - Gas main pipeline leak (GAIL)
            - Completely open deep manhole on active footpath/road (BWSSB)
            - Major building structural collapse warning (BBMP)
            Return a raw JSON object:
            {
              "isEmergency": true|false,
              "emergencyType": "<None|Gas Leak|Live Wire|Open Manhole|Structural Collapse|Flooding>",
              "escalationRequired": true|false,
              "emergencyActionPlan": "Instructions for emergency response dispatch",
              "confidence": <integer 0-100>
            }
        """.trimIndent()

        val prompt = "Category: $category. Severity: $severity. Description: $description."
        val response = client.callAgent(prompt, systemInstruction)

        return if (response != null) {
            try {
                val json = JSONObject(response)
                EmergencyResult(
                    isEmergency = json.getBoolean("isEmergency"),
                    emergencyType = json.getString("emergencyType"),
                    escalationRequired = json.getBoolean("escalationRequired"),
                    emergencyActionPlan = json.getString("emergencyActionPlan"),
                    confidence = json.getInt("confidence"),
                    fallbackUsed = false
                )
            } catch (e: Exception) {
                Log.e("EmergencyAgent", "JSON parse error: $response", e)
                getFallback(context)
            }
        } else {
            getFallback(context)
        }.also {
            context.emergencyResult = it
            context.traceLogs.add("EmergencyAgent: Emergency identified: ${it.isEmergency} (${it.emergencyType}) (Conf: ${it.confidence}%)")
            
            if (it.isEmergency) {
                context.requiresHumanVerification = true // Emergencies always get flagged for double human verification
                context.verificationReasons.add("Critical Emergency flagged: ${it.emergencyType}")
            }
            if (it.confidence < 70) {
                context.requiresHumanVerification = true
                context.verificationReasons.add("EmergencyAgent confidence low (${it.confidence}%)")
            }
        }
    }

    private fun getFallback(context: AgentContext): EmergencyResult {
        val category = context.categoryResult?.category ?: context.manualCategory ?: "Other"
        val severity = context.severityResult?.severity ?: 5
        val openManhole = category.lowercase() == "sewage" && severity >= 9
        val liveWire = category.lowercase() == "lights" && severity >= 9
        val gasLeak = category.lowercase() == "gas" || category.lowercase() == "other" && severity >= 9
        val isEmergency = openManhole || liveWire || gasLeak

        return EmergencyResult(
            isEmergency = isEmergency,
            emergencyType = when {
                openManhole -> "Open Manhole"
                liveWire -> "Live Wire"
                gasLeak -> "Gas Leak"
                else -> "None"
            },
            escalationRequired = isEmergency,
            emergencyActionPlan = if (isEmergency) "Immediate dispatch via emergency hotline" else "Standard queue",
            confidence = 85,
            fallbackUsed = true
        )
    }
}

// ─────────────────────────────────────────────
// 8. CITIZEN ASSISTANT (Chatbot Agent)
// ─────────────────────────────────────────────
object CivicAssistantPolicy {
    private val civicKeywords = listOf(
        "report", "ticket", "status", "issue", "map", "location", "gps", "ward",
        "department", "route", "pothole", "road", "water", "leak", "garbage",
        "waste", "light", "streetlight", "sewage", "drain", "footpath",
        "pedestrian", "upvote", "points", "reward", "trust", "escalat",
        "urgent", "emergency", "resolved", "verified", "progress", "bbmp",
        "pwd", "bwssb", "bescom", "municipal", "civic", "nagarnetra",
        "who", "what", "explain", "smart", "city", "assistant", "companion", "name"
    )

    private val broadAnswerMarkers = listOf(
        "break the topic", "share a little more detail", "main idea",
        "smallest first step", "cause, context, and impact", "i can help with that"
    )

    fun isCivicQuery(query: String, allIssues: List<Issue>): Boolean {
        return true
    }

    fun enforceScope(userQuery: String, answer: String, allIssues: List<Issue>): String {
        val trimmed = answer.trim()
        if (trimmed.isBlank()) return fallback(userQuery, allIssues)
        return trimmed
    }

    fun fallback(query: String, allIssues: List<Issue>): String {
        val q = query.lowercase().trim()
        val match = allIssues.find { q.contains(it.id.lowercase()) }
        if (match != null) {
            return "Ticket ${match.id}: ${match.title}\nStatus: ${match.status}\nCategory: ${match.category}\nDepartment: ${match.assignedDept}\nSeverity: ${match.severity}/10\nLocation: ${match.locationName}"
        }

        // Handle count / database questions directly to give dynamic accurate results
        if (q.contains("how many") || q.contains("count") || q.contains("number of")) {
            val totalIssues = allIssues.size
            val potholeCount = allIssues.count { it.category.equals("Pothole", ignoreCase = true) || it.title.contains("pothole", ignoreCase = true) }
            val waterCount = allIssues.count { it.category.equals("Water", ignoreCase = true) || it.title.contains("water", ignoreCase = true) }
            val lightsCount = allIssues.count { it.category.equals("Lights", ignoreCase = true) || it.title.contains("light", ignoreCase = true) }
            val garbageCount = allIssues.count { it.category.equals("Garbage", ignoreCase = true) || it.title.contains("garbage", ignoreCase = true) }
            val sewageCount = allIssues.count { it.category.equals("Sewage", ignoreCase = true) || it.title.contains("sewage", ignoreCase = true) }
            val pedestrianCount = allIssues.count { it.category.equals("Pedestrian", ignoreCase = true) || it.title.contains("footpath", ignoreCase = true) }
            
            return when {
                q.contains("pothole") || q.contains("road") -> 
                    "There are currently $potholeCount reported pothole issue(s) in Bengaluru according to our live civic database."
                q.contains("water") || q.contains("leak") -> 
                    "There are currently $waterCount reported water leakage/supply issue(s) in our database."
                q.contains("light") || q.contains("electricity") || q.contains("street") -> 
                    "There are currently $lightsCount reported streetlight/electricity issue(s) tracked in our system."
                q.contains("garbage") || q.contains("waste") || q.contains("trash") -> 
                    "There are currently $garbageCount reported garbage/waste disposal issue(s) pending attention."
                q.contains("sewage") || q.contains("drain") -> 
                    "There are currently $sewageCount reported sewage or drainage blockage issue(s)."
                q.contains("pedestrian") || q.contains("footpath") -> 
                    "There are currently $pedestrianCount reported footpath/pedestrian issue(s)."
                else -> 
                    "Currently, NagarNetra is tracking a total of $totalIssues reported issue(s) across Bengaluru, including $potholeCount potholes, $waterCount water issues, $lightsCount streetlights, and $garbageCount garbage reports."
            }
        }

        val categoryMatches = allIssues.filter { issue ->
            q.contains(issue.category.lowercase()) ||
                q.contains(issue.assignedDept.lowercase()) ||
                q.contains(issue.status.lowercase()) ||
                q.contains(issue.ward.lowercase())
        }
        if (categoryMatches.isNotEmpty()) {
            val top = categoryMatches.take(3).joinToString("\n") { issue ->
                "- ${issue.id}: ${issue.title} (${issue.status}, ${issue.assignedDept})"
            }
            return "I found ${categoryMatches.size} related NagarNetra issue(s) in your area:\n$top\nYou can ask about a specific ticket by mentioning its ID (e.g. T-1)."
        }

        return when {
            q.isBlank() -> "Ask me anything! I can help you report civic issues, check status of tickets, find responsible departments, explain the automatic escalation pipeline, or answer general smart city questions."
            listOf("hello", "hi", "hey", "namaste").any { q == it || q.startsWith("$it ") } ->
                "Namaste! I am NagarNetra AI Civic Companion, your smart assistant for Bengaluru's civic management. I help you report and track municipal problems like potholes, garbage, or water leaks."
            q.contains("report") || q.contains("photo") || q.contains("camera") ->
                "To report a new civic issue, simply go to the 'Report' tab. Upload or capture a photo of the issue. Our AI VisionBot will automatically verify the content and categorize it. You can adjust the location pin on the map, then tap 'Confirm and Dispatch Ticket' to submit it to the relevant department."
            q.contains("department") || q.contains("who handles") || q.contains("route") ->
                "NagarNetra automatically routes reports to the correct municipal authorities: potholes and road damage go to PWD, water line leaks and sewage issues go to BWSSB, streetlights and electrical outages go to BESCOM, and solid waste or garbage issues go to BBMP."
            q.contains("escalat") || q.contains("urgent") || q.contains("emergency") ->
                "NagarNetra implements an automatic SLA monitoring system. If an issue is critical or remains unresolved for too long (e.g., over 7 days for PWD potholes or BBMP garbage), the EscalationBot elevates its priority level and severity, notifying senior ward commissioners. For life-threatening emergencies, please dial the municipal helpline directly."
            q.contains("reward") || q.contains("point") || q.contains("trust") ->
                "You earn points and raise your trust score by reporting genuine civic issues, upvoting other verified reports in your neighborhood, and verifying resolved tickets. Higher trust means your reports are dispatched faster."
            q.contains("map") || q.contains("location") || q.contains("gps") ->
                "The map tab shows reported civic issues as visual pins across the city. When reporting, you can drag the map pin to set the exact coordinates, or let the app detect your GPS location automatically."
            q.contains("name") || q.contains("who are you") -> 
                "I am the NagarNetra AI Civic Companion. I'm a specialized assistant designed to bridge the communication gap between citizens and municipal bodies in Bengaluru."
            q.contains("smart city") -> 
                "A smart city utilizes IoT sensors, AI engines (like our Multi-Agent network), and data-driven routing to manage infrastructure, traffic, and utilities, maximizing administrative efficiency."
            q.contains("bbmp") -> 
                "BBMP (Bruhat Bengaluru Mahanagara Palike) is the civic body responsible for infrastructure and administrative tasks in the Greater Bengaluru metropolitan area."
            q.contains("bescom") -> 
                "BESCOM (Bangalore Electricity Supply Company) is responsible for power distribution in eight districts of Karnataka, including Bengaluru, and handles all streetlight reports."
            q.contains("bwssb") -> 
                "BWSSB (Bangalore Water Supply and Sewerage Board) is the premier governmental body in charge of sewage disposal and water supply to the city of Bengaluru."
            q.contains("pwd") -> 
                "The Public Works Department (PWD) in Bengaluru manages public infrastructure construction and road maintenance, including fixing potholes on primary roads."
            q.contains("pothole") || q.contains("road") -> 
                "Potholes are structural failures in asphalt road surfaces caused by water penetration and traffic stress. You can report them in NagarNetra to alert the PWD."
            q.contains("garbage") || q.contains("waste") -> 
                "Proper waste management is essential for public health. You can report overflowing bins, illegal dumping sites, or missed garbage pickup to BBMP using our app."
            q.contains("leak") || q.contains("water") -> 
                "Water pipeline leakages lead to resource loss and road damage. Report water line issues immediately so BWSSB can dispatch a plumber team."
            q.contains("what is") || q.contains("explain") -> 
                "I can explain: NagarNetra is an AI-powered system designed to automate municipal grievance redressal, utilizing deep learning to verify and dispatch citizen reports to the correct civic body."
            q.contains("how to") -> 
                "To accomplish tasks in this app: 1. Use the Report tab to submit new issues with photos. 2. Use the Map tab to view local issues. 3. Use the Assistant tab to query data or seek civic help."
            else -> 
                "I can answer NagarNetra civic questions only. Ask about reports, tickets, departments, locations, escalation, rewards, or app steps."
        }
    }

    private fun summarizeToCivicAnswer(answer: String, userQuery: String, allIssues: List<Issue>): String {
        val fallback = fallback(userQuery, allIssues)
        val civicLines = answer
            .lines()
            .map { it.trim().trim('-', '*', ' ') }
            .filter { it.isNotBlank() }
            .filter { line -> civicKeywords.any { line.contains(it, ignoreCase = true) } }
            .take(3)

        return if (civicLines.isNotEmpty()) civicLines.joinToString("\n") else fallback
    }
}
class CitizenAssistant(private val client: GeminiAgentClient) {
    suspend fun chat(
        userQuery: String,
        history: List<com.example.ui.ChatMessage>,
        allIssues: List<Issue>
    ): String {
        val q = userQuery.lowercase().trim()
        if (shouldAnswerLocally(q, allIssues)) {
            return CivicAssistantPolicy.fallback(userQuery, allIssues)
        }

        val systemInstruction = """
            You are NagarNetra AI Civic Companion for a Bengaluru smart city app.
            Answer in 1-3 short, useful sentences. Use the live database context when relevant.

            ${buildDbContext(allIssues)}
        """.trimIndent()

        val historyStr = history.takeLast(3).joinToString("\n") {
            "${if (it.isBot) "Assistant" else "Citizen"}: ${it.text.take(220)}"
        }
        val prompt = "$historyStr\nCitizen: $userQuery"

        val response = client.callAgent(
            prompt = prompt,
            systemInstruction = systemInstruction,
            responseJson = false,
            fastMode = true,
            maxOutputTokens = 256
        )
        return if (response != null) {
            val cleaned = response.trim()
            val answerText = if (cleaned.startsWith("{") && cleaned.contains("\"responseText\"")) {
                try {
                    val jsonObject = JSONObject(cleaned)
                    jsonObject.optString("responseText")
                } catch (e: Exception) {
                    cleaned
                }
            } else if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                cleaned.substring(1, cleaned.length - 1)
            } else cleaned
            CivicAssistantPolicy.enforceScope(userQuery, answerText, allIssues)
        } else {
            CivicAssistantPolicy.fallback(userQuery, allIssues)
        }
    }

    private fun shouldAnswerLocally(q: String, allIssues: List<Issue>): Boolean {
        if (q.isBlank()) return true
        if (allIssues.any { q.contains(it.id.lowercase()) }) return true
        if (listOf("hello", "hi", "hey", "namaste").any { q == it || q.startsWith("$it ") }) return true

        return listOf(
            "how many", "count", "number of", "status", "ticket", "pothole", "road",
            "water", "leak", "garbage", "waste", "trash", "light", "streetlight",
            "sewage", "drain", "department", "route", "report", "photo", "camera",
            "map", "location", "gps", "reward", "point", "trust", "bbmp", "pwd",
            "bwssb", "bescom", "escalat", "urgent", "emergency"
        ).any { q.contains(it) }
    }

    private fun buildDbContext(allIssues: List<Issue>): String {
        val active = allIssues.count { it.status != "Resolved" }
        val categoryCounts = listOf("Pothole", "Water", "Lights", "Garbage", "Sewage", "Pedestrian")
            .joinToString(", ") { category ->
                val count = allIssues.count { issue ->
                    issue.category.equals(category, ignoreCase = true) || issue.title.contains(category, ignoreCase = true)
                }
                "$category=$count"
            }
        val recentTickets = allIssues.take(8).joinToString("\n") { issue ->
            "- ${issue.id}: ${issue.title} | ${issue.category} | ${issue.status} | ${issue.assignedDept}"
        }
        return """
            Live context: total=${allIssues.size}, active=$active, resolved=${allIssues.size - active}, $categoryCounts
            Recent tickets:
            $recentTickets
        """.trimIndent()
    }
}
class AuthorityAssistant(private val client: GeminiAgentClient) {
    suspend fun compileWardSummary(ward: String, issues: List<Issue>): String {
        val summaryText = issues.filter { it.ward.lowercase().contains(ward.lowercase()) }.joinToString("\n") {
            "Ticket ${it.id} [${it.status}] [Category: ${it.category}] [Severity: ${it.severity}]"
        }

        val systemInstruction = """
            You are AuthorityAssistant (Agent 9) of NagarNetra AI.
            Compile a brief, professional ward operational summary report for the Ward Commissioner of Bengaluru.
            Return a raw JSON object:
            {
              "reportContent": "string markdown summary content",
              "confidence": <integer 0-100>
            }
        """.trimIndent()

        val prompt = "Ward: $ward. Active issues context:\n$summaryText"
        val response = client.callAgent(prompt, systemInstruction)

        return if (response != null) {
            try {
                val json = JSONObject(response)
                json.getString("reportContent")
            } catch (e: Exception) {
                Log.e("AuthorityAssistant", "JSON parse error: $response", e)
                buildLocalWardReport(issues)
            }
        } else {
            buildLocalWardReport(issues)
        }
    }

    suspend fun generateAccountabilityEmail(
        triggerType: String,
        issue: Issue?,
        allIssues: List<Issue>
    ): String {
        val systemInstruction = """
            You are AuthorityAssistant (Agent 9) of NagarNetra AI.
            Draft an automated official escalation email in HTML format for Bengaluru officers.
            Tone: Professional and urgent. Return raw HTML inside this JSON structure:
            {
              "emailBody": "HTML String",
              "confidence": <integer 0-100>
            }
        """.trimIndent()

        val prompt = "TriggerType: $triggerType. Issue details: ${issue?.id} ${issue?.category} ${issue?.severity}. Total Ward issues count: ${allIssues.size}."
        val response = client.callAgent(prompt, systemInstruction)

        return if (response != null) {
            try {
                val json = JSONObject(response)
                json.getString("emailBody")
            } catch (e: Exception) {
                Log.e("AuthorityAssistant", "JSON parse error: $response", e)
                getLocalFallbackEmail(triggerType, issue, allIssues)
            }
        } else {
            getLocalFallbackEmail(triggerType, issue, allIssues)
        }
    }

    private fun buildLocalWardReport(issues: List<Issue>): String {
        val active = issues.count { it.status != "Resolved" }
        return "NAGARNETRA WARD PERFORMANCE SUMMARY\n=======================================\nTotal Reports: ${issues.size}\nActive Tickets: $active\nSLA breaches: ${issues.count { it.slaStatus == "SLA BREACHED" }}\nResolved Tickets: ${issues.size - active}"
    }

    private fun getLocalFallbackEmail(triggerType: String, issue: Issue?, allIssues: List<Issue>): String {
        val subject = "NAGARNETRA AI CIVIC ESCALATION ALERT [${issue?.id ?: "WARD SUMMARY"}]"
        return """
            <h3>$subject</h3>
            <p>This is an automated administrative notification generated by the NagarNetra AI Multi-Agent Network.</p>
            <p><strong>Trigger Type:</strong> $triggerType</p>
            ${if (issue != null) "<p><strong>Issue:</strong> ${issue.title} (Category: ${issue.category}, Severity: ${issue.severity}/10)</p>" else ""}
            <p>Please log in to the NagarNetra Authority Control Room Dashboard to take appropriate action.</p>
        """.trimIndent()
    }
}

// ─────────────────────────────────────────────
// CENTRAL COORDINATOR PIPELINE (Agent Orchestrator)
// ─────────────────────────────────────────────
class MultiAgentCoordinator {
    private val client = GeminiAgentClient()
    
    val locationAgent = LocationAgent(client)
    val visionAgent = VisionAgent(client)
    val categoryAgent = CategoryAgent(client)
    val severityAgent = SeverityAgent(client)
    val duplicateAgent = DuplicateDetectionAgent(client)
    val routingAgent = RoutingAgent(client)
    val emergencyAgent = EmergencyAgent(client)
    val citizenAssistant = CitizenAssistant(client)
    val authorityAssistant = AuthorityAssistant(client)

    // Unified fast agent for primary analysis
    private val unifiedVisionAgent = UnifiedVisionAgent(client)
    private val localLocationResolver = LocalLocationResolver()

    suspend fun runOrchestrationPipeline(
        bitmap: Bitmap?,
        manualCategory: String?,
        latitude: Double,
        longitude: Double,
        locationNotes: String?,
        existingIssues: List<Issue>
    ): AgentContext = withContext(Dispatchers.IO) {

        val startTime = System.currentTimeMillis()

        // ── Step 1: Compress image FAST (50% quality, max 800px wide for speed)
        val imageBase64 = bitmap?.let { bmp ->
            try {
                val out = ByteArrayOutputStream()
                // Keep more detail for road-surface and pothole recognition.
                val maxDim = 1280
                val scaled = if (bmp.width > maxDim || bmp.height > maxDim) {
                    val scale = maxDim.toFloat() / maxOf(bmp.width, bmp.height)
                    android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
                } else bmp
                scaled.compress(Bitmap.CompressFormat.JPEG, 82, out) // Preserve texture details for vision classification
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            } catch (e: Exception) {
                Log.e("MultiAgentCoordinator", "Image compress failed", e)
                null
            }
        }

        // ── Step 2: Initialize context
        val context = AgentContext(
            imageBase64 = imageBase64,
            manualCategory = manualCategory,
            latitude = latitude,
            longitude = longitude,
            locationNotes = locationNotes,
            existingIssues = existingIssues
        )
        context.traceLogs.add("MultiAgentCoordinator: Fast Pipeline initiated.")

        // ── Step 3: Location resolved LOCALLY (instant, no API)
        localLocationResolver.resolve(context)

        // ── Step 4: Single unified Gemini call → Vision + Category + Severity + Emergency at once
        val unified = unifiedVisionAgent.run(context)

        // Apply results to context
        context.visionResult    = unified.vision
        context.categoryResult  = unified.category
        context.severityResult  = unified.severity
        context.emergencyResult = unified.emergency

        // Store professional title for use in submitReport
        context.traceLogs.add("[REPORT_TITLE]:${unified.reportTitle}")

        val confidence = unified.category.confidence
        if (confidence < 60) {
            context.requiresHumanVerification = true
            context.verificationReasons.add("AI confidence low (${confidence}%)")
        }
        if (unified.emergency.isEmergency) {
            context.requiresHumanVerification = true
            context.verificationReasons.add("Critical Emergency: ${unified.emergency.emergencyType}")
        }

        // ── Step 5: Duplicate check LOCALLY (instant proximity math, no API)
        LocalDuplicateChecker.check(context)

        // ── Step 6: Routing resolved LOCALLY (instant rule table, no API)
        val routing = LocalRoutingResolver.resolve(unified.category.category, unified.severity.severity)
        context.routingResult = routing

        val elapsed = System.currentTimeMillis() - startTime
        context.traceLogs.add("MultiAgentCoordinator: Pipeline complete in ${elapsed}ms. HumanReview: ${context.requiresHumanVerification}")
        Log.d("MultiAgentCoordinator", "Pipeline completed in ${elapsed}ms")
        return@withContext context
    }
}
