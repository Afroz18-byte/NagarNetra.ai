package com.example.data

import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AppRepository(private val db: AppDatabase) {
    val allIssuesFlow: Flow<List<Issue>> = db.issueDao().getAllIssuesFlow()
    val userFlow: Flow<User?> = db.userDao().getUserFlow()
    val allLogsFlow: Flow<List<AuthorityLog>> = db.authorityLogDao().getAllLogsFlow()
    val allAiDecisionLogsFlow: Flow<List<AiDecisionLog>> = db.aiDecisionLogDao().getAllLogsFlow()
    val allEmailLogsFlow: Flow<List<EmailLog>> = db.emailLogDao().getAllLogsFlow()
    val allWardReportsFlow: Flow<List<WardReport>> = db.wardReportDao().getAllReportsFlow()
    
    fun getMessagesForRoomFlow(roomId: String): Flow<List<CommunityChatMessage>> = db.communityChatDao().getMessagesForRoomFlow(roomId)
    fun getMessagesForRoomsFlow(roomIds: List<String>): Flow<List<CommunityChatMessage>> = db.communityChatDao().getMessagesForRoomsFlow(roomIds)
    suspend fun getMessagesForRoom(roomId: String): List<CommunityChatMessage> = db.communityChatDao().getMessagesForRoom(roomId)
    suspend fun insertChatMessage(message: CommunityChatMessage): Long = db.communityChatDao().insertMessage(message)
    suspend fun updateChatMessage(message: CommunityChatMessage) = db.communityChatDao().updateMessage(message)
    suspend fun hideChatMessageByAi(messageId: Int) = db.communityChatDao().hideMessageByAi(messageId)
    suspend fun deleteChatMessage(messageId: Int) = db.communityChatDao().deleteMessage(messageId)


    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val SERVER_BASE_URL = "http://10.0.2.2:5000"

    private fun isRunningOnEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase(Locale.US)
        val model = Build.MODEL.lowercase(Locale.US)
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
        val brand = Build.BRAND.lowercase(Locale.US)
        val device = Build.DEVICE.lowercase(Locale.US)
        val product = Build.PRODUCT.lowercase(Locale.US)

        return fingerprint.startsWith("generic") || fingerprint.contains("emulator") ||
            model.contains("google_sdk") || model.contains("emulator") || model.contains("android sdk built for") ||
            manufacturer.contains("genymotion") ||
            brand.startsWith("generic") && device.startsWith("generic") ||
            product.contains("sdk") || product.contains("emulator")
    }

    private fun shouldUseLocalDevServer(): Boolean = isRunningOnEmulator()

    private fun postToServer(endpoint: String, json: JSONObject) {
        if (!shouldUseLocalDevServer()) {
            Log.d("AppRepositorySync", "Skipping local dev-server sync for $endpoint on physical device.")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = json.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("$SERVER_BASE_URL$endpoint")
                    .post(body)
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("AppRepositorySync", "Failed to sync to $endpoint: ${response.code} ${response.message}")
                    } else {
                        Log.d("AppRepositorySync", "Successfully synced to $endpoint")
                    }
                }
            } catch (e: Exception) {
                Log.e("AppRepositorySync", "Error syncing to $endpoint: ${e.message}")
            }
        }
    }

    private fun deleteFromServer(endpoint: String) {
        if (!shouldUseLocalDevServer()) {
            Log.d("AppRepositorySync", "Skipping local dev-server delete for $endpoint on physical device.")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("$SERVER_BASE_URL$endpoint")
                    .delete()
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("AppRepositorySync", "Failed to delete $endpoint: ${response.code} ${response.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("AppRepositorySync", "Error deleting $endpoint: ${e.message}")
            }
        }
    }

    private fun issueToJson(issue: Issue): JSONObject {
        return JSONObject().apply {
            put("id", issue.id)
            put("title", issue.title)
            put("category", issue.category)
            put("ward", issue.ward)
            put("locationName", issue.locationName)
            put("latitude", issue.latitude)
            put("longitude", issue.longitude)
            put("severity", issue.severity)
            put("description", issue.description)
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            put("timePosted", isoFormat.format(Date(issue.timePosted)))
            put("upvotes", issue.upvotes)
            put("status", issue.status)
            put("isMyReport", issue.isMyReport)
            put("assignedDept", issue.assignedDept)
            put("photoBase64", issue.photoBase64 ?: JSONObject.NULL)
            put("resolutionPhotoBase64", issue.resolutionPhotoBase64 ?: JSONObject.NULL)
            put("resolutionNotes", issue.resolutionNotes ?: JSONObject.NULL)
            put("slaStatus", issue.slaStatus)
            put("objectId", issue.objectId ?: JSONObject.NULL)
            put("duplicateReports", issue.duplicateReports)
            put("clusteredIssueIds", issue.clusteredIssueIdsCsv.split(",").filter { it.isNotBlank() })
            put("source", issue.source)
            put("voiceTranscript", issue.voiceTranscript)
        }
    }

    private fun logToJson(log: AuthorityLog): JSONObject {
        return JSONObject().apply {
            put("timestamp", log.timestamp)
            put("isBotAction", log.isBotAction)
            put("message", log.message)
            put("priority", log.priority)
        }
    }

    private fun aiLogToJson(log: AiDecisionLog): JSONObject {
        return JSONObject().apply {
            put("timestamp", log.timestamp)
            put("agentName", log.agentName)
            put("issueId", log.issueId ?: JSONObject.NULL)
            put("decision", log.decision)
            put("detail", log.detail)
        }
    }

    suspend fun fetchIssuesFromApi(): List<Issue> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val list = mutableListOf<Issue>()
            if (!shouldUseLocalDevServer()) {
                Log.d("AppRepositorySync", "Skipping local dev-server fetch on physical device; using local Room data.")
                return@withContext list
            }
            try {
                val request = Request.Builder()
                    .url("$SERVER_BASE_URL/api/issues")
                    .get()
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (!bodyString.isNullOrEmpty()) {
                            val jsonArray = org.json.JSONArray(bodyString)
                            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val id = obj.optString("id", "")
                                val title = obj.optString("title", "")
                                val category = obj.optString("category", "")
                                val ward = obj.optString("ward", "")
                                val locationName = obj.optString("locationName", "")
                                val latitude = obj.optDouble("latitude", 0.0)
                                val longitude = obj.optDouble("longitude", 0.0)
                                val severity = obj.optInt("severity", 5)
                                val description = obj.optString("description", "")
                                
                                val timePostedStr = obj.optString("timePosted", "")
                                val timePosted = try {
                                    isoFormat.parse(timePostedStr)?.time ?: System.currentTimeMillis()
                                } catch (e: Exception) {
                                    obj.optLong("timePosted", System.currentTimeMillis())
                                }

                                val upvotes = obj.optInt("upvotes", 0)
                                val status = obj.optString("status", "Reported")
                                val isMyReport = obj.optBoolean("isMyReport", false)
                                val assignedDept = obj.optString("assignedDept", "")
                                val photoBase64 = if (obj.isNull("photoBase64")) null else obj.optString("photoBase64").takeIf { it.isNotEmpty() }
                                val resolutionPhotoBase64 = if (obj.isNull("resolutionPhotoBase64")) null else obj.optString("resolutionPhotoBase64").takeIf { it.isNotEmpty() }
                                val resolutionNotes = if (obj.isNull("resolutionNotes")) null else obj.optString("resolutionNotes").takeIf { it.isNotEmpty() }
                                val slaStatus = obj.optString("slaStatus", "SLA OK")
                                val requiresReview = obj.optBoolean("requiresReview", false)
                                val verificationReasons = obj.optString("verificationReasons", "")
                                val objectId = if (obj.isNull("objectId")) null else obj.optString("objectId").takeIf { it.isNotEmpty() }
                                val duplicateReports = obj.optInt("duplicateReports", 1).coerceAtLeast(1)
                                val clusteredIssueIdsCsv = obj.optJSONArray("clusteredIssueIds")?.let { arr ->
                                    (0 until arr.length()).joinToString(",") { idx -> arr.optString(idx) }
                                } ?: obj.optString("clusteredIssueIdsCsv", "")
                                val source = obj.optString("source", "Mobile App")
                                val voiceTranscript = obj.optString("voiceTranscript", "")

                                val issue = Issue(
                                    id = id,
                                    title = title,
                                    category = category,
                                    ward = ward,
                                    locationName = locationName,
                                    latitude = latitude,
                                    longitude = longitude,
                                    severity = severity,
                                    description = description,
                                    timePosted = timePosted,
                                    upvotes = upvotes,
                                    status = status,
                                    isMyReport = isMyReport,
                                    assignedDept = assignedDept,
                                    photoBase64 = photoBase64,
                                    resolutionPhotoBase64 = resolutionPhotoBase64,
                                    resolutionNotes = resolutionNotes,
                                    slaStatus = slaStatus,
                                    requiresReview = requiresReview,
                                    verificationReasons = verificationReasons,
                                    objectId = objectId,
                                    duplicateReports = duplicateReports,
                                    clusteredIssueIdsCsv = clusteredIssueIdsCsv,
                                    source = source,
                                    voiceTranscript = voiceTranscript
                                )
                                list.add(issue)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("AppRepositorySync", "Local dev-server fetch unavailable: ${e.message}")
            }
            list
        }
    }

    suspend fun syncIssuesFromApi(): Int {
        val remoteIssues = fetchIssuesFromApi()
        if (remoteIssues.isNotEmpty()) {
            remoteIssues.forEach { issue ->
                db.issueDao().insertIssue(issue)
            }
            Log.d("AppRepositorySync", "Successfully synced ${remoteIssues.size} issues from API to local database.")
        }
        return remoteIssues.size
    }

    suspend fun getAllIssues(): List<Issue> {
        return db.issueDao().getAllIssues()
    }

    suspend fun getIssueById(id: String): Issue? {
        return db.issueDao().getIssueById(id)
    }

    fun getIssueByIdFlow(id: String): Flow<Issue?> {
        return db.issueDao().getIssueByIdFlow(id)
    }

    suspend fun insertIssue(issue: Issue) {
        db.issueDao().insertIssue(issue)
        postToServer("/api/issues", issueToJson(issue))
    }

    suspend fun updateIssue(issue: Issue) {
        db.issueDao().updateIssue(issue)
        postToServer("/api/issues", issueToJson(issue))
    }

    suspend fun updateUser(user: User) {
        db.userDao().updateUser(user)
    }

    suspend fun getUser(): User? {
        return db.userDao().getUser()
    }

    suspend fun insertUser(user: User) {
        db.userDao().insertUser(user)
    }

    suspend fun getUserByEmail(email: String): User? {
        return db.userDao().getUserByEmail(email)
    }

    suspend fun deleteUserById(id: String) {
        db.userDao().deleteUserById(id)
    }

    suspend fun incrementUserPointsAndReports() {
        val user = db.userDao().getUser() ?: User(id = "sandbox_user", email = "sandbox@nagarnetra.in")
        val updatedUser = user.copy(
            points = user.points + 10,
            reportsCount = user.reportsCount + 1
        )
        db.userDao().insertUser(updatedUser)
    }

    suspend fun insertLog(log: AuthorityLog) {
        db.authorityLogDao().insertLog(log)
        postToServer("/api/logs", logToJson(log))
    }

    suspend fun clearIssues() {
        db.issueDao().clearIssues()
        deleteFromServer("/api/issues")
    }

    suspend fun deleteDemoIssues() {
        db.issueDao().deleteDemoIssues()
    }

    suspend fun insertAiDecisionLog(log: AiDecisionLog) {
        db.aiDecisionLogDao().insertLog(log)
        postToServer("/api/ai-logs", aiLogToJson(log))
    }

    suspend fun insertEmailLog(log: EmailLog) {
        db.emailLogDao().insertLog(log)
    }

    suspend fun insertWardReport(report: WardReport) {
        db.wardReportDao().insertReport(report)
    }

    suspend fun clearAiDecisionLogs() {
        db.aiDecisionLogDao().clearLogs()
    }

    suspend fun clearEmailLogs() {
        db.emailLogDao().clearLogs()
    }

    suspend fun clearWardReports() {
        db.wardReportDao().clearReports()
    }

    suspend fun clearAuthorityLogs() {
        db.authorityLogDao().clearLogs()
    }
}
