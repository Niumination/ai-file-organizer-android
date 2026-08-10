package com.arena.aifileorganizer.data

import android.util.Log
import com.arena.aifileorganizer.model.GeminiContent
import com.arena.aifileorganizer.model.GeminiPart
import com.arena.aifileorganizer.model.GeminiRequest
import com.arena.aifileorganizer.model.GeminiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Gemini REST client with a model fallback chain.
 *
 * `gemini-1.5-flash-latest` and the `*-latest` aliases have been retired, so this
 * client tries current flash models in order and degrades gracefully when one is
 * unavailable (HTTP 400/404/429). API-key errors (401/403) abort immediately so
 * the user gets a useful message instead of 13 wasted retries.
 */
class GeminiClient(private val apiKey: String) {

    sealed interface Result {
        data class Ok(val text: String) : Result
        data class Err(val httpCode: Int?, val message: String) : Result
    }

    companion object {
        private const val TAG = "GeminiClient"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        /** Tried in order until one answers. */
        val MODEL_CHAIN = listOf(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite"
        )
    }

    private val client = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Generate text. Tries each model in [MODEL_CHAIN] (or [model] alone when given).
     * Never wraps API-key failures in retries.
     */
    suspend fun generate(prompt: String, model: String? = null): Result = withContext(Dispatchers.IO) {
        val chain = if (model.isNullOrBlank()) MODEL_CHAIN else listOf(model)
        var lastErr: Result.Err = Result.Err(null, "Tidak ada respons dari Gemini")
        for (m in chain) {
            var attempt = 0
            while (attempt < 2) {
                attempt++
                when (val r = requestOnce(prompt, m)) {
                    is Result.Ok -> return@withContext r
                    is Result.Err -> {
                        lastErr = r.copy(message = "${r.message} (model $m)")
                        // Invalid key / permission → useless to try other models.
                        if (r.httpCode == 401 || r.httpCode == 403) return@withContext lastErr
                        // Rate limited → wait once, retry the same model, else move on.
                        if (r.httpCode == 429 && attempt < 2) {
                            delay(3_000)
                        } else {
                            break
                        }
                    }
                }
            }
        }
        lastErr
    }

    private fun requestOnce(prompt: String, model: String): Result {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val body = GeminiRequest(listOf(GeminiContent(listOf(GeminiPart(prompt)))))
            val requestBody = json.encodeToString(body).toRequestBody(JSON_MEDIA)
            val request = Request.Builder().url(url).post(requestBody).build()
            client.newCall(request).execute().use { resp ->
                val txt = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val serverMsg = runCatching {
                        JSONObject(txt).optJSONObject("error")?.optString("message")
                    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "HTTP ${resp.code}"
                    Log.w(TAG, "model=$model HTTP ${resp.code}: $serverMsg")
                    return Result.Err(resp.code, humanizeHttpError(resp.code, serverMsg))
                }
                val parsed = runCatching {
                    json.decodeFromString(GeminiResponse.serializer(), txt)
                }.getOrNull()
                val text = parsed?.candidates?.firstOrNull()?.content?.parts
                    ?.joinToString("\n") { it.text }
                if (text.isNullOrBlank()) Result.Err(resp.code, "Model mengembalikan respons kosong")
                else Result.Ok(text)
            }
        } catch (e: UnknownHostException) {
            Result.Err(null, "Tidak ada koneksi internet")
        } catch (e: SocketTimeoutException) {
            Result.Err(null, "Koneksi ke Gemini timeout")
        } catch (e: IOException) {
            Result.Err(null, "Gangguan jaringan: ${e.message ?: "IO error"}")
        } catch (e: Exception) {
            Log.e(TAG, "unexpected error", e)
            Result.Err(null, e.message ?: "Error tidak dikenal")
        }
    }

    private fun humanizeHttpError(code: Int, serverMsg: String): String = when (code) {
        400, 404 -> "Model tidak tersedia untuk API key ini"
        401, 403 -> "API key tidak valid — periksa kembali di Google AI Studio"
        429 -> "Kuota Gemini habis/terlalu sering — coba beberapa saat lagi"
        in 500..599 -> "Server Gemini sedang bermasalah — coba lagi nanti"
        else -> serverMsg
    }
}
