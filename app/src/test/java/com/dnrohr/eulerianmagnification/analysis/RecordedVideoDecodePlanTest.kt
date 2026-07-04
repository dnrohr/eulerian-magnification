package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordedVideoDecodePlanTest {
    @Test
    fun createsTimestampsAtTargetFps() {
        val timestamps = RecordedVideoDecodePlan.timestampsMicros(
            durationMillis = 1_000L,
            targetFps = 5.0,
            maxFrames = 10,
        )

        assertEquals(listOf(0L, 200_000L, 400_000L, 600_000L, 800_000L), timestamps)
    }

    @Test
    fun capsFrameCount() {
        val timestamps = RecordedVideoDecodePlan.timestampsMicros(
            durationMillis = 10_000L,
            targetFps = 30.0,
            maxFrames = 3,
        )

        assertEquals(3, timestamps.size)
        assertEquals(0L, timestamps[0])
        assertTrue(timestamps[1] > timestamps[0])
        assertTrue(timestamps[2] > timestamps[1])
    }

    @Test
    fun returnsEmptyForUnknownDuration() {
        val timestamps = RecordedVideoDecodePlan.timestampsMicros(
            durationMillis = 0L,
            targetFps = 30.0,
            maxFrames = 10,
        )

        assertTrue(timestamps.isEmpty())
    }
}
