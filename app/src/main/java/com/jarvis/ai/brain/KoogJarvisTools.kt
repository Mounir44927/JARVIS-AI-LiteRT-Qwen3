package com.jarvis.ai.brain

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.jarvis.ai.data.MemoryRepository
import com.jarvis.ai.data.SearchMemoryRepository
import com.jarvis.ai.data.db.Fact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class KoogToolState {
    var usedWebSearch: Boolean = false
        private set
    private val _sources = mutableListOf<SourceRef>()
    val sources: List<SourceRef> get() = _sources.distinctBy { it.url }

    fun recordSearch(result: AiAnswer) {
        usedWebSearch = true
        _sources += result.sources
    }
}

@LLMDescription(
    "JARVIS tools. Use semantic memory for personal context and webSearch for current, changing, " +
        "or externally verifiable information. Never claim a search happened unless webSearch was called."
)
class KoogJarvisTools(
    private val memory: MemoryRepository,
    private val searchMemory: SearchMemoryRepository,
    private val searchGateway: AiGateway,
    private val toolState: KoogToolState
) : ToolSet {

    @Tool
    @LLMDescription(
        "Semantically retrieve the user's most relevant saved memories for the supplied query. " +
            "Use this instead of dumping all memories."
    )
    suspend fun retrieveMemory(
        @LLMDescription("The current user request or concept to match against saved memories") query: String
    ): String = withContext(Dispatchers.IO) {
        val facts = memory.findRelevantFacts(query, limit = 8)
        if (facts.isEmpty()) {
            "No semantically relevant saved memories were found."
        } else {
            facts.joinToString("\n") { "- ${it.category}: ${it.content}" }
        }
    }

    @Tool
    @LLMDescription(
        "Search the live web using Gemini Google Search grounding. Use for current facts, news, prices, weather, " +
            "versions, schedules, public figures, recent events, or whenever the user explicitly asks to search online. " +
            "A semantic search cache is checked first; only call the network search when needed."
    )
    suspend fun webSearch(
        @LLMDescription("A precise web-search query that captures the user's information need") query: String
    ): String = withContext(Dispatchers.IO) {
        val cached = searchMemory.find(query)
        if (cached != null) {
            toolState.recordSearch(
                AiAnswer(
                    text = cached.answer,
                    sources = parseSources(cached.sourcesJson),
                    usedSearch = true
                )
            )
            buildString {
                append("Cached grounded result (semantic cache hit):\n")
                append(cached.answer)
                val sources = parseSources(cached.sourcesJson)
                if (sources.isNotEmpty()) {
                    append("\nSources:\n")
                    sources.forEach { append("- ${it.title}: ${it.url}\n") }
                }
            }
        } else {
            val result = searchGateway.answer(
                systemPrompt = """
                    You are JARVIS's web-search specialist.
                    Search the live web using Google Search grounding.
                    Return a factual, concise synthesis in English.
                    Include only facts supported by the retrieved web sources.
                    Do not invent citations.
                """.trimIndent(),
                context = "",
                userPrompt = query,
                search = true
            ).getOrThrow()

            toolState.recordSearch(result)
            buildString {
                append(result.text)
                if (result.sources.isNotEmpty()) {
                    append("\nSources:\n")
                    result.sources.forEach { append("- ${it.title}: ${it.url}\n") }
                }
            }
        }
    }

    @Tool
    @LLMDescription("Save one durable user preference or fact. Use only when the user explicitly asks JARVIS to remember it.")
    suspend fun remember(
        @LLMDescription("A concise key for the memory, for example preferred_name") key: String,
        @LLMDescription("The value to remember") value: String
    ): String = withContext(Dispatchers.IO) {
        val cleanKey = key.trim().take(120)
        val cleanValue = value.trim().take(1000)
        if (cleanKey.isBlank() || cleanValue.isBlank()) {
            return@withContext "Memory was not saved because the key or value was empty."
        }
        memory.saveFact(
            Fact(
                category = cleanKey,
                content = cleanValue,
                source = "koog-agent",
                confidence = 1f,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        "Saved memory: $cleanKey = $cleanValue"
    }

    @Tool
    @LLMDescription("Return the current local device date and time.")
    fun currentLocalDateTime(): String = DateFormat.getDateTimeInstance(
        DateFormat.FULL,
        DateFormat.MEDIUM,
        Locale.getDefault()
    ).format(Date())

    @Tool
    @LLMDescription("Calculate a basic arithmetic expression containing numbers, +, -, *, /, parentheses, and decimals.")
    fun calculate(
        @LLMDescription("Basic arithmetic expression, such as (12.5 * 4) / 2") expression: String
    ): String {
        return try {
            val normalized = expression.replace(" ", "")
            require(normalized.length <= 120)
            require(normalized.matches(Regex("[0-9+\\-*/().]+")))
            val result = SimpleArithmetic.evaluate(normalized)
            if (!result.isFinite()) "Calculation error: non-finite result." else result.toString()
        } catch (_: Throwable) {
            "Calculation error: unsupported expression."
        }
    }

    private fun parseSources(value: String): List<SourceRef> = value.lineSequence().mapNotNull {
        val parts = it.split('|', limit = 2)
        if (parts.size == 2 && parts[1].isNotBlank()) SourceRef(parts[0], parts[1]) else null
    }.toList()
}

private object SimpleArithmetic {
    fun evaluate(source: String): Double {
        val parser = Parser(source)
        val value = parser.parseExpression()
        require(parser.isAtEnd())
        return value
    }

    private class Parser(private val s: String) {
        private var i = 0
        fun isAtEnd() = i == s.length

        fun parseExpression(): Double {
            var v = parseTerm()
            while (i < s.length) {
                when (s[i]) {
                    '+' -> { i++; v += parseTerm() }
                    '-' -> { i++; v -= parseTerm() }
                    else -> break
                }
            }
            return v
        }

        private fun parseTerm(): Double {
            var v = parseFactor()
            while (i < s.length) {
                when (s[i]) {
                    '*' -> { i++; v *= parseFactor() }
                    '/' -> { i++; v /= parseFactor() }
                    else -> break
                }
            }
            return v
        }

        private fun parseFactor(): Double {
            if (i < s.length && s[i] == '+') { i++; return parseFactor() }
            if (i < s.length && s[i] == '-') { i++; return -parseFactor() }
            if (i < s.length && s[i] == '(') {
                i++
                val v = parseExpression()
                require(i < s.length && s[i] == ')')
                i++
                return v
            }
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            require(start < i)
            return s.substring(start, i).toDouble()
        }
    }
}
