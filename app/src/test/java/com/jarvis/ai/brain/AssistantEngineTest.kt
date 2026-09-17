package com.jarvis.ai.brain

import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantEngineTest {
    @Test fun http_error_is_categorized() {
        val ex = GeminiHttpException(429, "quota")
        assertTrue(ex.code == 429)
    }
}
