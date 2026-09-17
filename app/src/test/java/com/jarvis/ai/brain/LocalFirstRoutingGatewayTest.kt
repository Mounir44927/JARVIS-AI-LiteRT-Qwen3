package com.jarvis.ai.brain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFirstRoutingGatewayTest {
    @Test
    fun acceptedLocalResultDoesNotCallCloud() = runTest {
        val local = FakeGateway(Result.success(AiAnswer("local")))
        val cloud = FakeGateway(Result.success(AiAnswer("cloud")))
        val gateway = LocalFirstRoutingGateway(local, cloud)

        val result = gateway.answer("system", "context", "hello", search = false).getOrThrow()

        assertEquals("local", result.text)
        assertEquals(1, local.calls)
        assertEquals(0, cloud.calls)
    }

    @Test
    fun localFailureFallsBackToKoog() = runTest {
        val local = FakeGateway(Result.failure(LocalNeedsCloudException()))
        val cloud = FakeGateway(Result.success(AiAnswer("cloud")))
        val gateway = LocalFirstRoutingGateway(local, cloud)

        val result = gateway.answer("system", "context", "hello", search = false).getOrThrow()

        assertEquals("cloud", result.text)
        assertEquals(1, local.calls)
        assertEquals(1, cloud.calls)
    }

    @Test
    fun forcedCloudPolicySkipsLocal() = runTest {
        val local = FakeGateway(Result.success(AiAnswer("local")))
        val cloud = FakeGateway(Result.success(AiAnswer("cloud")))
        val gateway = LocalFirstRoutingGateway(local, cloud)

        val result = gateway.answer("system", "context", "What is the latest Android version?", search = false).getOrThrow()

        assertEquals("cloud", result.text)
        assertEquals(0, local.calls)
        assertEquals(1, cloud.calls)
    }

    @Test
    fun explicitSearchSkipsLocal() = runTest {
        val local = FakeGateway(Result.success(AiAnswer("local")))
        val cloud = FakeGateway(Result.success(AiAnswer("cloud", usedSearch = true)))
        val gateway = LocalFirstRoutingGateway(local, cloud)

        val result = gateway.answer("system", "context", "find this", search = true).getOrThrow()

        assertTrue(result.usedSearch)
        assertEquals(0, local.calls)
        assertEquals(1, cloud.calls)
    }

    private class FakeGateway(private val result: Result<AiAnswer>) : AiGateway {
        var calls: Int = 0
            private set

        override suspend fun answer(
            systemPrompt: String,
            context: String,
            userPrompt: String,
            search: Boolean
        ): Result<AiAnswer> {
            calls++
            return result
        }
    }
}
