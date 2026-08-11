package com.jarvis.assistant

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Trimite mesaje către Claude API (Anthropic), cu tool-ul de web search activat
 * pentru research real, și un protocol simplu de "acțiuni" pe care le poate cere
 * modelul pentru a controla telefonul prin JarvisAccessibilityService.
 */
class ClaudeApiClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
        Ești Jarvis, asistentul personal de pe telefonul Android al utilizatorului.
        Poți face research pe web când e nevoie, folosind tool-ul de căutare disponibil.
        Dacă utilizatorul cere o acțiune pe telefon, răspunde normal în română, apoi adaugă
        la finalul mesajului, pe o linie nouă, un bloc EXACT în formatul:
        [ACTION:{"type":"open_app","package":"com.whatsapp"}]
        Tipuri de acțiuni disponibile:
        - open_app (cu "package": numele pachetului Android, ex: com.whatsapp, com.google.android.gm, com.google.android.apps.maps)
        - go_home
        - go_back
        - open_notifications
        Dacă nu e nevoie de nicio acțiune pe telefon, NU adaugi deloc blocul [ACTION:...].
        Fii concis, prietenos, și răspunde tot timpul în limba română.
    """.trimIndent()

    fun sendMessage(
        userMessage: String,
        history: List<Pair<String, String>>,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val messagesArray = JSONArray()
        for ((role, content) in history) {
            messagesArray.put(JSONObject().put("role", role).put("content", content))
        }
        messagesArray.put(JSONObject().put("role", "user").put("content", userMessage))

        val toolsArray = JSONArray().put(
            JSONObject()
                .put("type", "web_search_20250305")
                .put("name", "web_search")
        )

        val payload = JSONObject()
            .put("model", "claude-sonnet-4-6")
            .put("max_tokens", 1024)
            .put("system", systemPrompt)
            .put("messages", messagesArray)
            .put("tools", toolsArray)

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Eroare de rețea")
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    onError("Eroare API (${response.code}): $bodyStr")
                    return
                }
                try {
                    val json = JSONObject(bodyStr)
                    val contentArray = json.getJSONArray("content")
                    val textBuilder = StringBuilder()
                    for (i in 0 until contentArray.length()) {
                        val block = contentArray.getJSONObject(i)
                        if (block.optString("type") == "text") {
                            textBuilder.append(block.optString("text"))
                        }
                    }
                    onResult(textBuilder.toString())
                } catch (ex: Exception) {
                    onError("Eroare la parsare răspuns: ${ex.message}")
                }
            }
        })
    }
}
