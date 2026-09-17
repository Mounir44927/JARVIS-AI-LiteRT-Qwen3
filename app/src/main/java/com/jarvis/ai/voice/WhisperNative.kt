package com.jarvis.ai.voice

/**
 * Tiny Kotlin/JNI facade over the pinned whisper.cpp native layer.
 *
 * The shared object is intentionally loaded lazily so app startup does not pay native/model cost.
 */
object WhisperNative {
    @Volatile
    private var loaded = false

    @Synchronized
    fun ensureLoaded() {
        if (!loaded) {
            System.loadLibrary("jarvis_whisper")
            loaded = true
        }
    }

    external fun initContext(modelPath: String): Long
    external fun freeContext(contextHandle: Long)
    external fun transcribeArabic(
        contextHandle: Long,
        audioPcmFloat32: FloatArray,
        numThreads: Int
    ): String

    external fun cancel(contextHandle: Long)
    external fun systemInfo(): String
}
