package com.jarvis.ai.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceLevelNormalizerTest {
    @Test fun normalizes_and_clamps_rms() {
        assertEquals(0f, VoiceLevelNormalizer.normalize(Float.NEGATIVE_INFINITY))
        assertTrue(VoiceLevelNormalizer.normalize(-50f) in 0f..1f)
        assertTrue(VoiceLevelNormalizer.normalize(30f) in 0f..1f)
    }

    @Test fun smoothing_stays_in_range_and_rises_with_louder_input() {
        val low = VoiceLevelNormalizer.smooth(0f, 0.2f)
        val high = VoiceLevelNormalizer.smooth(low, 1f)
        assertTrue(low in 0f..1f)
        assertTrue(high in low..1f)
    }
}
