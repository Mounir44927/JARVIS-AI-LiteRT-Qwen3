package com.jarvis.ai.voice

import com.jarvis.ai.core.ArabicTextNormalizer

/**
 * MVP anti-accidental-activation layer.
 * This is not biometric authentication and must not be presented as such.
 */
class VoiceLock(private val maxAttempts: Int = 3, private val threshold: Double = 0.88) {
    private var attempts = 0

    data class Result(val accepted: Boolean, val confidence: Double, val attemptsLeft: Int)

    fun verify(transcript: String, wakePhrase: String): Result {
        if (attempts >= maxAttempts) return Result(false, 0.0, 0)
        attempts++
        val confidence = ArabicTextNormalizer.similarity(transcript, wakePhrase)
        return Result(confidence >= threshold, confidence, (maxAttempts - attempts).coerceAtLeast(0))
    }

    fun reset() { attempts = 0 }
}
