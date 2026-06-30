package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class EmailJsClient {

    companion object {
        private const val TAG = "NagarNetraEmail"
        private const val EMAILJS_SEND_URL = "https://api.emailjs.com/api/v1.0/email/send"

        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }

    private fun isKeyValid(key: String?): Boolean {
        return !key.isNullOrEmpty() &&
               key != "your-emailjs-service-id" &&
               key != "your-emailjs-template-id" &&
               key != "your-emailjs-public-key" &&
               !key.contains("placeholder", ignoreCase = true)
    }

    suspend fun sendAccountabilityEmail(
        toEmail: String,
        toName: String,
        subject: String,
        htmlContent: String
    ): Boolean = withContext(Dispatchers.IO) {
        val serviceId = BuildConfig.EMAILJS_SERVICE_ID
        val templateId = BuildConfig.EMAILJS_TEMPLATE_ID
        val publicKey = BuildConfig.EMAILJS_PUBLIC_KEY

        val realSendingEnabled = isKeyValid(serviceId) && isKeyValid(templateId) && isKeyValid(publicKey)

        if (!realSendingEnabled) {
            Log.i(TAG, "🤖 [SIMULATOR ACTIVE] Email simulated successfully to $toEmail ($toName)")
            Log.d(TAG, "Subject: $subject")
            Log.d(TAG, "HTML Content Snippet: ${htmlContent.take(200)}...")
            return@withContext true
        }

        try {
            // Standard EmailJS POST JSON Structure
            val jsonBody = JSONObject().apply {
                put("service_id", serviceId)
                put("template_id", templateId)
                put("user_id", publicKey)
                put("template_params", JSONObject().apply {
                    put("to_email", toEmail)
                    put("to_name", toName)
                    put("subject", subject)
                    put("message_html", htmlContent) // Dynamic HTML body from Gemini
                })
            }

            val request = Request.Builder()
                .url(EMAILJS_SEND_URL)
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d(TAG, "→ Sending email via EmailJS API to $toEmail...")
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Log.i(TAG, "← EmailJS dispatch successful: $responseBody")
                true
            } else {
                Log.e(TAG, "← EmailJS dispatch failed (${response.code}): $responseBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching EmailJS request: ${e.message}", e)
            false
        }
    }
}
