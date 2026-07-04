package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimestampTrackerTest {
    @Test
    fun firstTimestampIsAccepted() {
        val tracker = TimestampTracker()

        assertTrue(tracker.record(100L).isMonotonic)
    }

    @Test
    fun increasingTimestampIsMonotonic() {
        val tracker = TimestampTracker()

        tracker.record(100L)

        assertTrue(tracker.record(200L).isMonotonic)
    }

    @Test
    fun repeatedOrDecreasingTimestampIsNotMonotonic() {
        val tracker = TimestampTracker()

        tracker.record(200L)

        assertFalse(tracker.record(200L).isMonotonic)
        assertFalse(tracker.record(100L).isMonotonic)
    }
}
