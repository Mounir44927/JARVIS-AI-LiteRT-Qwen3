package com.jarvis.ai.voice

object VoiceLevelNormalizer {
    fun normalize(rmsDb: Float): Float {
        if (!rmsDb.isFinite()) return 0f
        return ((rmsDb + 2f) / 14f).coerceIn(0f, 1f)
    }

    fun smooth(previous: Float, current: Float, attack: Float = 0.25f, release: Float = 0.12f): Float {
        val alpha = if (current >= previous) attack else release
        return (previous + (current - previous) * alpha).coerceIn(0f, 1f)
    }

    fun reset(): Float = 0f
}
