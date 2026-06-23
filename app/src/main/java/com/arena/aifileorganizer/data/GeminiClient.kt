package com.arena.aifileorganizer.data

import android.util.Log
import com.arena.aifileorganizer.model.GeminiContent
import com.arena.aifileorganizer.model.GeminiPart
import com.arena.aifileorganizer.model.GeminiRequest
import com.arena.aifileorganizer.model.GeminiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiClient(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    suspend fun generate(prompt: String, model: String = "gemini-1.5-flash-latest"): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val body = GeminiRequest(listOf(GeminiContent(listOf(GeminiPart(prompt)))))
            val requestBody = json.encodeToString(body).toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            client.newCall(request).execute().use { resp ->
                val txt = resp.body?.string() ?: return@withContext null
                if (!resp.isSuccessful) { Log.e("Gemini", "HTTP ${resp.code}: $txt"); return@withContext null }
                val parsed = json.decodeFromString<GeminiResponse>(txt)
                return@withContext parsed.candidates?.firstOrNull()?.content?.parts?.joinToString("\n") { it.text }
            }
        } catch (e: Exception) { Log.e("Gemini", "error", e); null }
    }
}
