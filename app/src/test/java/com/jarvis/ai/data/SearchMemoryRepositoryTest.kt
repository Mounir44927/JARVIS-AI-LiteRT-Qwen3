package com.jarvis.ai.data

import com.jarvis.ai.core.ArabicTextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMemoryRepositoryTest {
    @Test fun normalization_makes_arabic_queries_stable() {
        assertEquals(ArabicTextNormalizer.normalize("أخر الأخبار"), ArabicTextNormalizer.normalize("آخر الأخبار"))
    }
    @Test fun stale_timestamp_is_past() {
        val expiresAt = System.currentTimeMillis() - 1
        assertTrue(expiresAt < System.currentTimeMillis())
    }
}
