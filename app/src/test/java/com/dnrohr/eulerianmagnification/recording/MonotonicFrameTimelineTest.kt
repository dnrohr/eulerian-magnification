package com.dnrohr.eulerianmagnification.recording

import org.junit.Assert.assertEquals
import org.junit.Test

class MonotonicFrameTimelineTest {
    @Test
    fun startsAtZero() {
        val timeline = MonotonicFrameTimeline()

        assertEquals(0L, timeline.next(10_000L))
    }

    @Test
    fun preservesElapsedTimeForIncreasingSourceTimestamps() {
        val timeline = MonotonicFrameTimeline()

        timeline.next(10_000_000L)

        assertEquals(50_000_000L, timeline.next(60_000_000L))
        assertEquals(100_000_000L, timeline.next(110_000_000L))
    }

    @Test
    fun advancesByMinimumIntervalForRepeatedOrDecreasingSourceTimestamps() {
        val timeline = MonotonicFrameTimeline(minFrameIntervalNanos = 33L)

        assertEquals(0L, timeline.next(100L))
        assertEquals(33L, timeline.next(100L))
        assertEquals(66L, timeline.next(90L))
    }

    @Test
    fun clampsNegativeSourceElapsedTimeAfterFirstFrame() {
        val timeline = MonotonicFrameTimeline(minFrameIntervalNanos = 20L)

        timeline.next(1_000L)

        assertEquals(20L, timeline.next(900L))
    }
}
