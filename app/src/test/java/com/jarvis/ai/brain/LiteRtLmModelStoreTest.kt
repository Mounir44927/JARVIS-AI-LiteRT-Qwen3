package com.jarvis.ai.brain

import org.junit.Assert.assertEquals
import org.junit.Test

class LiteRtLmModelStoreTest {
    @Test
    fun pinsOfficialModelArtifact() {
        assertEquals(
            "Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm",
            LiteRtLmModelStore.MODEL_FILE_NAME
        )
        assertEquals(977_184_032L, LiteRtLmModelStore.MODEL_SIZE_BYTES)
        assertEquals(
            "2eeffef7b51bc3e1225ea69fe7aa5f417397934b56a5b6c20cc068d6fd2c918b",
            LiteRtLmModelStore.MODEL_SHA256
        )
    }
}
