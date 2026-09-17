package com.jarvis.ai.brain

import org.junit.Assert.assertEquals
import org.junit.Test

class DirectGeminiGatewayTest {
    @Test
    fun usesGemini36Flash() {
        assertEquals("gemini-2.5-flash", DirectGeminiGateway.MODEL_ID)
    }
}
