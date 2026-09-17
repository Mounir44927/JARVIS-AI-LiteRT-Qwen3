package com.jarvis.ai.core

object ArabicTextNormalizer {
    private val tashkeel = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670]")
    private val tatweel = "ـ"

    fun normalize(input: String): String = input
        .trim()
        .replace(tatweel, "")
        .replace(tashkeel, "")
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace(Regex("\\s+"), " ")
        .lowercase()

    fun similarity(a: String, b: String): Double {
        val x = normalize(a)
        val y = normalize(b)
        if (x == y) return 1.0
        if (x.isBlank() || y.isBlank()) return 0.0
        val max = maxOf(x.length, y.length)
        return 1.0 - levenshtein(x, y).toDouble() / max
    }

    private fun levenshtein(a: String, b: String): Int {
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in a.indices) {
            curr[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                curr[j + 1] = minOf(curr[j] + 1, prev[j + 1] + 1, prev[j] + cost)
            }
            for (j in prev.indices) prev[j] = curr[j]
        }
        return prev[b.length]
    }

    fun spokenText(input: String, maxChars: Int = 450): String {
        val normalized = input
            .replace(Regex("https?://\\S+"), " رابط ")
            .replace(Regex("\\b\\d{4,}\\b")) { it.value.toLongOrNull()?.let(::spellNumber) ?: it.value }
            .replace(Regex("[\\t\\r\\n]+"), " ")
            .replace(Regex(" {2,}"), " ")
            .trim()
        return if (normalized.length <= maxChars) normalized else normalized.take(maxChars).substringBeforeLast(' ', missingDelimiterValue = normalized.take(maxChars)) + "…"
    }

    private fun spellNumber(value: Long): String = when (value) {
        in 0..10 -> value.toString()
        else -> value.toString()
    }
}
