package com.jarvis.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechRecognizerController(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null
    private var retried = false

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onRmsChanged: (Float) -> Unit = {}
    ) {
        retried = false
        startInternal(onResult, onError, onRmsChanged)
    }

    private fun startInternal(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onRmsChanged: (Float) -> Unit
    ) {
        if (!isAvailable()) {
            onError("التعرف الصوتي غير متاح حاليًا. يمكنك استخدام الكتابة.")
            return
        }

        destroy()
        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { speech ->
                speech.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = onRmsChanged(rmsdB)
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit

                    override fun onResults(results: Bundle?) {
                        val value = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        destroy()
                        if (value.isBlank()) onError("لم أفهم الصوت. حاول مرة أخرى أو استخدم الكتابة.")
                        else onResult(value)
                    }

                    override fun onError(error: Int) {
                        val temporary = error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        if (temporary && !retried) {
                            retried = true
                            startInternal(onResult, onError, onRmsChanged)
                        } else {
                            destroy()
                            onError(mapError(error))
                        }
                    }
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                speech.startListening(intent)
            }
        } catch (t: Throwable) {
            destroy()
            onError("تعذر بدء الميكروفون. تحقق من إذن الميكروفون ثم حاول مرة أخرى.")
        }
    }

    fun destroy() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun mapError(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "تعذر الوصول إلى الميكروفون."
        SpeechRecognizer.ERROR_CLIENT -> "تعذر بدء التعرف الصوتي."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "إذن الميكروفون غير متاح."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "تعذر الوصول إلى خدمة التعرف الصوتي."
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم أفهم ما قيل. حاول مرة أخرى."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "خدمة التعرف الصوتي مشغولة. حاول بعد لحظة."
        SpeechRecognizer.ERROR_SERVER -> "حدث خطأ في خدمة التعرف الصوتي."
        else -> "تعذر استخدام التعرف الصوتي حاليًا."
    }
}
