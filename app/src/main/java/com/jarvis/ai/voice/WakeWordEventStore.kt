package com.jarvis.ai.voice

import android.content.Context

/**
 * Small durable hand-off between the foreground voice service and MainActivity.
 * The service records a detection before broadcasting it, so a detection that
 * happens while the activity is stopped is consumed on the next onStart().
 */
class WakeWordEventStore(context: Context) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun markPending(score: Float) {
        prefs.edit()
            .putBoolean(KEY_PENDING, true)
            .putFloat(KEY_SCORE, score)
            .apply()
    }

    fun consumePending(): Float? {
        if (!prefs.getBoolean(KEY_PENDING, false)) return null
        val score = prefs.getFloat(KEY_SCORE, 0f)
        prefs.edit().clear().apply()
        return score
    }

    companion object {
        private const val FILE_NAME = "wake_word_events"
        private const val KEY_PENDING = "pending"
        private const val KEY_SCORE = "score"
    }
}
