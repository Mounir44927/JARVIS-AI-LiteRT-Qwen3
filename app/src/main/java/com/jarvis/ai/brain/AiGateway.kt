package com.jarvis.ai.brain

data class SourceRef(val title: String, val url: String)

data class AiAnswer(
    val text: String,
    val sources: List<SourceRef> = emptyList(),
    val usedSearch: Boolean = false
)

interface AiGateway {
    suspend fun answer(
        systemPrompt: String,
        context: String,
        userPrompt: String,
        search: Boolean
    ): Result<AiAnswer>
}
