package com.dnrohr.eulerianmagnification.quality

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LightingFlickerDetectorTest {
    @Test
    fun detectsAlternatingBrightness() {
        val detector = LightingFlickerDetector()
        val values = listOf(100.0, 108.0, 99.0, 109.0, 98.0, 108.0, 99.0, 109.0, 98.0)

        val detected = values.map(detector::update).last()

        assertTrue(detected)
    }

    @Test
    fun ignoresSmoothBrightnessDrift() {
        val detector = LightingFlickerDetector()
        val values = listOf(100.0, 101.0, 102.0, 103.0, 104.0, 105.0, 106.0, 107.0, 108.0)

        val detected = values.map(detector::update).last()

        assertFalse(detected)
    }

    @Test
    fun ignoresTinyAlternationsBelowThreshold() {
        val detector = LightingFlickerDetector(deltaThreshold = 2.5)
        val values = listOf(100.0, 101.0, 100.0, 101.0, 100.0, 101.0, 100.0, 101.0, 100.0)

        val detected = values.map(detector::update).last()

        assertFalse(detected)
    }
}
