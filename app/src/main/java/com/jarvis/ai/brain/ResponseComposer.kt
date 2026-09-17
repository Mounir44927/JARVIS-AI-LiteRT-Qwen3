package com.jarvis.ai.brain

object ResponseComposer {
    fun voiceSummary(answer: AiAnswer): String {
        val text = answer.text.trim()
        return when {
            text.isNotBlank() -> text
            answer.sources.isNotEmpty() -> "I have your answer and sources on screen, sir."
            else -> "I have your answer on screen, sir."
        }
    }
}
