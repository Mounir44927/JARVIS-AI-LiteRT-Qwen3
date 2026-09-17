package com.jarvis.ai.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicTextNormalizerTest {
    @Test fun normalize_removes_tashkeel_and_tatweel() {
        assertEquals("السلام عليكم", ArabicTextNormalizer.normalize("اَلسـلامُ عَلَيْكُم"))
    }
    @Test fun similarity_exact_is_one() {
        assertEquals(1.0, ArabicTextNormalizer.similarity("JARVIS", "JARVIS"), 0.0001)
    }
    @Test fun spoken_text_removes_urls() {
        assertTrue(!ArabicTextNormalizer.spokenText("اقرأ https://example.com الآن").contains("http"))
    }
}
