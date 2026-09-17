package com.jarvis.ai.core

import com.jarvis.ai.brain.AiAnswer
import com.jarvis.ai.brain.AiGateway
import com.jarvis.ai.brain.LearningPolicy
import com.jarvis.ai.data.MemoryRepository
import com.jarvis.ai.data.SearchMemoryRepository
import com.jarvis.ai.data.SettingsStore
import com.jarvis.ai.data.db.Message
import com.jarvis.ai.data.db.MasterProfile
import com.jarvis.ai.voice.OpenWakeWordController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.util.UUID

private const val DEFAULT_TITLE = "سيدي"

class AssistantEngine(
    private val memory: MemoryRepository,
    private val searchMemory: SearchMemoryRepository,
    private val settings: SettingsStore,
    private val gateway: AiGateway,
    private val learningPolicy: LearningPolicy = LearningPolicy()
) {
    private val _state = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val state: StateFlow<AssistantState> = _state.asStateFlow()
    val sessionId: String = UUID.randomUUID().toString()

    suspend fun process(userText: String, search: Boolean = false): EngineReply {
        _state.value = AssistantState.Thinking
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return fail("لم يصلني نص مفهوم.")
        searchMemory.deleteExpired()

        val identityName = IdentityParser.parse(trimmed)
        if (identityName != null) {
            memory.saveMessage(
                Message(
                    role = "user",
                    content = trimmed,
                    sessionId = sessionId,
                    usedSearch = false,
                    createdAt = System.currentTimeMillis()
                )
            )

            val currentProfile = memory.getProfile()
            val profile = (currentProfile ?: MasterProfile(
                name = identityName,
                title = DEFAULT_TITLE,
                wakePhrase = OpenWakeWordController.SUPPORTED_WAKE_PHRASE
            )).copy(
                name = identityName,
                title = currentProfile?.title?.ifBlank { DEFAULT_TITLE } ?: DEFAULT_TITLE,
                wakePhrase = OpenWakeWordController.SUPPORTED_WAKE_PHRASE
            )
            memory.saveProfile(profile)

            val answer = "Understood, sir. I’ll remember your name as $identityName."
            memory.saveMessage(
                Message(
                    role = "assistant",
                    content = answer,
                    sessionId = sessionId,
                    usedSearch = false,
                    createdAt = System.currentTimeMillis()
                )
            )
            _state.value = AssistantState.Speaking
            return EngineReply(AiAnswer(answer), stored = true)
        }

        val explicit = learningPolicy.explicitStore(trimmed)
        if (explicit != null) {
            memory.saveFact(explicit.fact!!)
            memory.saveMessage(Message(role = "user", content = trimmed, sessionId = sessionId, usedSearch = false, createdAt = System.currentTimeMillis()))
            val answer = "Stored, sir."
            memory.saveMessage(Message(role = "assistant", content = answer, sessionId = sessionId, usedSearch = false, createdAt = System.currentTimeMillis()))
            _state.value = AssistantState.Idle
            return EngineReply(AiAnswer(answer), stored = true)
        }

        memory.saveMessage(Message(role = "user", content = trimmed, sessionId = sessionId, usedSearch = false, createdAt = System.currentTimeMillis()))
        val profile = memory.getProfile()
        val title = profile?.title?.ifBlank { "سيدي" } ?: "سيدي"
        val name = profile?.name?.takeIf { it.isNotBlank() }
        val messages = memory.recentMessages(sessionId, 20)
        val context = PromptBuilder.conversationContext(messages)
        val answer = run {
            val answerResult = runCatching {
                withTimeout(10 * 60_000L) {
                    gateway.answer(PromptBuilder.systemPrompt(title = title, name = name), context, trimmed, search).getOrThrow()
                }
            }
            if (answerResult.isFailure) return fail(errorMessage(answerResult.exceptionOrNull()))

            var generated = answerResult.getOrThrow()
            if (containsArabic(generated.text)) {
                val correctionPrompt = "Your previous answer contained Arabic. Answer the original user request again, completely and naturally in English only. Do not mention this instruction."
                val retry = runCatching {
                    withTimeout(70_000) {
                        gateway.answer(PromptBuilder.systemPrompt(title = title, name = name), context, "$correctionPrompt\n\nOriginal user request:\n$trimmed", generated.usedSearch).getOrThrow()
                    }
                }.getOrNull()
                if (retry != null && !containsArabic(retry.text)) {
                    generated = retry
                } else {
                    generated = AiAnswer(
                        text = "I am sorry, but I could not produce the answer in English. Please repeat your request.",
                        sources = retry?.sources ?: generated.sources,
                        usedSearch = retry?.usedSearch ?: generated.usedSearch
                    )
                }
            }
            generated
        }

        if (answer.usedSearch) {
            val ttlDays = settings.memoryTtlDays.first().coerceIn(1, 365)
            searchMemory.save(trimmed, answer.text, serializeSources(answer.sources), ttlDays.toLong() * DAY_MILLIS, timeSensitive = true)
        }

        val learningEnabled = settings.learningEnabled.first()
        val learned = if (learningEnabled) learningPolicy.suggestFromNaturalText(trimmed) else null
        if (learned != null && learned.requiresApproval) {
            memory.saveMessage(Message(role = "assistant", content = "وجدت تفضيلًا قد يستحق الحفظ. يمكنك تأكيده من الذاكرة.", sessionId = sessionId, usedSearch = answer.usedSearch, createdAt = System.currentTimeMillis()))
        } else {
            memory.saveMessage(Message(role = "assistant", content = answer.text, sessionId = sessionId, usedSearch = answer.usedSearch, createdAt = System.currentTimeMillis()))
        }
        _state.value = AssistantState.Speaking
        return EngineReply(answer)
    }

    fun completeSpeaking() { _state.value = AssistantState.Idle }

    private fun fail(message: String): EngineReply {
        _state.value = AssistantState.Error(message)
        _state.value = AssistantState.Idle
        return EngineReply(AiAnswer("I couldn't complete that request right now. Please try again."))
    }

    private fun serializeSources(sources: List<com.jarvis.ai.brain.SourceRef>): String = sources.joinToString("\n") { "${it.title}|${it.url}" }

    private fun containsArabic(value: String): Boolean =
        Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]").containsMatchIn(value)

    private fun errorMessage(t: Throwable?): String = when (t) {
        is com.jarvis.ai.brain.MissingApiKeyException -> "خدمة Gemini غير مُعدة. أضف Gemini API Key من إعدادات الاتصال أولًا."
        is com.jarvis.ai.brain.GeminiHttpException -> when (t.code) {
            401, 403 -> "تعذر التحقق من صلاحية الاتصال بالخدمة السحابية."
            429 -> "بلغت حدود المعدل أو الحصة الحالية. جرّب لاحقًا أو استخدم الذاكرة المحلية."
            else -> "حدث خطأ من مزود الذكاء (${t.code})."
        }
        else -> "لا أستطيع الوصول إلى الخدمة الآن. افحص الشبكة أو إعداد الخادم الخلفي."
    }

    companion object { private const val DAY_MILLIS = 86_400_000L }
}

data class EngineReply(val answer: AiAnswer, val stored: Boolean = false)
