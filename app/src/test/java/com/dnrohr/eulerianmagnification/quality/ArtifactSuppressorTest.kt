package com.dnrohr.eulerianmagnification.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtifactSuppressorTest {
    @Test
    fun suppressesSignalsBelowNoiseFloor() {
        val result = ArtifactSuppressor(noiseFloor = 0.05).amplify(
            bandpassedSignal = 0.01,
            amplification = 20.0f,
        )

        assertEquals(0.0, result.value, 0.0)
        assertTrue(result.wasNoiseSuppressed)
    }

    @Test
    fun clampsLargeAmplifiedSignals() {
        val result = ArtifactSuppressor(maxAmplifiedMagnitude = 10.0).amplify(
            bandpassedSignal = 2.0,
            amplification = 20.0f,
        )

        assertEquals(10.0, result.value, 0.0)
        assertTrue(result.wasClamped)
        assertEquals(1.0, result.normalizedMagnitude, 0.0)
    }

    @Test
    fun leavesUsableSignalsAlone() {
        val result = ArtifactSuppressor().amplify(
            bandpassedSignal = -0.5,
            amplification = 10.0f,
        )

        assertEquals(-5.0, result.value, 0.0)
        assertFalse(result.wasNoiseSuppressed)
        assertFalse(result.wasClamped)
    }
}
