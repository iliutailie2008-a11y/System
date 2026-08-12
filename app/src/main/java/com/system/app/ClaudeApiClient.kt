package com.system.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Minimal client for the Anthropic Messages API.
 * The API key is injected at build time from the CLAUDE_API_KEY GitHub
 * Actions secret (see BuildConfig.CLAUDE_API_KEY) — it never lives in the repo.
 */
object ClaudeApiClient {

    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-sonnet-4-6"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT = """
Ești System, un asistent personal inspirat de JARVIS din Iron Man.
Vorbești calm, eficient, la obiect, fără cuvinte în plus. Îi spui
utilizatorului "Master". Ești bilingv română/engleză — răspunzi în limba
în care ți se scrie. Ești specializat pe eCommerce/business și pe
mecanică-electrică auto (utilizatorul e începător, ești mentorul lui),
dar poți răspunde competent la orice domeniu. Ai un mic firescul umor sec,
folosit rar, nu constant.
""".trimIndent()

    /**
     * Sends the conversation so far and returns System's reply text.
     * history: list of Pair(isUser, text) in chronological order.
     */
    suspend fun sendMessage(history: List<Pair<Boolean, String>>): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val messagesJson = JSONArray()
                for ((isUser, text) in history) {
                    val msg = JSONObject()
                    msg.put("role", if (isUser) "user" else "assistant")
                    msg.put("content", text)
                    messagesJson.put(msg)
                }

                val body = JSONObject().apply {
                    put("model", MODEL)
                    put("max_tokens", 1024)
                    put("system", SYSTEM_PROMPT)
                    put("messages", messagesJson)
                }

                val requestBody = body.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(ENDPOINT)
                    .addHeader("x-api-key", BuildConfig.CLAUDE_API_KEY)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("content-type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("System nu a putut răspunde (HTTP ${response.code}): $responseBody")
                        )
                    }
                    val json = JSONObject(responseBody)
                    val content = json.getJSONArray("content")
                    val textBuilder = StringBuilder()
                    for (i in 0 until content.length()) {
                        val block = content.getJSONObject(i)
                        if (block.optString("type") == "text") {
                            textBuilder.append(block.optString("text"))
                        }
                    }
                    Result.success(textBuilder.toString().ifBlank { "..." })
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
