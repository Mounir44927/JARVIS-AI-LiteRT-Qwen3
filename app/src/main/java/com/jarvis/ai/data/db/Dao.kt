package com.jarvis.ai.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM MasterProfile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): MasterProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: MasterProfile)

    @Query("SELECT * FROM Fact ORDER BY updatedAt DESC")
    fun observeFacts(): Flow<List<Fact>>

    @Query("SELECT * FROM Fact ORDER BY updatedAt DESC")
    suspend fun getFacts(): List<Fact>

    @Query("SELECT * FROM Fact")
    suspend fun getAllFacts(): List<Fact>

    @Query("UPDATE Fact SET embeddingJson = :embeddingJson WHERE id = :id")
    suspend fun updateFactEmbedding(id: Long, embeddingJson: String)

    @Insert
    suspend fun insertFact(fact: Fact)

    @Query("DELETE FROM Fact WHERE id = :id")
    suspend fun deleteFact(id: Long)

    @Query("DELETE FROM Fact")
    suspend fun deleteAllFacts()

    @Query("SELECT * FROM SearchMemory WHERE normalizedQuery = :query AND expiresAt > :now LIMIT 1")
    suspend fun findValidSearch(query: String, now: Long): SearchMemory?

    @Query("SELECT * FROM SearchMemory WHERE expiresAt > :now ORDER BY updatedAt DESC")
    suspend fun getValidSearchMemories(now: Long): List<SearchMemory>

    @Insert
    suspend fun insertSearchMemory(memory: SearchMemory)

    @Query("DELETE FROM SearchMemory WHERE expiresAt <= :now")
    suspend fun deleteExpiredSearch(now: Long)

    @Query("SELECT * FROM Message WHERE sessionId = :sessionId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMessages(sessionId: String, limit: Int): List<Message>

    @Query("SELECT * FROM Message WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observeMessages(sessionId: String): Flow<List<Message>>

    @Insert
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM Message")
    suspend fun deleteAllMessages()
}
