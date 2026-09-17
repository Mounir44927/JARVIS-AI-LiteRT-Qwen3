package com.jarvis.ai.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LearningPolicyTest {
    private val policy = LearningPolicy()

    @Test fun explicit_memory_is_stored_without_approval() {
        val decision = policy.explicitStore("تذكر أنني أحب الشاي")
        assertNotNull(decision)
        assertEquals(false, decision!!.requiresApproval)
        assertEquals("أحب الشاي", decision.fact!!.content)
    }

    @Test fun unrelated_text_is_not_stored() {
        assertNull(policy.explicitStore("ما عاصمة فرنسا؟"))
    }
}
