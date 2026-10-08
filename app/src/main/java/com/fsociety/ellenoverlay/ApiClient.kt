package com.fsociety.ellenoverlay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun chat(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        history: List<Pair<String, String>>
    ): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "API key не задан." }
        require(baseUrl.isNotBlank()) { "Base URL не задан." }
        require(model.isNotBlank()) { "Model не задан." }

        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })
        history.takeLast(30).forEach { (role, content) ->
            messages.put(JSONObject().apply {
                put("role", role)
                put("content", content)
            })
        }

        val body = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", 0.8)
        }.toString()

        val url = baseUrl.trimEnd('/') + "/chat/completions"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    JSONObject(raw).optJSONObject("error")?.optString("message")
                }.getOrNull()
                throw IllegalStateException(message?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}")
            }

            val json = JSONObject(raw)
            val choices = json.optJSONArray("choices")
                ?: throw IllegalStateException("API не вернул choices.")
            if (choices.length() == 0) throw IllegalStateException("API вернул пустой ответ.")

            choices.getJSONObject(0)
                .optJSONObject("message")
                ?.optString("content")
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("API не вернул текст ответа.")
        }
    }
}
