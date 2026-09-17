package com.jarvis.ai.voice

import android.content.Context
import android.util.Log
import com.rementia.openwakeword.lib.WakeWordEngine
import com.rementia.openwakeword.lib.model.WakeWordModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Real local wake-word detector.
 *
 * Current bundled pretrained model detects the English phrase "Hey Jarvis".
 * The bundled model is English-only and uses the fixed wake phrase "Hey Jarvis".
 */
interface WakeWordController {
    fun enable()
    fun disable()
    fun isEnabled(): Boolean
    fun matches(transcript: String, wakePhrase: String): Boolean
    fun release()
}

class LocalWakeWordController : WakeWordController {
    private var enabled = false
    override fun enable() { enabled = true }
    override fun disable() { enabled = false }
    override fun isEnabled(): Boolean = enabled
    override fun matches(transcript: String, wakePhrase: String): Boolean =
        com.jarvis.ai.core.ArabicTextNormalizer.similarity(transcript, wakePhrase) >= 0.88
    override fun release() = Unit
}

/**
 * ONNX Runtime / openWakeWord implementation.
 * Audio capture and inference are performed by the library off the main thread.
 */
class OpenWakeWordController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onDetected: (score: Float) -> Unit
) : WakeWordController {

    companion object {
        const val MODEL_NAME = "Hey Jarvis"
        const val SUPPORTED_WAKE_PHRASE = "Hey Jarvis"
        const val MODEL_ASSET = "hey_jarvis_v0.1.onnx"

        // Start conservative; tune upward for fewer false activations.
        const val DEFAULT_THRESHOLD = 0.60f
    }

    private var enabled = false
    private var engine: WakeWordEngine? = null
    private var collectionStarted = false

    override fun enable() {
        if (enabled) return
        try {
            val modelAvailable = runCatching {
                context.assets.open(MODEL_ASSET).use { it.available() > 0 }
            }.getOrDefault(false)
            require(modelAvailable) {
                "Wake-word model asset is missing or empty: $MODEL_ASSET. Run scripts/setup_wakeword.sh before building."
            }

            val detector = engine ?: WakeWordEngine(
                context = context,
                models = listOf(
                    WakeWordModel(
                        name = MODEL_NAME,
                        modelPath = MODEL_ASSET,
                        threshold = DEFAULT_THRESHOLD
                    )
                ),
                detectionCooldownMs = 2500L
            ).also { engine = it }

            if (!collectionStarted) {
                collectionStarted = true
                scope.launch {
                    detector.detections
                        .catch { error ->
                            Log.e("JarvisWakeWord", "Detection stream failed", error)
                            enabled = false
                        }
                        .collect { detection ->
                            if (enabled) onDetected(detection.score)
                        }
                }
            }

            detector.start()
            enabled = true
            Log.i("JarvisWakeWord", "Wake word listener started")
        } catch (t: Throwable) {
            enabled = false
            Log.e("JarvisWakeWord", "Failed to start wake word listener", t)
        }
    }

    override fun disable() {
        if (!enabled) return
        try {
            engine?.stop()
        } catch (t: Throwable) {
            Log.w("JarvisWakeWord", "Failed to stop wake word listener", t)
        } finally {
            enabled = false
        }
    }

    override fun isEnabled(): Boolean = enabled

    override fun matches(transcript: String, wakePhrase: String): Boolean =
        com.jarvis.ai.core.ArabicTextNormalizer.similarity(transcript, wakePhrase) >= 0.88

    override fun release() {
        try {
            engine?.release()
        } catch (_: Throwable) {
        } finally {
            engine = null
            enabled = false
            collectionStarted = false
        }
    }
}
