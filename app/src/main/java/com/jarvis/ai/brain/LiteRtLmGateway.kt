package com.jarvis.ai.brain

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Local-first JARVIS inference gateway backed by Google's LiteRT-LM Kotlin API.
 *
 * The gateway is deliberately conservative:
 * - search and policy-forced cloud requests never enter local generation;
 * - the local model must emit ROUTE=LOCAL and confidence >= 0.82;
 * - GPU is attempted first, but CPU is a mandatory fallback;
 * - NPU is intentionally not assumed or initialized.
 */
class LiteRtLmGateway(
    context: Context,
    private val modelStore: LiteRtLmModelStore = LiteRtLmModelStore(context.applicationContext),
    private val runtimeCoordinator: ModelRuntimeCoordinator = ModelRuntimeCoordinator(context.applicationContext)
) : AiGateway, AutoCloseable {

    private val mutex = Mutex()
    private var engine: Engine? = null
    private var usingGpu = false

    override suspend fun answer(
        systemPrompt: String,
        context: String,
        userPrompt: String,
        search: Boolean
    ): Result<AiAnswer> {
        if (search || CactusRoutingPolicy.mustUseCloud(userPrompt)) {
            return Result.failure(LocalNeedsCloudException())
        }
        if (userPrompt.length > CactusRoutingPolicy.MAX_LOCAL_CHARS) {
            return Result.failure(LocalNeedsCloudException())
        }

        return runCatching {
            runtimeCoordinator.withLiteRt {
                mutex.withLock {
                    val firstAttempt = runLocal(
                        systemPrompt = systemPrompt,
                        context = context,
                        userPrompt = userPrompt
                    )
                    if (firstAttempt.route == LocalRoute.LOCAL &&
                        firstAttempt.confidence >= CactusRoutingPolicy.MIN_CONFIDENCE &&
                        firstAttempt.answer.isNotBlank()
                    ) {
                        return@withLock AiAnswer(firstAttempt.answer)
                    }
                    throw LocalNeedsCloudException()
                }
            }
        }
    }

    private suspend fun runLocal(
        systemPrompt: String,
        context: String,
        userPrompt: String
    ): LocalParsedDecision = withContext(Dispatchers.Default) {
        try {
            generateWithCurrentEngine(systemPrompt, context, userPrompt)
        } catch (gpuFailure: Throwable) {
            if (!usingGpu) throw gpuFailure
            // GPU delegate failure after initialization: tear it down and retry on CPU once.
            switchToCpu()
            generateWithCurrentEngine(systemPrompt, context, userPrompt)
        }
    }

    private suspend fun generateWithCurrentEngine(
        systemPrompt: String,
        context: String,
        userPrompt: String
    ): LocalParsedDecision {
        val modelPath = modelStore.getModelFile().absolutePath
        val activeEngine = ensureEngine(modelPath)
        val prompt = buildLocalPrompt(systemPrompt, context, userPrompt)
        activeEngine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(prompt),
                samplerConfig = SamplerConfig(topK = 20, topP = 0.95, temperature = 0.0),
                maxOutputToken = 220,
                thinkingConfig = ThinkingConfig(enableThinking = false)
            )
        ).use { conversation ->
            val message = conversation.sendMessage(userPrompt)
            val raw = message.toString().trim()
            return LocalRouteParser.parse(raw) ?: throw LocalNeedsCloudException()
        }
    }

    private suspend fun ensureEngine(modelPath: String): Engine {
        engine?.let { if (it.isInitialized()) return it }

        // GPU is opportunistic only. If delegate initialization fails, CPU becomes the live engine.
        val gpu = Engine(
            EngineConfig(
                modelPath = modelPath,
                backend = Backend.GPU(),
                maxNumTokens = 4096,
                cacheDir = cacheDir(modelPath)
            )
        )
        try {
            gpu.initialize()
            engine = gpu
            usingGpu = true
            return gpu
        } catch (_: Throwable) {
            closeQuietly(gpu)
        }

        val cpu = Engine(
            EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
                maxNumTokens = 4096,
                cacheDir = cacheDir(modelPath)
            )
        )
        cpu.initialize()
        engine = cpu
        usingGpu = false
        return cpu
    }

    private suspend fun switchToCpu() {
        closeQuietly(engine)
        engine = null
        usingGpu = false
    }

    private fun cacheDir(modelPath: String): String =
        File(modelPath).parentFile!!.resolve("cache").apply { mkdirs() }.absolutePath

    /**
     * Called by ModelRuntimeCoordinator only on memory-constrained devices before Whisper.
     * We intentionally do not unload on healthy devices.
     */
    fun releaseForMemoryPressure() {
        runCatching { closeQuietly(engine) }
        engine = null
        usingGpu = false
    }

    /**
     * Re-warmp/reloads the local model after a constrained-device Whisper session.
     * The coordinator measures this call so benchmark data can compare reload cost against
     * the end-to-end local answer latency.
     */
    suspend fun warmUpAfterPressure() {
        val modelPath = modelStore.getModelFile().absolutePath
        ensureEngine(modelPath)
    }

    fun runtimeMetrics(): ModelRuntimeCoordinator.ModelRuntimeMetrics =
        runtimeCoordinator.metrics()

    override fun close() {
        runCatching { closeQuietly(engine) }
        engine = null
        usingGpu = false
    }

    private fun closeQuietly(value: Engine?) {
        if (value?.isInitialized() == true) {
            runCatching { value.close() }
        }
    }

    private fun buildLocalPrompt(systemPrompt: String, context: String, userPrompt: String): String =
        buildString {
            append(
                """
                You are JARVIS's local mobile router and answerer.

                Your job is to decide whether the user's request is safe, short, stable,
                and reliable enough to answer locally. Do not guess.

                Output EXACTLY these three fields, one per line:
                ROUTE=LOCAL or ROUTE=CLOUD
                CONFIDENCE=0.00 to 1.00
                ANSWER=one concise final answer

                Choose CLOUD for current or changing facts, web/search requests, news, prices,
                weather, schedules, versions, public figures, recent events, legal/medical/
                financial advice, coding/debugging, research, multi-step reasoning, long text,
                comparisons requiring external facts, or anything uncertain.
                Choose LOCAL only for short, stable, low-risk requests answerable from common
                knowledge or the supplied conversation context.
                Never invent current facts. Never execute or claim Android actions.
                Final ANSWER must be natural English and concise.
                """.trimIndent()
            )
            if (systemPrompt.isNotBlank()) {
                append("\n\nCore assistant rules:\n")
                append(systemPrompt.take(3500))
            }
            if (context.isNotBlank()) {
                append("\n\nRecent conversation context:\n")
                append(context.take(2200))
            }
            append("\n\nUser request:\n")
            append(userPrompt)
        }

    companion object {
        const val MODEL_REPO = "litert-community/Qwen3-1.7B"
        const val MODEL_FILE = LiteRtLmModelStore.MODEL_FILE_NAME
    }
}

enum class LocalRoute { LOCAL, CLOUD }

data class LocalParsedDecision(
    val route: LocalRoute,
    val confidence: Float,
    val answer: String
)

object LocalRouteParser {
    fun parse(raw: String): LocalParsedDecision? {
        val cleaned = raw.replace(Regex("(?is)<think>.*?</think>"), "").trim()
        val route = Regex("""(?im)^\s*ROUTE\s*=\s*(LOCAL|CLOUD)\s*$""")
            .find(cleaned)?.groupValues?.getOrNull(1)?.uppercase() ?: return null
        val confidence = Regex("""(?im)^\s*CONFIDENCE\s*=\s*(0(?:\.\d+)?|1(?:\.0+)?)\s*$""")
            .find(cleaned)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: return null
        val answer = Regex("""(?ims)^\s*ANSWER\s*=\s*(.+?)\s*$""")
            .find(cleaned)?.groupValues?.getOrNull(1)?.trim().orEmpty()

        return LocalParsedDecision(
            route = if (route == "LOCAL") LocalRoute.LOCAL else LocalRoute.CLOUD,
            confidence = confidence.coerceIn(0f, 1f),
            answer = answer
        )
    }
}

class LocalNeedsCloudException : Exception("Local model routed the request to the cloud")
