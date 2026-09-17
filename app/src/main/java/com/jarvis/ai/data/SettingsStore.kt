package com.jarvis.ai.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jarvis_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val memoryLearningEnabled = booleanPreferencesKey("memory_learning_enabled")
        val memoryTtlDays = intPreferencesKey("memory_ttl_days")
        val speechRate = floatPreferencesKey("speech_rate")
        val alwaysListeningEnabled = booleanPreferencesKey("always_listening_enabled")
    }

    val learningEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.memoryLearningEnabled] ?: true }
    val memoryTtlDays: Flow<Int> = context.dataStore.data.map { it[Keys.memoryTtlDays] ?: 30 }
    val speechRate: Flow<Float> = context.dataStore.data.map { it[Keys.speechRate] ?: 0.95f }
    val alwaysListeningEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.alwaysListeningEnabled] ?: false }

    suspend fun setLearningEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.memoryLearningEnabled] = enabled } }
    suspend fun setMemoryTtlDays(days: Int) { context.dataStore.edit { it[Keys.memoryTtlDays] = days.coerceIn(1, 365) } }
    suspend fun setSpeechRate(rate: Float) { context.dataStore.edit { it[Keys.speechRate] = rate.coerceIn(0.5f, 2f) } }
    suspend fun setAlwaysListeningEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.alwaysListeningEnabled] = enabled } }
}
