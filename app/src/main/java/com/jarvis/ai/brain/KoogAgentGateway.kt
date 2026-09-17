package com.jarvis.ai.brain

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import com.jarvis.ai.data.GeminiApiKeyStore
import com.jarvis.ai.data.MemoryRepository
import com.jarvis.ai.data.SearchMemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real JARVIS agent layer backed by JetBrains Koog.
 *
 * The agent owns the tool-selection loop. Live web search is exposed as a
 * first-class tool rather than being preselected by AssistantEngine.
 */
class KoogAgentGateway(
    private val keyStore: GeminiApiKeyStore,
    private val memory: MemoryRepository,
    private val searchMemory: SearchMemoryRepository,
    private val fallback: AiGateway
) : AiGateway {

    override suspend fun answer(
        systemPrompt: String,
        context: String,
        userPrompt: String,
        search: Boolean
    ): Result<AiAnswer> {
        val apiKey = keyStore.getApiKey()?.trim().orEmpty()
        if (apiKey.isBlank()) return fallback.answer(systemPrompt, context, userPrompt, search)

        return runCatching {
            withContext(Dispatchers.IO) {
                val toolState = KoogToolState()
                val prompt = buildString {
                    append(systemPrompt)
                    append("\n\n")
                    if (search) {
                        append("\nThe user explicitly requested web search. You MUST call webSearch before answering.\n")
                    }
                    append(
                        """
                        You are the JARVIS reasoning agent.
                        You choose tools yourself.
                        IMPORTANT:
                        - For current/changing/external facts, news, prices, weather, versions, schedules,
                          public figures, recent events, or explicit online-search requests, call webSearch.
                        - For personal context, call retrieveMemory with the user's current request.
                        - Do not answer a current-information request from model knowledge alone when webSearch is available.
                        - You may call multiple tools and refine the task before your final answer.
                        - Never claim a tool action occurred unless that tool returned successfully.
                        - Final response must be natural English suitable for voice playback.
                        """.trimIndent()
                    )
                    if (context.isNotBlank()) {
                        append("\n\nRecent conversation context:\n")
                        append(context)
                    }
                }

                val agent = AIAgent(
                    promptExecutor = simpleGoogleAIExecutor(apiKey),
                    llmModel = GoogleModels.Gemini2_5Flash,
                    systemPrompt = prompt,
                    temperature = 0.2,
                    toolRegistry = ToolRegistry {
                        tools(
                            KoogJarvisTools(
                                memory = memory,
                                searchMemory = searchMemory,
                                searchGateway = fallback,
                                toolState = toolState
                            )
                        )
                    },
                    maxIterations = 12
                )

                val text = agent.run(userPrompt).trim()
                require(text.isNotBlank()) { "Koog returned an empty response" }

                AiAnswer(
                    text = text,
                    sources = toolState.sources,
                    usedSearch = toolState.usedWebSearch
                )
            }
        }.recoverCatching { error ->
            // If an agent run fails, preserve the old tested gateway as a safe
            // compatibility path. The fallback still honors the caller's
            // explicit search request.
            fallback.answer(systemPrompt, context, userPrompt, search).getOrThrow()
        }
    }
}
