package com.dnrohr.eulerianmagnification.gl

import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessedGlFrameTest {
    @Test
    fun acceptsProcessedTextureFrameMetadata() {
        val frame = ProcessedGlFrame(
            textureId = 42,
            size = GlTextureSize(640, 480),
            presentationTimestampNanos = 33_333_333L,
            splitMode = false,
        )

        assertEquals(42, frame.textureId)
        assertEquals(33_333_333L, frame.presentationTimestampNanos)
    }

    @Test
    fun acceptsRawTextureForSplitFrameMetadata() {
        val frame = ProcessedGlFrame(
            textureId = 42,
            rawTextureId = 41,
            size = GlTextureSize(640, 480),
            presentationTimestampNanos = 33_333_333L,
            splitMode = true,
        )

        assertEquals(41, frame.rawTextureId)
        assertEquals(42, frame.textureId)
        assertEquals(true, frame.splitMode)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidTextureId() {
        ProcessedGlFrame(
            textureId = 0,
            size = GlTextureSize(640, 480),
            presentationTimestampNanos = 0L,
            splitMode = false,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidRawTextureId() {
        ProcessedGlFrame(
            textureId = 1,
            rawTextureId = 0,
            size = GlTextureSize(640, 480),
            presentationTimestampNanos = 0L,
            splitMode = true,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativePresentationTimestamp() {
        ProcessedGlFrame(
            textureId = 1,
            size = GlTextureSize(640, 480),
            presentationTimestampNanos = -1L,
            splitMode = false,
        )
    }
}
