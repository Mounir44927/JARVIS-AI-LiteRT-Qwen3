package com.jarvis.ai.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.JarvisApp
import com.jarvis.ai.brain.AiAnswer
import com.jarvis.ai.core.AssistantEngine
import com.jarvis.ai.core.AssistantState
import com.jarvis.ai.data.db.Fact
import com.jarvis.ai.data.db.MasterProfile
import com.jarvis.ai.data.db.Message
import com.jarvis.ai.permissions.PermissionManager
import com.jarvis.ai.voice.SpeechRecognizerController
import com.jarvis.ai.voice.VoiceLevelNormalizer
import com.jarvis.ai.voice.TtsController
import com.jarvis.ai.voice.OpenWakeWordController
import com.jarvis.ai.voice.VoiceForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

private const val DEFAULT_TITLE = "سيدي"

class JarvisViewModel(private val app: JarvisApp) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val speech = SpeechRecognizerController(app)
    private val tts = TtsController(app, { ready -> _uiState.value = _uiState.value.copy(ttsReady = ready) }, { engine.completeSpeaking() })
    private val engine = AssistantEngine(app.memory, app.searchMemory, app.settings, app.createGateway())
    private var listeningJob: Job? = null

    init {
        viewModelScope.launch {
            val profile = app.memory.getProfile()
            val normalizedProfile = profile?.let {
                if (it.wakePhrase.equals(OpenWakeWordController.SUPPORTED_WAKE_PHRASE, ignoreCase = true)) {
                    it.copy(wakePhrase = OpenWakeWordController.SUPPORTED_WAKE_PHRASE)
                } else {
                    // Existing profiles from the previous free-form UI could contain a phrase
                    // that the bundled ONNX classifier never recognized. Normalize them to the
                    // actual model phrase instead of leaving a permanently misleading setting.
                    it.copy(wakePhrase = OpenWakeWordController.SUPPORTED_WAKE_PHRASE).also { fixed ->
                        app.memory.saveProfile(fixed)
                    }
                }
            }
            _uiState.value = _uiState.value.copy(
                profile = normalizedProfile,
                screen = Screen.Home
            )
        }
        viewModelScope.launch {
            engine.state.collect { state ->
                _uiState.value = _uiState.value.copy(
                    assistantState = state,
                    voiceLevel = if (state is AssistantState.ListeningForCommand) _uiState.value.voiceLevel else 0f
                )
            }
        }
        viewModelScope.launch {
            app.settings.alwaysListeningEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(alwaysListeningEnabled = enabled)
            }
        }
        viewModelScope.launch {
            app.settings.speechRate.collect { rate -> tts.speechRate = rate.coerceIn(0.75f, 1.15f) }
        }
        viewModelScope.launch {
            app.geminiKeyStore.isConfigured.collect { configured ->
                _uiState.value = _uiState.value.copy(geminiApiConfigured = configured)
            }
        }
        viewModelScope.launch {
            app.memory.observeFacts().collect { _uiState.value = _uiState.value.copy(facts = it) }
        }
        viewModelScope.launch {
            app.memory.observeMessages(engine.sessionId).collect { _uiState.value = _uiState.value.copy(messages = it) }
        }
    }

    fun onInputChange(value: String) { _uiState.value = _uiState.value.copy(input = value) }
    fun navigate(screen: Screen) { _uiState.value = _uiState.value.copy(screen = screen) }

    fun saveGeminiApiKey(value: String) {
        viewModelScope.launch {
            val key = value.trim()
            app.geminiKeyStore.setApiKey(key)
            _uiState.value = _uiState.value.copy(
                geminiApiConfigured = key.isNotBlank(),
                geminiStatus = if (key.isNotBlank()) "مفتاح Gemini مفعّل. النموذج: Gemini 2.5 Flash" else "تمت إزالة مفتاح Gemini. سيتم استخدام Backend إذا كان مُعدًا."
            )
        }
    }

    fun clearGeminiApiKey() {
        viewModelScope.launch {
            app.geminiKeyStore.clear()
            _uiState.value = _uiState.value.copy(
                geminiApiConfigured = false,
                geminiStatus = "تمت إزالة مفتاح Gemini. سيتم استخدام Backend إذا كان مُعدًا."
            )
        }
    }

    fun submitText() {
        val text = _uiState.value.input.trim()
        if (text.isBlank()) return
        _uiState.value = _uiState.value.copy(input = "")
        launchHandleText(text)
    }

    fun startListening() {
        if (!PermissionManager.microphoneGranted(app)) {
            _uiState.value = _uiState.value.copy(micPermissionNeeded = true)
            return
        }
        if (!speech.isAvailable()) {
            _uiState.value = _uiState.value.copy(voiceError = "التعرف الصوتي غير متاح حاليًا على هذا الجهاز. يمكنك الكتابة أو استخدام زر الاستماع اليدوي.")
            return
        }
        if (processingJob?.isActive == true) return
        listeningJob?.cancel()
        smoothedVoiceLevel = VoiceLevelNormalizer.reset()
        _uiState.value = _uiState.value.copy(assistantState = AssistantState.ListeningForCommand, voiceError = null, voiceLevel = 0f)
        if (_uiState.value.alwaysListeningEnabled) {
            sendWakeWordService(com.jarvis.ai.voice.VoiceForegroundService.ACTION_SUSPEND)
        }
        listeningJob = viewModelScope.launch {
            delay(450)
            speech.start(
                onResult = { transcript ->
                    if (_uiState.value.alwaysListeningEnabled) {
                        sendWakeWordService(com.jarvis.ai.voice.VoiceForegroundService.ACTION_RESUME)
                    }
                    smoothedVoiceLevel = VoiceLevelNormalizer.reset()
                    _uiState.value = _uiState.value.copy(voiceLevel = VoiceLevelNormalizer.reset())
                    launchHandleText(transcript)
                },
                onRmsChanged = { rmsDb ->
                    updateVoiceLevel(rmsDb)
                },
                onError = { message ->
                    if (_uiState.value.alwaysListeningEnabled) {
                        sendWakeWordService(com.jarvis.ai.voice.VoiceForegroundService.ACTION_RESUME)
                    }
                    smoothedVoiceLevel = VoiceLevelNormalizer.reset()
                    _uiState.value = _uiState.value.copy(assistantState = AssistantState.Idle, voiceError = message, voiceLevel = 0f)
                }
            )
        }
    }

    private var smoothedVoiceLevel = 0f

    private fun updateVoiceLevel(rmsDb: Float) {
        val current = VoiceLevelNormalizer.normalize(rmsDb)
        smoothedVoiceLevel = VoiceLevelNormalizer.smooth(smoothedVoiceLevel, current)
        _uiState.value = _uiState.value.copy(voiceLevel = smoothedVoiceLevel)
    }

    private fun sendWakeWordService(action: String) {
        if (!_uiState.value.alwaysListeningEnabled) return
        runCatching {
            app.startService(
                Intent(app, com.jarvis.ai.voice.VoiceForegroundService::class.java).setAction(action)
            )
        }.onFailure {
            _uiState.value = _uiState.value.copy(voiceError = "تعذر التحكم في خدمة الاستماع المستمر. يمكنك إعادة فتح التطبيق والمحاولة.")
        }
    }

    private var pendingAlwaysListeningEnable = false
    private var processingJob: Job? = null

    fun onMicrophonePermissionResult(granted: Boolean) {
        val enableAlwaysListening = pendingAlwaysListeningEnable
        pendingAlwaysListeningEnable = false
        _uiState.value = _uiState.value.copy(
            micPermissionNeeded = false,
            voiceError = if (granted) null else "إذن الميكروفون مرفوض. يمكنك الكتابة بدلًا من ذلك."
        )
        if (granted && enableAlwaysListening) {
            setAlwaysListeningEnabledInternal(true)
        }
    }

    fun setAlwaysListeningEnabled(enabled: Boolean, requestPermission: () -> Unit) {
        if (enabled && !PermissionManager.microphoneGranted(app)) {
            pendingAlwaysListeningEnable = true
            _uiState.value = _uiState.value.copy(micPermissionNeeded = true)
            requestPermission()
            return
        }
        setAlwaysListeningEnabledInternal(enabled)
    }

    private fun setAlwaysListeningEnabledInternal(enabled: Boolean) {
        viewModelScope.launch {
            app.settings.setAlwaysListeningEnabled(enabled)
            runCatching {
                val intent = Intent(app, VoiceForegroundService::class.java).setAction(VoiceForegroundService.ACTION_RESUME)
                if (enabled) {
                    app.startForegroundService(intent)
                } else {
                    app.stopService(intent)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(voiceError = "تعذر تطبيق إعداد الاستماع المستمر.")
            }
        }
    }

    fun reportVoiceError(message: String) {
        _uiState.value = _uiState.value.copy(voiceError = message)
    }

    fun dismissVoiceError() { _uiState.value = _uiState.value.copy(voiceError = null) }

    fun deleteFact(id: Long) { viewModelScope.launch { app.memory.deleteFact(id) } }
    fun clearMemory() { viewModelScope.launch { app.memory.deleteAllMemory() } }

    override fun onCleared() {
        speech.destroy()
        tts.shutdown()
        super.onCleared()
    }

    private fun launchHandleText(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) return
        if (processingJob?.isActive == true) {
            _uiState.value = _uiState.value.copy(voiceError = "ما زلت أعالج الطلب السابق. انتظر اكتماله ثم أرسل الطلب التالي.")
            return
        }
        processingJob = viewModelScope.launch {
            try {
                val reply = engine.process(normalized)
                val refreshedProfile = if (reply.stored) app.memory.getProfile() else _uiState.value.profile
                _uiState.value = _uiState.value.copy(
                    lastAnswer = reply.answer,
                    profile = refreshedProfile
                )
                if (reply.answer.text.isNotBlank()) speak(com.jarvis.ai.brain.ResponseComposer.voiceSummary(reply.answer))
            } finally {
                processingJob = null
            }
        }
    }

    private fun speak(text: String) {
        _uiState.value = _uiState.value.copy(assistantState = AssistantState.Speaking)
        tts.speak(text)
    }

    companion object {
        fun factory(app: JarvisApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = JarvisViewModel(app) as T
        }
    }
}

enum class Screen { Home, Conversation, Memory, Profile, Settings, Connection, Permissions, About }

data class UiState(
    val profile: MasterProfile? = null,
    val screen: Screen = Screen.Home,
    val input: String = "",
    val assistantState: AssistantState = AssistantState.Idle,
    val voiceLevel: Float = 0f,
    val messages: List<Message> = emptyList(),
    val facts: List<Fact> = emptyList(),
    val lastAnswer: AiAnswer? = null,
    val ttsReady: Boolean = false,
    val micPermissionNeeded: Boolean = false,
    val voiceError: String? = null,
    val alwaysListeningEnabled: Boolean = false,
    val geminiApiConfigured: Boolean = false,
    val geminiStatus: String? = null
)
