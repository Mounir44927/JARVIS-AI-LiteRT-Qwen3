package com.jarvis.ai.brain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRuntimePolicyTest {
    @Test
    fun sixGiBDeviceWithHeadroomKeepsBothModelsResident() {
        val total = 6L * 1024L * 1024L * 1024L
        val available = 2L * 1024L * 1024L * 1024L

        assertTrue(
            ModelRuntimePolicy.shouldKeepHeavyModelsResident(
                totalMemBytes = total,
                availMemBytes = available,
                lowMemory = false
            )
        )
    }

    @Test
    fun sixGiBDeviceUnderMemoryPressureReleasesWhisperCompanion() {
        val total = 8L * 1024L * 1024L * 1024L
        val available = 900L * 1024L * 1024L

        assertFalse(
            ModelRuntimePolicy.shouldKeepHeavyModelsResident(
                totalMemBytes = total,
                availMemBytes = available,
                lowMemory = false
            )
        )
    }

    @Test
    fun lowMemorySignalAlwaysWins() {
        val total = 12L * 1024L * 1024L * 1024L
        val available = 8L * 1024L * 1024L * 1024L

        assertFalse(
            ModelRuntimePolicy.shouldKeepHeavyModelsResident(
                totalMemBytes = total,
                availMemBytes = available,
                lowMemory = true
            )
        )
    }
}
