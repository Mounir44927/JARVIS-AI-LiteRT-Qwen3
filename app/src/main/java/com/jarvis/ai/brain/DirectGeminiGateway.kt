package com.jarvis.ai.brain

import com.jarvis.ai.data.GeminiApiKeyStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class DirectGeminiGateway(
    private val keyStore: GeminiApiKeyStore,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : AiGateway {
    override suspend fun answer(
        systemPrompt: String,
        context: String,
        userPrompt: String,
        search: Boolean
    ): Result<AiAnswer> {
        val key = keyStore.getApiKey()?.trim().orEmpty()
        if (key.isBlank()) return Result.failure(MissingApiKeyException())

        val contents = JSONArray().put(
            JSONObject().apply {
                put("role", "user")
                put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put("text", buildString {
                            if (context.isNotBlank()) append("Context:\n").append(context).append("\n\n")
                            append("User request:\n").append(userPrompt)
                        })
                    )
                )
            }
        )

        val body = JSONObject().apply {
            put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
            put("contents", contents)
            if (search) {
                put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
            }
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/${MODEL_ID}:generateContent"
        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", key)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw GeminiHttpException(response.code, responseBody)
                parseResponse(responseBody, search)
            }
        }
    }

    private fun parseResponse(raw: String, usedSearch: Boolean): AiAnswer {
        val root = JSONObject(raw)
        val candidates = root.optJSONArray("candidates") ?: throw IOException("No candidates")
        val first = candidates.optJSONObject(0) ?: throw IOException("Empty candidates")
        val content = first.optJSONObject("content") ?: throw IOException("No content")
        val parts = content.optJSONArray("parts") ?: JSONArray()
        val text = buildString {
            for (i in 0 until parts.length()) {
                parts.optJSONObject(i)?.optString("text")?.takeIf { it.isNotBlank() }?.let(::append)
            }
        }.trim()
        if (text.isBlank()) {
            val blocked = first.optJSONObject("finishReason")?.optString("reason").orEmpty()
            throw IOException(if (blocked.isNotBlank()) "Gemini returned no text: $blocked" else "Empty answer")
        }

        val sources = mutableListOf<SourceRef>()
        val grounding = first.optJSONObject("groundingMetadata")
        grounding?.optJSONArray("groundingChunks")?.let { chunks ->
            for (i in 0 until chunks.length()) {
                val web = chunks.optJSONObject(i)?.optJSONObject("web") ?: continue
                val title = web.optString("title", "Source")
                val uri = web.optString("uri")
                if (uri.isNotBlank()) sources += SourceRef(title, uri)
            }
        }
        return AiAnswer(text = text, sources = sources.distinctBy { it.url }, usedSearch = usedSearch)
    }

    companion object {
        const val MODEL_ID = "gemini-2.5-flash"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

class MissingApiKeyException : IOException("Gemini API key is not configured")
class GeminiHttpException(val code: Int, body: String) : IOException("Gemini HTTP $code: $body")
