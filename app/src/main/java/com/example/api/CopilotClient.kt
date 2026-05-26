package com.example.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit

object CopilotClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun generateContent(
        prompt: String,
        systemPrompt: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject()
            val messagesArray = JSONArray()

            if (systemPrompt != null) {
                val sysMessage = JSONObject()
                sysMessage.put("role", "system")
                sysMessage.put("content", systemPrompt)
                messagesArray.put(sysMessage)
            }

            val userMessage = JSONObject()
            userMessage.put("role", "user")
            userMessage.put("content", prompt)
            messagesArray.put(userMessage)

            jsonBody.put("messages", messagesArray)
            jsonBody.put("model", "openai") // Can be openai, mistral, llama
            
            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("https://text.pollinations.ai/")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string() ?: "Empty response"
            } else {
                "Copilot API error: ${response.code} ${response.message}"
            }
        } catch (e: Exception) {
            "Copilot connection error: ${e.message}"
        }
    }
}
