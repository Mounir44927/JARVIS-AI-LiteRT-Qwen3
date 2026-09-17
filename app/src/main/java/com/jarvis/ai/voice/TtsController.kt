package com.jarvis.ai.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

/** Offline JARVIS English voice with a safe Android TTS fallback. */
class TtsController(
    private val context: Context,
    private val onReady: (Boolean) -> Unit,
    private val onSpeechFinished: () -> Unit = {}
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val main = Handler(Looper.getMainLooper())
    private val speechMutex = Mutex()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE)
            }
        }
        .build()
    private val focusRequest: AudioFocusRequest? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { }
            .build()
    } else null

    @Volatile private var jarvisReady = false
    @Volatile private var initializationStarted = false
    private var jarvis: OfflineTts? = null
    @Volatile private var audioTrack: AudioTrack? = null
    @Volatile private var androidTts: TextToSpeech? = null
    @Volatile private var androidTtsReady = false

    @Volatile var speechRate: Float = 0.95f

    fun speak(text: String) {
        val prepared = prepareEnglish(text)
        if (prepared.isBlank()) {
            main.post(onSpeechFinished)
            return
        }

        if (jarvisReady) {
            speakWithJarvis(prepared)
            return
        }

        // Initialize on first real speech request, not during ViewModel construction.
        ensureJarvisInitialized {
            if (jarvisReady) {
                speakWithJarvis(prepared)
            } else {
                speakWithAndroidFallback(prepared)
            }
        }
    }

    private fun ensureJarvisInitialized(onComplete: () -> Unit) {
        if (initializationStarted) {
            scope.launch {
                repeat(120) {
                    if (jarvisReady) return@launch
                    kotlinx.coroutines.delay(50)
                }
                main.post(onComplete)
            }
            return
        }

        initializationStarted = true
        scope.launch {
            try {
                val config = OfflineTtsConfig(
                    model = OfflineTtsModelConfig(
                        vits = OfflineTtsVitsModelConfig(
                            model = "jarvis/jarvis-high.onnx",
                            tokens = "jarvis/tokens.txt",
                            dataDir = "jarvis/espeak-ng-data",
                            noiseScale = 0.667f,
                            noiseScaleW = 0.8f,
                            lengthScale = 1.15f
                        ),
                        numThreads = 2,
                        debug = false,
                        provider = "cpu"
                    ),
                    maxNumSentences = 1,
                    silenceScale = 0.2f
                )
                jarvis = OfflineTts(context.assets, config)
                jarvisReady = true
                Log.i(TAG, "JARVIS English TTS initialized")
            } catch (t: Throwable) {
                jarvisReady = false
                Log.e(TAG, "JARVIS English model failed to load; using Android TTS fallback", t)
            }
            main.post {
                onReady(jarvisReady)
                onComplete()
            }
        }
    }

    private fun speakWithJarvis(text: String) {
        scope.launch {
            var handedOffToFallback = false
            try {
                speechMutex.withLock {
                    speakJarvis(text) { handedOffToFallback = true }
                }
            } finally {
                if (!handedOffToFallback) {
                    main.post(onSpeechFinished)
                }
            }
        }
    }

    private suspend fun speakJarvis(text: String, onFallback: () -> Unit) {
        if (!requestAudioFocus()) return
        var track: AudioTrack? = null
        try {
            stopCurrentTrack()
            val engine = jarvis ?: return
            val audio = engine.generate(
                text = text,
                sid = 0,
                speed = speechRate.coerceIn(0.75f, 1.15f)
            )
            val pcm = ShortArray(audio.samples.size) { i ->
                (audio.samples[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            }
            if (pcm.isEmpty()) return

            val minBuffer = AudioTrack.getMinBufferSize(
                audio.sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).takeIf { it > 0 }?.coerceAtLeast(pcm.size * 2) ?: (pcm.size * 2)

            track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(audio.sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuffer)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            check(track.state == AudioTrack.STATE_INITIALIZED) { "AudioTrack initialization failed" }
            audioTrack = track
            check(track.write(pcm, 0, pcm.size) > 0) { "AudioTrack write failed" }

            val finished = CompletableDeferred<Unit>()
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(audioTrack: AudioTrack) { finished.complete(Unit) }
                override fun onPeriodicNotification(audioTrack: AudioTrack) = Unit
            }, main)
            track.notificationMarkerPosition = pcm.size
            track.play()
            finished.await()
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Log.e(TAG, "JARVIS playback failed; falling back to Android TTS", t)
            onFallback()
            main.post { speakWithAndroidFallback(text) }
        } finally {
            releaseTrack(track)
            abandonAudioFocus()
        }
    }

    private fun speakWithAndroidFallback(text: String) {
        main.post {
            try {
                val tts = androidTts ?: TextToSpeech(context.applicationContext) { status ->
                    androidTtsReady = status == TextToSpeech.SUCCESS
                    if (androidTtsReady) {
                        configureAndroidTts()
                        speakWithAndroidFallback(text)
                    } else {
                        Log.e(TAG, "Android TTS fallback initialization failed")
                        onSpeechFinished()
                    }
                }.also { androidTts = it }

                if (!androidTtsReady) {
                    if (tts.isSpeaking) tts.stop()
                    configureAndroidTts()
                }
                if (androidTtsReady) {
                    tts.setSpeechRate(speechRate.coerceIn(0.75f, 1.15f))
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String) = Unit

                        override fun onDone(utteranceId: String) {
                            main.post(onSpeechFinished)
                        }

                        @Deprecated("Deprecated in Android SDK")
                        override fun onError(utteranceId: String) {
                            main.post(onSpeechFinished)
                        }

                        override fun onError(utteranceId: String, errorCode: Int) {
                            main.post(onSpeechFinished)
                        }
                    })
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis-fallback")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Android TTS fallback failed", t)
                onSpeechFinished()
            }
        }
    }

    private fun configureAndroidTts() {
        val tts = androidTts ?: return
        val result = tts.setLanguage(Locale.UK)
        androidTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun stopCurrentTrack() {
        audioTrack?.let { current -> releaseTrack(current) }
    }

    private fun releaseTrack(track: AudioTrack?) {
        if (track == null) return
        if (audioTrack === track) audioTrack = null
        runCatching { track.stop() }
        runCatching { track.flush() }
        runCatching { track.release() }
    }

    fun shutdown() {
        scope.cancel()
        releaseTrack(audioTrack)
        audioTrack = null
        abandonAudioFocus()
        runCatching { androidTts?.stop() }
        runCatching { androidTts?.shutdown() }
        androidTts = null
        androidTtsReady = false
        jarvis?.release()
        jarvis = null
        jarvisReady = false
    }

    private fun prepareEnglish(input: String): String = EnglishSpeechFilter.prepare(input)

    companion object { private const val TAG = "JarvisTTS" }
}
