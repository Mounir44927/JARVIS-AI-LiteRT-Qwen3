package com.jarvis.ai.brain

/**
 * Exact high-level routing order for JARVIS:
 *
 * search=true or a policy-forced cloud request -> Koog/Gemini.
 * Otherwise local LiteRT-LM screens first; only a confident LOCAL decision is accepted.
 * Every other local outcome falls through to Koog.
 */
class LocalFirstRoutingGateway(
    private val local: AiGateway,
    private val cloud: AiGateway
) : AiGateway {
    override suspend fun answer(
        systemPrompt: String,
        context: String,
        userPrompt: String,
        search: Boolean
    ): Result<AiAnswer> {
        if (search || CactusRoutingPolicy.mustUseCloud(userPrompt)) {
            return cloud.answer(systemPrompt, context, userPrompt, search)
        }

        val localResult = runCatching {
            local.answer(systemPrompt, context, userPrompt, false).getOrThrow()
        }
        if (localResult.isSuccess) return localResult

        return cloud.answer(systemPrompt, context, userPrompt, search)
    }
}
