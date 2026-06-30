package com.example.data

import android.graphics.Bitmap
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

// ─────────────────────────────────────────────
// OUTPUT MODEL
// ─────────────────────────────────────────────

data class IssueAnalysisResult(
    val category: String,       // Pothole, Water, Lights, Garbage, Sewage, Pedestrian, Other
    val severity: Int,          // 1–10
    val riskLevel: String,      // Low, Medium, High, Critical
    val department: String,     // PWD, BWSSB, BESCOM, BBMP
    val description: String,
    val estimatedDays: String,  // e.g. "3-5 Days"
    val confidence: Int = 0,    // 0–100
    val source: String = "gemini", // "gemini" | "fallback" | "preset"
    val reportTitle: String = ""   // AI-generated professional report title
)

// ─────────────────────────────────────────────
// GEMINI CLIENT
// ─────────────────────────────────────────────

class GeminiClient {

    companion object {
        private const val TAG = "NagarNetraAI"

        // ✅ FIX 1: Use latest vision-capable model; fallback chain if unavailable
        private const val PRIMARY_MODEL   = "gemini-3.5-flash"
        private const val FALLBACK_MODEL  = "gemini-2.5-flash"
        private const val CHAT_MODEL      = "gemini-2.5-flash-lite"

        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models"

        private val VALID_CATEGORIES = setOf(
            "Pothole", "Water", "Lights", "Garbage", "Sewage", "Pedestrian", "Other"
        )
        private val VALID_RISK_LEVELS  = setOf("Low", "Medium", "High", "Critical")
        private val VALID_DEPARTMENTS  = setOf("PWD", "BWSSB", "BESCOM", "BBMP")

        // ✅ FIX 2: Singleton OkHttpClient — never create one per call
        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
        }
    }

    // ─────────────────────────────────────────
    // BITMAP → BASE64
    // ─────────────────────────────────────────

    // ✅ FIX 3: Compress at 85 quality (75 loses detail Gemini needs for classification)
    private fun Bitmap.toBase64(): String {
        val out = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    // ─────────────────────────────────────────
    // VALIDATE API KEY
    // ─────────────────────────────────────────

    private fun String?.isValidKey(): Boolean =
        !isNullOrEmpty() &&
        this != "MY_GEMINI_API_KEY" &&
        !contains("placeholder", ignoreCase = true)

    // ─────────────────────────────────────────
    // ANALYZE CIVIC ISSUE FROM BITMAP
    // ─────────────────────────────────────────

    suspend fun analyzeCivicIssue(
        bitmap: Bitmap?,
        promptText: String? = null
    ): IssueAnalysisResult = withContext(Dispatchers.IO) {

        val apiKey = BuildConfig.GEMINI_API_KEY
        Log.d(TAG, "analyzeCivicIssue — API key present: ${apiKey.isValidKey()}")

        if (!apiKey.isValidKey()) {
            Log.w(TAG, "No valid API key — using simulated analysis.")
            return@withContext getSimulatedAnalysis(bitmap)
        }

        if (bitmap == null) {
            Log.w(TAG, "No bitmap provided — using simulated analysis.")
            return@withContext getSimulatedAnalysis(null)
        }

        // ✅ FIX 4: Gemini Vision prompt based on user instructions
        val systemPrompt = """
            Analyze the submitted image for civic/infrastructure issues. 
            CRITICAL INSTRUCTION: The image is the absolute source of truth. If the user's category hint contradicts the visual evidence in the image, IGNORE the hint and classify the issue strictly based on the image contents. For example, if the hint says "Garbage" but the image shows a "Pothole", your classification MUST be "Pothole".
            
            Identify: 
            1) Issue category (Pothole, Garbage, Broken Light, Water Leakage, Road Damage, Sewage, Pedestrian, etc.) based strictly on the image content.
            2) Severity score out of 10.
            3) Brief visual description of what you see in the image.
            4) Recommended municipal department.
            
            Be accurate — do not guess. Respond in JSON format.
            
            Return a JSON object with these exact keys:
            {
              "category": "Detected category name",
              "severity": 1-10 integer score,
              "description": "Visual description of what you see",
              "department": "Recommended municipal department",
              "riskLevel": "Low, Medium, High, or Critical",
              "estimatedDays": "Estimated days to resolve, e.g., 3-5 Days",
              "confidence": 0-100 integer confidence score
            }
        """.trimIndent()

        val base64Img = bitmap.toBase64()

        val partsArray = JSONArray().apply {
            put(JSONObject().put("text", systemPrompt))
            if (!promptText.isNullOrBlank()) put(JSONObject().put("text", promptText))
            put(JSONObject().put("inlineData", JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", base64Img)
            }))
        }

        val requestBody = buildGeminiRequest(
            partsArray = partsArray,
            temperature = 0.0,          // ✅ FIX 5: 0.0 for max determinism on structured output
            responseJson = true
        )

        // ✅ FIX 6: Try primary model, auto-retry with fallback on 404
        val result = callGemini(PRIMARY_MODEL, requestBody, apiKey)
            ?: callGemini(FALLBACK_MODEL, requestBody, apiKey)

        result?.let { parseVisionResponse(it) } ?: getSimulatedAnalysis(bitmap)
    }

    // ─────────────────────────────────────────
    // CHAT WITH CIVIC AGENT
    // ─────────────────────────────────────────

    suspend fun chatWithCivicAgent(
        apiKey: String?,
        currentMessage: String,
        history: List<com.example.ui.ChatMessage>,
        allIssues: List<Issue>
    ): String = withContext(Dispatchers.IO) {
        val q = currentMessage.lowercase().trim()
        if (shouldUseLocalChatAnswer(q, allIssues)) {
            return@withContext getLocalFallbackAnswer(currentMessage, allIssues)
        }

        if (!apiKey.isValidKey()) {
            return@withContext getLocalFallbackAnswer(currentMessage, allIssues)
        }

        val totalIssues = allIssues.size
        val activeCount = allIssues.count { it.status != "Resolved" }
        val dbContext = """
            Live context: total=$totalIssues, active=$activeCount, resolved=${totalIssues - activeCount}
            Recent tickets:
            ${allIssues.take(8).joinToString("\n") { issue ->
                "- ${issue.id}: ${issue.title} | ${issue.category} | ${issue.status} | ${issue.assignedDept}"
            }}
        """.trimIndent()

        val systemInstruction = """
            You are NagarNetra AI Voice Civic Agent, a helpful general-purpose assistant inside a civic reporting app.
            Answer any safe user question clearly and naturally, not only civic questions.
            Keep replies concise for mobile voice playback: usually 1-4 short sentences.
            If the user asks about NagarNetra, reports, tickets, departments, maps, locations, or local civic status, use the live context below.
            If the user asks a general question, answer from broad knowledge and do not force the answer back to municipal issues.
            If the user wants to report an issue, tell them to say or type that they want to report and the app will open the camera.

            $dbContext
        """.trimIndent()

        val contentsArray = JSONArray().apply {
            history.takeLast(3).forEach { msg ->
                val role = if (msg.isBot) "model" else "user"
                put(JSONObject()
                    .put("role", role)
                    .put("parts", JSONArray().put(JSONObject().put("text", msg.text.take(220))))
                )
            }
            put(JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", currentMessage)))
            )
        }

        val requestBody = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", 256)
            })
        }.toString()

        val result = callGemini(CHAT_MODEL, requestBody, apiKey)
            ?: return@withContext getLocalFallbackAnswer(currentMessage, allIssues)

        extractText(result) ?: getLocalFallbackAnswer(currentMessage, allIssues)
    }

    private fun shouldUseLocalChatAnswer(q: String, allIssues: List<Issue>): Boolean {
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
    suspend fun compileWardReport(
        apiKey: String?,
        issues: List<Issue>
    ): String = withContext(Dispatchers.IO) {

        if (!apiKey.isValidKey()) {
            return@withContext buildLocalWardReport(issues)
        }

        val issuesSummary = issues.joinToString("\n") {
            "Ticket ${it.id}: ${it.category} at ${it.locationName} | " +
            "Severity: ${it.severity}/10 | Status: ${it.status} | ${it.description}"
        }

        // ✅ FIX 9: Improved report prompt — requests structured executive output
        val prompt = """
You are compiling an official municipal operations report for BBMP Ward 80, Bengaluru.

ACTIVE COMPLAINTS DATA:
$issuesSummary

Produce a professional executive report with:
1. Executive Summary (3-4 sentences)
2. Key Metrics (total, active, resolved, by category)
3. High-Priority Issues (severity 7+)
4. Department-wise Action Items (BBMP, BWSSB, PWD, BESCOM)
5. Preventive Recommendations

Use formal language suitable for the Ward Commissioner. Be factual and data-driven.
        """.trimIndent()

        val partsArray = JSONArray().apply {
            put(JSONObject().put("text", prompt))
        }
        val requestBody = buildGeminiRequest(partsArray, temperature = 0.3, responseJson = false)

        val result = callGemini(PRIMARY_MODEL, requestBody, apiKey)
            ?: callGemini(FALLBACK_MODEL, requestBody, apiKey)
            ?: return@withContext buildLocalWardReport(issues)

        extractText(result) ?: buildLocalWardReport(issues)
    }

    // ─────────────────────────────────────────
    // SHARED HELPERS
    // ─────────────────────────────────────────

    /** Build a standard Gemini generateContent request body */
    private fun buildGeminiRequest(
        partsArray: JSONArray,
        temperature: Double,
        responseJson: Boolean
    ): String {
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().put("parts", partsArray))
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", temperature)
                put("maxOutputTokens", 1024)
                if (responseJson) put("responseMimeType", "application/json")
            })
            // ✅ FIX 10: Relaxed safety settings — civic images trigger false positives
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
        }.toString()
    }

    /** Execute a Gemini API call; returns raw response body or null on failure */
    private fun callGemini(model: String, requestBody: String, apiKey: String?): String? {
        return try {
            val url = "$BASE_URL/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d(TAG, "→ Calling Gemini model: $model")
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful && body.isNotEmpty()) {
                Log.d(TAG, "← Gemini $model success (${body.length} chars)")
                body
            } else {
                Log.e(TAG, "← Gemini $model error ${response.code}: $body")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "callGemini($model) exception: ${e.message}", e)
            null
        }
    }

    /** Extract text content from a Gemini response body */
    private fun extractText(responseBody: String): String? {
        return try {
            JSONObject(responseBody)
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "extractText failed: ${e.message}")
            null
        }
    }

    /** Parse vision JSON response into IssueAnalysisResult */
    private fun parseVisionResponse(responseBody: String): IssueAnalysisResult? {
        return try {
            val rawText = extractText(responseBody) ?: return null

            // Strip markdown fences if present despite responseMimeType setting
            val cleaned = rawText.trim()
                .replace("```json", "")
                .replace("```", "")
                .trim()

            // Extract JSON object if wrapped in other text
            val jsonStr = if (cleaned.startsWith("{")) cleaned
            else Regex("\\{.*}", RegexOption.DOT_MATCHES_ALL).find(cleaned)?.value ?: cleaned

            val parsed = JSONObject(jsonStr)

            val rawCategory = parsed.optString("category", "Other")
            val description = parsed.optString("description", "Civic issue detected.").ifBlank { "Civic issue detected." }
            val category = inferCivicCategory(rawCategory, description)

            val rawRisk = parsed.optString("riskLevel", "Medium")
            val riskLevel = VALID_RISK_LEVELS.find { it.equals(rawRisk, ignoreCase = true) }
                ?: "Medium"

            val rawDept = parsed.optString("department", "BBMP")
            val department = VALID_DEPARTMENTS.find { it.equals(rawDept, ignoreCase = true) }
                ?: rawDept.ifBlank { departmentForCategory(category) }

            val severity    = parsed.optInt("severity", 5).coerceIn(1, 10)
            val estDays     = parsed.optString("estimatedDays", "3-5 Days").ifBlank { "3-5 Days" }
            val confidence  = parsed.optInt("confidence", 70).coerceIn(0, 100)
            Log.i(TAG, "✅ Vision parsed: $category | severity=$severity | risk=$riskLevel | confidence=$confidence%")

            IssueAnalysisResult(
                category     = category,
                severity     = severity,
                riskLevel    = riskLevel,
                department   = department,
                description  = description,
                estimatedDays= estDays,
                confidence   = confidence,
                source       = "gemini"
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseVisionResponse failed: ${e.message}")
            null
        }
    }

    private fun departmentForCategory(category: String): String = when (category.lowercase()) {
        "pothole"    -> "PWD"
        "water",
        "sewage"     -> "BWSSB"
        "lights"     -> "BESCOM"
        else         -> "BBMP"
    }

    // ─────────────────────────────────────────
    // LOCAL FALLBACKS
    // ─────────────────────────────────────────

    private fun getLocalFallbackAnswer(query: String, allIssues: List<Issue>): String {
        val q = query.lowercase().trim()
        
        // 1. Greet check
        if (q.contains("hello") || q.contains("hi") || q.contains("hey") || q.contains("namaste")) {
            return "Namaste! I am CivicChatBot of NagarNetra AI. I can help you check the status of local complaints, categories of issues, or summarize municipal status. Ask me about a specific ticket (e.g., 'status of IDS-181') or a category like potholes, water, lights, or garbage!"
        }
        
        // 2. Ticket ID check (e.g. "IDS-181" or "181")
        val numberRegex = "\\d+".toRegex()
        val matchResult = numberRegex.find(q)
        if (matchResult != null) {
            val numStr = matchResult.value
            val match = allIssues.find { it.id.lowercase().contains(numStr) }
            if (match != null) {
                return "🎫 **Ticket ${match.id}**\n" +
                       "• **Title**: ${match.title}\n" +
                       "• **Status**: ${match.status}\n" +
                       "• **Department**: ${match.assignedDept}\n" +
                       "• **Severity**: ${match.severity}/10\n" +
                       "• **Location**: ${match.locationName}\n" +
                       "• **Description**: ${match.description}"
            }
        }
        
        // 3. Category checks
        if (q.contains("pothole") || q.contains("road") || q.contains("street") || q.contains("pwd")) {
            val potholes = allIssues.filter { it.category.lowercase().contains("pothole") || it.category.lowercase().contains("road") }
            val active = potholes.count { it.status != "Resolved" }
            if (potholes.isEmpty()) {
                return "Great news! There are currently no reported pothole or road issues in this ward."
            }
            val listStr = potholes.take(3).joinToString("\n") { "• Ticket ${it.id} (${it.status}): ${it.title} at ${it.locationName}" }
            return "🛣️ **Potholes & Roads (PWD)**\n" +
                   "We have ${potholes.size} complaints reported ($active active/unresolved):\n" +
                   "$listStr" + (if (potholes.size > 3) "\n• And ${potholes.size - 3} more issues." else "")
        }
        
        if (q.contains("water") || q.contains("sewage") || q.contains("leak") || q.contains("drain") || q.contains("bwssb")) {
            val water = allIssues.filter { it.category.lowercase().contains("water") || it.category.lowercase().contains("sewage") || it.category.lowercase().contains("drain") }
            val active = water.count { it.status != "Resolved" }
            if (water.isEmpty()) {
                return "No water or sewage complaints reported in this area."
            }
            val listStr = water.take(3).joinToString("\n") { "• Ticket ${it.id} (${it.status}): ${it.title} at ${it.locationName}" }
            return "💧 **Water & Sewage (BWSSB)**\n" +
                   "We have ${water.size} complaints reported ($active active):\n" +
                   "$listStr" + (if (water.size > 3) "\n• And ${water.size - 3} more issues." else "")
        }
        
        if (q.contains("garbage") || q.contains("trash") || q.contains("waste") || q.contains("bbmp")) {
            val garbage = allIssues.filter { it.category.lowercase().contains("garbage") || it.category.lowercase().contains("trash") || it.category.lowercase().contains("waste") }
            val active = garbage.count { it.status != "Resolved" }
            if (garbage.isEmpty()) {
                return "All clear! No garbage piled-up complaints reported."
            }
            val listStr = garbage.take(3).joinToString("\n") { "• Ticket ${it.id} (${it.status}): ${it.title} at ${it.locationName}" }
            return "♻️ **Garbage & Sanitation (BBMP)**\n" +
                   "We have ${garbage.size} complaints reported ($active active):\n" +
                   "$listStr" + (if (garbage.size > 3) "\n• And ${garbage.size - 3} more issues." else "")
        }
        
        if (q.contains("light") || q.contains("lamp") || q.contains("electricity") || q.contains("bescom")) {
            val lights = allIssues.filter { it.category.lowercase().contains("light") || it.category.lowercase().contains("lamp") || it.category.lowercase().contains("electricity") }
            val active = lights.count { it.status != "Resolved" }
            if (lights.isEmpty()) {
                return "All streetlights are working fine according to current reports."
            }
            val listStr = lights.take(3).joinToString("\n") { "• Ticket ${it.id} (${it.status}): ${it.title} at ${it.locationName}" }
            return "💡 **Streetlights & Power (BESCOM)**\n" +
                   "We have ${lights.size} complaints reported ($active active):\n" +
                   "$listStr" + (if (lights.size > 3) "\n• And ${lights.size - 3} more issues." else "")
        }
        
        if (q.contains("ward") || q.contains("report") || q.contains("summary") || q.contains("status")) {
            val active = allIssues.count { it.status != "Resolved" }
            return "📊 **Ward 80 Summary Report**\n" +
                   "• **Total Complaints**: ${allIssues.size}\n" +
                   "• **Active/Pending**: $active\n" +
                   "• **Resolved**: ${allIssues.size - active}\n" +
                   "• **Resolution Rate**: ${"%.1f".format(if (allIssues.isEmpty()) 0.0 else (allIssues.size - active) * 100.0 / allIssues.size)}%\n" +
                   "Please ask about a specific ticket or category if you need detailed department action logs!"
        }
        
        // 4. Default fallback
        val active = allIssues.count { it.status != "Resolved" }
        return "I found ${allIssues.size} total complaints in Ward 80 ($active active).\n" +
               "Please search for category (e.g. 'water leaks', 'pothole list') or specific ticket ID (e.g. 'status of IDS-181')."
    }

    private fun buildLocalWardReport(issues: List<Issue>): String {
        val active   = issues.count { it.status != "Resolved" }
        val resolved = issues.count { it.status == "Resolved" }
        val byCategory = issues.groupBy { it.category }
            .map { (cat, list) -> "  • $cat: ${list.size}" }.joinToString("\n")
        val highPriority = issues.filter { it.severity >= 7 }
            .joinToString("\n") { "  • Ticket ${it.id}: ${it.title} (Severity ${it.severity}/10)" }

        return """
NAGARNETRA AI — WARD 80 OPERATIONAL STATUS REPORT
==================================================
Total Complaints : ${issues.size}
Active           : $active
Resolved         : $resolved
Resolution Rate  : ${"%.1f".format(if (issues.isEmpty()) 0.0 else resolved * 100.0 / issues.size)}%

CATEGORY BREAKDOWN:
$byCategory

HIGH PRIORITY ISSUES (Severity 7+):
${highPriority.ifEmpty { "  None currently." }}

ACTION ITEMS:
  1. BWSSB — Audit water/sewage clusters for main line failures
  2. PWD   — Deploy road crews to pothole hotspots within 48 hours
  3. BBMP  — Reschedule garbage collection routes for missed zones
  4. BESCOM — Inspect feeder lines for multi-lamp outages
        """.trimIndent()
    }

    private fun getSimulatedAnalysis(bitmap: Bitmap?): IssueAnalysisResult {
        return IssueAnalysisResult(
            category      = "Other",
            severity      = 5,
            riskLevel     = "Medium",
            department    = "BBMP",
            description   = "Gemini Vision could not confidently analyze this image. Please review the photo and report details before dispatch.",
            estimatedDays = "3-5 Days",
            confidence    = 0,
            source        = "fallback"
        )
    }

    // ─────────────────────────────────────────
    // PRESET CATALOG
    // ─────────────────────────────────────────

    fun getPresetAnalysis(category: String): IssueAnalysisResult = when (category.lowercase()) {
        "pothole" -> IssueAnalysisResult(
            category = "Pothole", severity = 8, riskLevel = "High", department = "PWD",
            description = "Severe pavement crater approx 1.5 ft wide on main carriageway. Drivers swerving dangerously.",
            estimatedDays = "3-5 Days", confidence = 95, source = "preset"
        )
        "water" -> IssueAnalysisResult(
            category = "Water", severity = 7, riskLevel = "Medium", department = "BWSSB",
            description = "Underground water pipe burst. Active leak spilling onto commercial footpath.",
            estimatedDays = "2-3 Days", confidence = 95, source = "preset"
        )
        "lights" -> IssueAnalysisResult(
            category = "Lights", severity = 6, riskLevel = "Medium", department = "BESCOM",
            description = "Multiple streetlights out on secondary cross road. Safety concern for pedestrians at night.",
            estimatedDays = "1-2 Days", confidence = 95, source = "preset"
        )
        "garbage" -> IssueAnalysisResult(
            category = "Garbage", severity = 8, riskLevel = "High", department = "BBMP",
            description = "Large uncollected waste pile near public market. Attracting vectors and causing health hazard.",
            estimatedDays = "1-2 Days", confidence = 95, source = "preset"
        )
        "sewage", "manhole" -> IssueAnalysisResult(
            category = "Sewage", severity = 10, riskLevel = "Critical", department = "BWSSB",
            description = "Open manhole on pedestrian pathway. Critical public safety hazard requiring immediate closure.",
            estimatedDays = "1 Day", confidence = 95, source = "preset"
        )
        else -> IssueAnalysisResult(
            category = "Other", severity = 5, riskLevel = "Low", department = "BBMP",
            description = "General civic complaint routed for routine ward inspection.",
            estimatedDays = "5-7 Days", confidence = 80, source = "preset"
        )
    }

    // ─────────────────────────────────────────
    // GENERATE ACCOUNTABILITY EMAIL via GEMINI
    // ─────────────────────────────────────────

    suspend fun generateAccountabilityEmail(
        apiKey: String?,
        triggerType: String, // "HIGH_VOTES" | "SEVEN_DAY" | "FOURTEEN_DAY" | "THIRTY_DAY" | "WEEKLY_SUMMARY" | "MONTHLY_REPORT"
        issue: Issue?,
        allIssues: List<Issue> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        if (!apiKey.isValidKey()) {
            return@withContext getLocalFallbackEmail(triggerType, issue, allIssues)
        }

        val prompt = when (triggerType) {
            "HIGH_VOTES" -> {
                val iss = issue!!
                """
                You are EscalationBot of NagarNetra AI, generating an urgent High Citizen Endorsement email alert.
                The civic issue "${iss.title}" (Category: ${iss.category}, Severity: ${iss.severity}/10) located at ${iss.locationName} has crossed a critical threshold of ${iss.upvotes} community endorsements (upvotes).
                
                Generate a professional HTML email in an Indian government style demanding action from the ${iss.assignedDept} department.
                Include:
                - A bold saffron colored header (#FF6B00) with "NAGARNETRA AI — CITIZEN DEMAND ALERT"
                - A clear severity badge
                - Detailed description: "${iss.description}"
                - The number of affected citizens: ${iss.upvotes}
                - A clear call-to-action button or link for the Google Maps location (using coords lat=${iss.latitude}, lng=${iss.longitude})
                - A call-to-action button to access the NagarNetra Authority Control Room Dashboard
                - A polite but authoritative tone warning of civic pressure and administrative SLA expectations.
                - Use inline styles for HTML. Keep it clean and elegant. Return ONLY the HTML content.
                """.trimIndent()
            }
            "SEVEN_DAY" -> {
                val iss = issue!!
                """
                You are EscalationBot of NagarNetra AI, generating a 7-day unresolved SLA warning email.
                The issue "${iss.title}" (Category: ${iss.category}) at ${iss.locationName} has been active for over 7 days with no resolution.
                
                Generate a professional HTML email addressed to the ${iss.assignedDept} Ward Officer.
                Tone: Polite but firm, emphasizing civic accountability and urging them to update the ticket status.
                Include:
                - A saffron header (#FF6B00) "NAGARNETRA AI — 7-DAY UNRESOLVED WARNING"
                - Details of the issue
                - Maps link and Dashboard link
                - Return ONLY the HTML content.
                """.trimIndent()
            }
            "FOURTEEN_DAY" -> {
                val iss = issue!!
                """
                You are EscalationBot of NagarNetra AI, generating a 14-day unresolved SLA stern warning.
                The issue "${iss.title}" at ${iss.locationName} remains unresolved after 14 days of posting.
                
                Generate an HTML email in a stern, commanding tone demanding immediate mobilization of road/drainage crews by ${iss.assignedDept}.
                Include:
                - Header: "NAGARNETRA AI — STERN 14-DAY ACTION DISPATCH"
                - Severity badge and warnings of public impact and rising citizen upvotes.
                - Return ONLY the HTML content.
                """.trimIndent()
            }
            "THIRTY_DAY" -> {
                val iss = issue!!
                """
                You are EscalationBot of NagarNetra AI, generating a critical 30-day unresolved breach escalation email.
                The issue "${iss.title}" at ${iss.locationName} has been completely ignored for 30 days.
                
                Generate a high-pressure HTML email escalated to the Chief Ward Commissioner and senior officials of the ${iss.assignedDept} department.
                Tone: Critical, stern, citing systemic failure, and demanding emergency intervention.
                Include:
                - Header: "NAGARNETRA AI — CRITICAL 30-DAY BREACH ESCALATION TO COMMISSIONER"
                - Emergency indicators, maps link, full description, and citizen dissatisfaction score.
                - Return ONLY the HTML content.
                """.trimIndent()
            }
            "WEEKLY_SUMMARY" -> {
                val issuesSummary = allIssues.take(15).joinToString("\n") {
                    "- [${it.status}] Ticket ${it.id} (${it.category}): ${it.title} at ${it.locationName} | Upvotes: ${it.upvotes}"
                }
                """
                You are ReportBot of NagarNetra AI, generating a Weekly Ward Civic Health Summary email.
                
                Generate a professional HTML report summarizing the following active complaints:
                $issuesSummary
                
                Include:
                - Saffron header: "NAGARNETRA AI — WEEKLY WARD CIVIC HEALTH REGISTER"
                - A breakdown of total active issues, critical items, and resolving progress.
                - Clean table layout with inline CSS styles.
                - Return ONLY the HTML content.
                """.trimIndent()
            }
            "MONTHLY_REPORT" -> {
                val issuesSummary = allIssues.take(15).joinToString("\n") {
                    "- Ticket ${it.id} (${it.category}) at ${it.locationName} | Status: ${it.status} | Upvotes: ${it.upvotes} | Created ${java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(it.timePosted))}"
                }
                """
                You are ReportBot of NagarNetra AI, generating a Monthly Ward Civic Performance and Unresolved Issues Report.
                
                Generate an elegant, professional HTML report for the Ward Commissioner reviewing all unresolved tickets:
                $issuesSummary
                
                Include:
                - Saffron header: "NAGARNETRA AI — MONTHLY WARD PERFORMANCE REPORT & PERFORMANCE INDEX"
                - Performance grade (e.g. B-, C+ depending on unresolved issues)
                - List of long-standing unresolved issues.
                - Return ONLY the HTML content.
                """.trimIndent()
            }
            else -> "<h3>NagarNetra AI Civic Alert</h3>"
        }

        val partsArray = JSONArray().apply {
            put(JSONObject().put("text", prompt))
        }
        val requestBody = buildGeminiRequest(partsArray, temperature = 0.3, responseJson = false)

        val result = callGemini(PRIMARY_MODEL, requestBody, apiKey)
            ?: callGemini(FALLBACK_MODEL, requestBody, apiKey)
            ?: return@withContext getLocalFallbackEmail(triggerType, issue, allIssues)

        extractText(result) ?: getLocalFallbackEmail(triggerType, issue, allIssues)
    }

    fun getLocalFallbackEmail(
        triggerType: String,
        issue: Issue?,
        allIssues: List<Issue>
    ): String {
        val subject = when (triggerType) {
            "HIGH_VOTES" -> "NAGARNETRA AI — CITIZEN DEMAND ALERT [Ticket ${issue?.id}]"
            "SEVEN_DAY" -> "NAGARNETRA AI — 7-DAY UNRESOLVED WARNING [Ticket ${issue?.id}]"
            "FOURTEEN_DAY" -> "NAGARNETRA AI — STERN 14-DAY ACTION DISPATCH [Ticket ${issue?.id}]"
            "THIRTY_DAY" -> "NAGARNETRA AI — CRITICAL 30-DAY BREACH ESCALATION [Ticket ${issue?.id}]"
            "WEEKLY_SUMMARY" -> "NAGARNETRA AI — WEEKLY WARD CIVIC HEALTH REGISTER"
            "MONTHLY_REPORT" -> "NAGARNETRA AI — MONTHLY WARD PERFORMANCE REPORT"
            else -> "NAGARNETRA AI CIVIC ALERT"
        }

        val severityColor = when (issue?.severity ?: 5) {
            in 9..10 -> "#EF4444"
            in 7..8 -> "#F59E0B"
            else -> "#3B82F6"
        }

        val issueDetails = if (issue != null) {
            """
            <div style="border-left: 4px solid $severityColor; padding-left: 15px; margin: 15px 0; background-color: #F8FAFC; padding: 15px; border-radius: 8px;">
                <h3 style="margin-top: 0; color: #1E293B;">${issue.title}</h3>
                <p><strong>Category:</strong> ${issue.category} | <strong>Severity:</strong> ${issue.severity}/10</p>
                <p><strong>Location:</strong> ${issue.locationName}</p>
                <p><strong>Description:</strong> ${issue.description}</p>
                <p><strong>Citizens Affected:</strong> ${issue.upvotes} upvotes</p>
                <p><strong>SLA Status:</strong> <span style="background-color: #FEE2E2; color: #EF4444; padding: 3px 8px; border-radius: 4px; font-weight: bold; font-size: 11px;">${issue.slaStatus}</span></p>
            </div>
            """.trimIndent()
        } else ""

        val listItems = if (allIssues.isNotEmpty()) {
            allIssues.take(10).joinToString("") {
                """
                <tr style="border-bottom: 1px solid #E2E8F0;">
                    <td style="padding: 10px; font-weight: bold; color: #FF6B00;">${it.id}</td>
                    <td style="padding: 10px;">${it.category}</td>
                    <td style="padding: 10px;">${it.locationName}</td>
                    <td style="padding: 10px; font-weight: bold;">${it.upvotes}</td>
                    <td style="padding: 10px; color: ${if (it.status == "Resolved") "#10B981" else "#F59E0B"}; font-weight: bold;">${it.status}</td>
                </tr>
                """.trimIndent()
            }
        } else ""

        val tablesHtml = if (triggerType == "WEEKLY_SUMMARY" || triggerType == "MONTHLY_REPORT") {
            """
            <h3 style="color: #1E293B;">Active Issues List</h3>
            <table style="width: 100%; border-collapse: collapse; margin-top: 10px;">
                <thead>
                    <tr style="background-color: #F1F5F9; border-bottom: 2px solid #CBD5E1; text-align: left;">
                        <th style="padding: 10px;">Ticket ID</th>
                        <th style="padding: 10px;">Category</th>
                        <th style="padding: 10px;">Location</th>
                        <th style="padding: 10px;">Upvotes</th>
                        <th style="padding: 10px;">Status</th>
                    </tr>
                </thead>
                <tbody>
                    $listItems
                </tbody>
            </table>
            """.trimIndent()
        } else ""

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>$subject</title>
        </head>
        <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #F1F5F9; margin: 0; padding: 20px; color: #334155;">
            <table align="center" border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                <!-- Header -->
                <tr>
                    <td style="background-color: #FF6B00; padding: 25px 20px; text-align: center;">
                        <h1 style="color: #ffffff; margin: 0; font-size: 22px; font-weight: 800; letter-spacing: 0.5px;">NAGARNETRA AI</h1>
                        <p style="color: #FFE5D9; margin: 5px 0 0 0; font-size: 11px; font-weight: bold; letter-spacing: 1px;">THE EYES OF THE CITY • AUTOMATED CIVIC PRESSURE</p>
                    </td>
                </tr>
                <!-- Body -->
                <tr>
                    <td style="padding: 30px 20px;">
                        <h2 style="color: #0F172A; margin-top: 0; font-size: 18px; font-weight: 700;">$subject</h2>
                        <p style="font-size: 14px; line-height: 1.6; color: #475569;">
                            This is an automated administrative notification dispatched autonomously by the NagarNetra AI accountability network. 
                            Issues crossing urgency thresholds, receiving critical citizen support, or breaching assigned SLA timelines trigger escalations directly to ward officers.
                        </p>
                        
                        $issueDetails
                        $tablesHtml
                        
                        <div style="margin: 30px 0; text-align: center;">
                            <a href="https://maps.google.com/?q=${issue?.latitude ?: 12.973},${issue?.longitude ?: 77.635}" target="_blank" style="background-color: #FF6B00; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 14px; display: inline-block; margin-right: 10px; box-shadow: 0 2px 4px rgba(255,107,0,0.3);">📍 View Google Maps</a>
                            <a href="https://nagarnetra-ai.web.app/dashboard" target="_blank" style="background-color: #1E293B; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 14px; display: inline-block; box-shadow: 0 2px 4px rgba(30,41,59,0.3);">💻 Open Control Panel</a>
                        </div>
                    </td>
                </tr>
                <!-- Footer -->
                <tr>
                    <td style="background-color: #F8FAFC; padding: 20px; text-align: center; border-top: 1px solid #E2E8F0; font-size: 11px; color: #94A3B8; line-height: 1.5;">
                        <p style="margin: 0; font-weight: bold; color: #64748B;">NagarNetra AI Ward Governance Council</p>
                        <p style="margin: 4px 0 0 0;">This email was generated autonomously using Gemini-2.0-flash cognitive decision mapping.</p>
                        <p style="margin: 8px 0 0 0; color: #CBD5E1;">© 2026 NagarNetra AI. All rights reserved.</p>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.trimIndent()
    }
}
