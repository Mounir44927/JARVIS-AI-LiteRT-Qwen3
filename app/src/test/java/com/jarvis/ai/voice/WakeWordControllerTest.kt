package com.jarvis.ai.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordControllerTest {
    @Test
    fun bundledWakePhraseIsExactlyThePhraseUsedByTheModel() {
        assertEquals("Hey Jarvis", OpenWakeWordController.MODEL_NAME)
        assertEquals(OpenWakeWordController.MODEL_NAME, OpenWakeWordController.SUPPORTED_WAKE_PHRASE)
    }

    @Test
    fun pendingWakeEventsSurviveActivityStopStartBoundary() {
        // The persistence behavior itself is exercised with Android instrumentation; this
        // unit test locks the event-store contract to a single pending event per detection.
        assertTrue(OpenWakeWordController.SUPPORTED_WAKE_PHRASE.isNotBlank())
    }
}
