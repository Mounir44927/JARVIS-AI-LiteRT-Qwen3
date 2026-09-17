package com.jarvis.ai.core

/**
 * Extracts an explicitly declared user name without treating ordinary first-person
 * sentences such as "أنا أحب الشاي" as an identity declaration.
 */
object IdentityParser {
    private val arabicPattern = Regex(
        """^\s*(?:تذكر\s+(?:أن|ان)\s+)?(?:أنا\s+)?اسمي\s+(?:هو\s+)?(.+?)\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val englishPattern = Regex(
        """^\s*(?:remember\s+that\s+)?my\s+name\s+is\s+(.+?)\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val commaClauseTail = Regex(
        """[،,]\s+.*$""",
        RegexOption.IGNORE_CASE
    )
    private val englishConjunctionTail = Regex(
        """\s+(?:and|but|because)\s+.*$""",
        RegexOption.IGNORE_CASE
    )
    private val validName = Regex("""^\p{L}+(?:['’\-]\p{L}+)*(?:\s+\p{L}+(?:['’\-]\p{L}+)*){0,3}$""")

    fun parse(input: String): String? {
        val raw = input.trim()
        if (raw.isBlank() || raw.length > 160) return null

        val captured = arabicPattern.matchEntire(raw)?.groupValues?.getOrNull(1)
            ?: englishPattern.matchEntire(raw)?.groupValues?.getOrNull(1)
            ?: return null

        val candidate = englishConjunctionTail
            .replace(commaClauseTail.replace(captured, ""), "")
            .trim()
            .trimEnd('.', '!', '?', '؟', '،', ',', ';', ':', '…')
            .trim('"', '\'', '“', '”', '’', '`')
            .replace(Regex("\\s+"), " ")
            .trim()

        if (candidate.isBlank() || candidate.length > 60) return null
        if (!validName.matches(candidate)) return null
        return candidate
    }
}
