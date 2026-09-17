package com.jarvis.ai.brain

/**
 * Conservative gate for deciding whether a request is even eligible for the local model.
 *
 * The policy name is kept for compatibility with the existing routing tests and callers.
 */
object CactusRoutingPolicy {
    const val MAX_LOCAL_CHARS = 700
    const val MIN_CONFIDENCE = 0.82f

    private val cloudPatterns = listOf(
        "\\b(latest|current|today|tonight|tomorrow|news|price|weather|schedule|version|release)\\b",
        "\\b(search|google|look up|browse|web)\\b",
        "\\b(analy[sz]e|analysis|compare|research|debug|debugging|implement|code|program|architecture)\\b",
        "\\b(legal|law|medical|diagnos|financial|investment|contract)\\b",
        "(آخر|اليوم|حالي|الآن|أخبار|سعر|طقس|ابحث|بحث|قارن|حلل|برمج|قانون|طبي|مالي)",
        "(https?://|www\\.)"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    fun mustUseCloud(input: String): Boolean {
        val text = input.trim()
        if (text.length > MAX_LOCAL_CHARS) return true
        return cloudPatterns.any { it.containsMatchIn(text) }
    }
}
