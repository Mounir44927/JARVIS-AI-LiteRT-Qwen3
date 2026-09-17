package com.jarvis.ai.data

import com.jarvis.ai.data.db.AppDatabase
import com.jarvis.ai.data.db.Fact
import com.jarvis.ai.data.db.MasterProfile
import com.jarvis.ai.data.db.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MemoryRepository(
    private val db: AppDatabase,
    private val embeddings: GeminiEmbeddingService
) {
    private val dao = db.dao()

    suspend fun getProfile(): MasterProfile? = dao.getProfile()
    suspend fun saveProfile(profile: MasterProfile) = dao.upsertProfile(profile)
    fun observeFacts(): Flow<List<Fact>> = dao.observeFacts()
    suspend fun getFacts(): List<Fact> = dao.getFacts()

    suspend fun saveFact(fact: Fact) {
        val embedding = embeddings.embed(
            text = "${fact.category}: ${fact.content}",
            instruction = "Represent this durable user memory for semantic retrieval across languages."
        )
        dao.insertFact(fact.copy(embeddingJson = embedding?.let(embeddings::encode)))
    }

    suspend fun deleteFact(id: Long) = dao.deleteFact(id)
    suspend fun deleteAllMemory() { dao.deleteAllFacts(); dao.deleteAllMessages() }
    suspend fun saveMessage(message: Message) = dao.insertMessage(message)
    suspend fun recentMessages(sessionId: String, limit: Int = 20): List<Message> =
        dao.getRecentMessages(sessionId, limit).reversed()
    fun observeMessages(sessionId: String): Flow<List<Message>> = dao.observeMessages(sessionId)

    suspend fun findRelevantFacts(query: String, limit: Int = 8, minScore: Float = 0.55f): List<Fact> =
        withContext(Dispatchers.IO) {
            val queryEmbedding = embeddings.embed(
                text = query,
                instruction = "Represent this user query for retrieving semantically relevant memories."
            )
            val facts = dao.getAllFacts()
            if (queryEmbedding == null) {
                return@withContext facts.take(limit)
            }

            val scored = facts.map { fact ->
                var vector = embeddings.decode(fact.embeddingJson)
                if (vector == null) {
                    val generated = embeddings.embed(
                        text = "${fact.category}: ${fact.content}",
                        instruction = "Represent this durable user memory for semantic retrieval across languages."
                    )
                    if (generated != null) {
                        dao.updateFactEmbedding(fact.id, embeddings.encode(generated))
                        vector = generated
                    }
                }
                fact to embeddings.cosine(queryEmbedding, vector)
            }

            scored
                .filter { it.second >= minScore }
                .sortedByDescending { it.second }
                .take(limit)
                .map { it.first }
        }
}
