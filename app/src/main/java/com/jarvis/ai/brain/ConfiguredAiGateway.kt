package com.jarvis.ai.brain

import com.jarvis.ai.BuildConfig
import com.jarvis.ai.data.GeminiApiKeyStore
import kotlinx.coroutines.flow.first

/**
 * Cloud-side compatibility gateway.
 *
 * Local inference is intentionally not performed here anymore. This gateway is the cloud fallback
 * used by Koog for missing-key recovery, tool-backed web search, and direct Gemini/backend calls.
 */
class ConfiguredAiGateway(
    private val keyStore: GeminiApiKeyStore,
    private val direct: AiGateway = DirectGeminiGateway(keyStore),
    private val backend: AiGateway = BackendGeminiGateway()
) : AiGateway {

    override suspend fun answer(
        systemPrompt: String,
        context: String,
        userPrompt: String,
        search: Boolean
    ): Result<AiAnswer> {
        if (keyStore.isConfigured.first()) {
            return direct.answer(systemPrompt, context, userPrompt, search)
        }
        if (BackendEndpointValidator.isValidBaseUrl(BuildConfig.BACKEND_BASE_URL)) {
            return backend.answer(systemPrompt, context, userPrompt, search)
        }
        return Result.failure(MissingApiKeyException())
    }
}
