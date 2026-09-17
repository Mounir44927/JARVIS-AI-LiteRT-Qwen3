package com.jarvis.ai.brain

import com.jarvis.ai.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class BackendGeminiGateway(private val client: OkHttpClient = OkHttpClient()) : AiGateway {
    override suspend fun answer(systemPrompt: String, context: String, userPrompt: String, search: Boolean): Result<AiAnswer> {
        val base = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
        if (!BackendEndpointValidator.isValidBaseUrl(base)) return Result.failure(IOException("Backend URL is invalid"))
        val payload = JSONObject().apply {
            put("systemPrompt", systemPrompt)
            put("context", context)
            put("prompt", userPrompt)
            put("search", search)
        }
        val request = Request.Builder()
            .url("$base/v1/answer")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw GeminiHttpException(response.code, response.body?.string().orEmpty())
                val body = JSONObject(response.body?.string().orEmpty())
                val sources = mutableListOf<SourceRef>()
                body.optJSONArray("sources")?.let { array ->
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        sources += SourceRef(obj.optString("title", "مصدر"), obj.optString("url"))
                    }
                }
                AiAnswer(body.optString("text"), sources.filter { it.url.isNotBlank() }, body.optBoolean("usedSearch", search))
            }
        }
    }
}
