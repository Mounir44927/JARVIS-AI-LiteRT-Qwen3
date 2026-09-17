package com.jarvis.ai.voice

/**
 * Final safety gate for the English-only JARVIS TTS model.
 * The AI is instructed to answer in English; this class only removes
 * unsupported control/unicode characters and refuses Arabic text instead
 * of silently deleting words and producing a corrupted sentence.
 */
object EnglishSpeechFilter {
    private val arabic = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]")
    private val control = Regex("[\\u0000-\\u001F\\u007F]")
    private val nonEnglish = Regex("[^A-Za-z0-9.,!?;:'\"()%+*/=\\- ]")
    private val whitespace = Regex("\\s+")

    fun prepare(input: String, maxChars: Int = 450): String {
        val normalized = input
            .replace(Regex("https?://\\S+"), " ")
            .replace(control, " ")
            .replace(Regex("[\\r\\n]+"), " . ")
            .replace(nonEnglish, " ")
            .replace(whitespace, " ")
            .trim()

        // Never feed Arabic text to the English-only neural TTS model.
        // Returning an empty string is safer than deleting Arabic words from
        // an otherwise valid sentence and speaking a corrupted fragment.
        if (arabic.find(normalized) != null) return ""

        return normalized.take(maxChars).trim()
    }

}
