package com.dnrohr.eulerianmagnification.profiling

import org.junit.Assert.assertEquals
import org.junit.Test

class FpsMeterTest {
    @Test
    fun reportsZeroUntilTwoFramesAreRecorded() {
        val meter = FpsMeter()

        meter.recordFrame(1_000_000_000L)

        assertEquals(0.0, meter.framesPerSecond(), 0.0)
    }

    @Test
    fun reportsFrameRateFromRecentFrames() {
        val meter = FpsMeter(windowSize = 3)

        meter.recordFrame(0L)
        meter.recordFrame(33_333_333L)
        meter.recordFrame(66_666_666L)

        assertEquals(30.0, meter.framesPerSecond(), 0.01)
    }
}
