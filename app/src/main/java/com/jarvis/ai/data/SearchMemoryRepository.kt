package com.jarvis.ai.data

import com.jarvis.ai.core.ArabicTextNormalizer
import com.jarvis.ai.data.db.AppDao
import com.jarvis.ai.data.db.SearchMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchMemoryRepository(
    private val dao: AppDao,
    private val embeddings: GeminiEmbeddingService
) {
    suspend fun find(query: String): SearchMemory? = withContext(Dispatchers.IO) {
        val normalized = ArabicTextNormalizer.normalize(query)
        if (normalized.isBlank()) return@withContext null
        val now = System.currentTimeMillis()

        // Exact match is retained as the fastest path.
        dao.findValidSearch(normalized, now)?.let { return@withContext it }

        // Semantic cache retrieval: compare the query embedding to cached
        // search embeddings. Old rows without vectors remain eligible for the
        // exact-match path and will be replaced with vectors when refreshed.
        val q = embeddings.embed(
            text = query,
            instruction = "Represent this query for semantic retrieval of a previously answered search."
        ) ?: return@withContext null

        dao.getValidSearchMemories(now)
            .asSequence()
            .mapNotNull { item ->
                val v = embeddings.decode(item.embeddingJson) ?: return@mapNotNull null
                item to embeddings.cosine(q, v)
            }
            .filter { it.second >= 0.88f }
            .maxByOrNull { it.second }
            ?.first
    }

    suspend fun save(
        query: String,
        answer: String,
        sourcesJson: String,
        ttlMillis: Long,
        timeSensitive: Boolean
    ) {
        val normalized = ArabicTextNormalizer.normalize(query)
        if (normalized.isBlank()) return
        val now = System.currentTimeMillis()
        val vector = embeddings.embed(
            text = query,
            instruction = "Represent this search query for semantic cache retrieval."
        )
        dao.insertSearchMemory(
            SearchMemory(
                normalizedQuery = normalized,
                query = query,
                answer = answer,
                sourcesJson = sourcesJson,
                createdAt = now,
                updatedAt = now,
                embeddingJson = vector?.let(embeddings::encode),
                expiresAt = now + ttlMillis,
                timeSensitive = timeSensitive
            )
        )
    }

    suspend fun deleteExpired(now: Long = System.currentTimeMillis()) =
        dao.deleteExpiredSearch(now)
}
