package com.jarvis.ai.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnglishSpeechFilterTest {
    @Test fun arabic_text_is_removed_from_tts_input() {
        val output = EnglishSpeechFilter.prepare("Hello سيدي, Jarvis is online")
        assertTrue(output.contains("Hello"))
        assertTrue(output.contains("Jarvis"))
        assertFalse(output.any { it in '\u0600'..'\u06FF' })
    }

    @Test fun punctuation_and_ascii_text_are_preserved() {
        val output = EnglishSpeechFilter.prepare("Sir, system status: 100% OK!")
        assertTrue(output.contains("Sir"))
        assertTrue(output.contains("100"))
        assertTrue(output.contains("OK"))
    }
}
