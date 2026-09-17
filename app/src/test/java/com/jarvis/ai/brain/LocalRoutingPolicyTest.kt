package com.jarvis.ai.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalRoutingPolicyTest {
    @Test
    fun currentRequestsAlwaysGoCloud() {
        assertTrue(CactusRoutingPolicy.mustUseCloud("What is the latest Android version?"))
        assertTrue(CactusRoutingPolicy.mustUseCloud("ابحث عن أخبار الذكاء الاصطناعي"))
    }

    @Test
    fun simpleStableRequestsCanBeLocalCandidates() {
        assertFalse(CactusRoutingPolicy.mustUseCloud("What is 2 + 2?"))
        assertFalse(CactusRoutingPolicy.mustUseCloud("Say hello"))
    }

    @Test
    fun parserRequiresExplicitRouteAndConfidence() {
        val parsed = LocalRouteParser.parse(
            """
            ROUTE=LOCAL
            CONFIDENCE=0.91
            ANSWER=4
            """.trimIndent()
        )
        assertEquals(LocalRoute.LOCAL, parsed?.route)
        assertEquals(0.91f, parsed?.confidence)
        assertEquals("4", parsed?.answer)
    }

    @Test
    fun parserStripsQwenThinkingBlock() {
        val parsed = LocalRouteParser.parse(
            """
            <think>private reasoning</think>
            ROUTE=CLOUD
            CONFIDENCE=0.95
            ANSWER=Use Gemini for this.
            """.trimIndent()
        )
        assertEquals(LocalRoute.CLOUD, parsed?.route)
        assertEquals("Use Gemini for this.", parsed?.answer)
    }
}
