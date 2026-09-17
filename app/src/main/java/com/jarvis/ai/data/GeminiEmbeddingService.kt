package com.jarvis.ai.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * Gemini semantic embedding client.
 *
 * Uses the current stable text/multimodal embedding model for semantic retrieval.
 * We keep the vector as JSON because this app's corpus is small enough for an
 * on-device linear scan and does not justify a native vector database yet.
 */
class GeminiEmbeddingService(
    private val keyStore: GeminiApiKeyStore,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    suspend fun embed(text: String, instruction: String): FloatArray? {
        val key = keyStore.getApiKey()?.trim().orEmpty()
        val clean = text.trim()
        if (key.isBlank() || clean.isBlank()) return null

        val body = JSONObject().apply {
            put("content", JSONObject().put(
                "parts", JSONArray().put(
                    JSONObject().put("text", "$instruction\n$clean")
                )
            ))
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-2:embedContent")
            .header("x-goog-api-key", key)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IOException("Embedding HTTP ${response.code}: $raw")
                val root = JSONObject(raw)
                val values = root.optJSONObject("embedding")?.optJSONArray("values")
                    ?: root.optJSONArray("embeddings")?.optJSONObject(0)?.optJSONArray("values")
                    ?: throw IOException("No embedding values")
                FloatArray(values.length()) { i -> values.getDouble(i).toFloat() }.normalize()
            }
        }.getOrNull()
    }

    fun encode(vector: FloatArray): String =
        JSONArray().apply { vector.forEach { put(it.toDouble()) } }.toString()

    fun decode(json: String?): FloatArray? = runCatching {
        if (json.isNullOrBlank()) return null
        val a = JSONArray(json)
        FloatArray(a.length()) { i -> a.getDouble(i).toFloat() }
    }.getOrNull()

    fun cosine(a: FloatArray?, b: FloatArray?): Float {
        if (a == null || b == null || a.isEmpty() || a.size != b.size) return -1f
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        if (na == 0.0 || nb == 0.0) return -1f
        return (dot / (sqrt(na) * sqrt(nb))).toFloat()
    }

    private fun FloatArray.normalize(): FloatArray {
        var sum = 0.0
        for (v in this) sum += v * v
        val norm = sqrt(sum)
        if (norm == 0.0) return this
        for (i in indices) this[i] = (this[i] / norm).toFloat()
        return this
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
