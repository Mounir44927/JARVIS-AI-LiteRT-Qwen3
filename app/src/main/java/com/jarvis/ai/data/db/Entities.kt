package com.jarvis.ai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MasterProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val title: String,
    val wakePhrase: String,
    val voiceProfileVersion: Int = 1,
    val voiceProfileData: ByteArray? = null,
    val voicePitch: Float = 1.12f,
    val voiceRate: Float = 0.95f
)

@Entity
data class Fact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val content: String,
    val source: String,
    val confidence: Float = 1f,
    val createdAt: Long,
    val updatedAt: Long,
    val embeddingJson: String? = null
)

@Entity
data class SearchMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val normalizedQuery: String,
    val query: String,
    val answer: String,
    val sourcesJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val embeddingJson: String? = null,
    val expiresAt: Long,
    val timeSensitive: Boolean = false
)

@Entity
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val sessionId: String,
    val usedSearch: Boolean,
    val createdAt: Long
)
