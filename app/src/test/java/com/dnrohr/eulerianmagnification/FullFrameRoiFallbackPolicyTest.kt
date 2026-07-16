package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.RoiSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullFrameRoiFallbackPolicyTest {
    @Test
    fun fallsBackAfterRepeatedLowFpsFullFrameSamples() {
        var state = FullFrameRoiFallbackState()
        repeat(4) {
            val decision = FullFrameRoiFallbackPolicy.observe(
                roiSource = RoiSource.FullFrame,
                analysisFps = 12.0,
                state = state,
            )
            assertFalse(decision.shouldFallbackToAuto)
            state = decision.nextState
        }

        val decision = FullFrameRoiFallbackPolicy.observe(
            roiSource = RoiSource.FullFrame,
            analysisFps = 12.0,
            state = state,
        )

        assertTrue(decision.shouldFallbackToAuto)
    }

    @Test
    fun doesNotFallbackForStartupUnknownFpsOrAutoRoi() {
        val startup = FullFrameRoiFallbackPolicy.observe(
            roiSource = RoiSource.FullFrame,
            analysisFps = 0.0,
            state = FullFrameRoiFallbackState(lowFpsSampleCount = 4),
        )
        val auto = FullFrameRoiFallbackPolicy.observe(
            roiSource = RoiSource.Auto,
            analysisFps = 12.0,
            state = FullFrameRoiFallbackState(lowFpsSampleCount = 4),
        )

        assertFalse(startup.shouldFallbackToAuto)
        assertFalse(auto.shouldFallbackToAuto)
    }

    @Test
    fun healthyFpsResetsLowFpsCount() {
        val decision = FullFrameRoiFallbackPolicy.observe(
            roiSource = RoiSource.FullFrame,
            analysisFps = 30.0,
            state = FullFrameRoiFallbackState(lowFpsSampleCount = 4),
        )

        assertFalse(decision.shouldFallbackToAuto)
        assertEquals(0, decision.nextState.lowFpsSampleCount)
    }

    @Test
    fun criticalThermalFallsBackImmediately() {
        val decision = FullFrameRoiFallbackPolicy.observe(
            roiSource = RoiSource.FullFrame,
            analysisFps = 30.0,
            state = FullFrameRoiFallbackState(),
            thermalStatus = "critical",
        )

        assertTrue(decision.shouldFallbackToAuto)
        assertEquals("thermal", decision.reason)
    }

    @Test
    fun severeThermalFallsBackImmediately() {
        val decision = FullFrameRoiFallbackPolicy.observe(
            roiSource = RoiSource.FullFrame,
            analysisFps = 30.0,
            state = FullFrameRoiFallbackState(),
            thermalStatus = "severe",
        )

        assertTrue(decision.shouldFallbackToAuto)
        assertEquals("thermal", decision.reason)
    }

    @Test
    fun settledLowGlCameraFpsFallsBackImmediately() {
        val decision = FullFrameRoiFallbackPolicy.observe(
            roiSource = RoiSource.FullFrame,
            analysisFps = 30.0,
            state = FullFrameRoiFallbackState(),
            cameraFrameFps = 12.0,
            cameraFrameSampleCount = 10,
        )

        assertTrue(decision.shouldFallbackToAuto)
        assertEquals("camera_fps", decision.reason)
    }

    @Test
    fun lowGlCameraFpsDoesNotFallbackWhileStatsSettle() {
        val decision = FullFrameRoiFallbackPolicy.observe(
            roiSource = RoiSource.FullFrame,
            analysisFps = 30.0,
            state = FullFrameRoiFallbackState(),
            cameraFrameFps = 12.0,
            cameraFrameSampleCount = 9,
        )

        assertFalse(decision.shouldFallbackToAuto)
    }

    @Test
    fun moderateThermalDoesNotForceFallback() {
        val decision = FullFrameRoiFallbackPolicy.observe(
            roiSource = RoiSource.FullFrame,
            analysisFps = 30.0,
            state = FullFrameRoiFallbackState(),
            thermalStatus = "moderate",
        )

        assertFalse(decision.shouldFallbackToAuto)
    }
}
