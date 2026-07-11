package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ChrominanceAmplificationComparisonTest {
    @Test
    fun syntheticComparisonReportsAllColorStrategies() {
        val metrics = ChrominanceAmplificationComparison.compareSyntheticSkinPulse()

        assertEquals(ColorAmplificationStrategy.entries.toSet(), metrics.map { it.strategy }.toSet())
        metrics.forEach { metric ->
            assertTrue(metric.targetChromaResponse > 0.0)
            assertTrue(metric.backgroundPumping >= 0.0)
        }
    }

    @Test
    fun chrominanceStrategyImprovesTargetResponseToBackgroundPumpingRatio() {
        val metrics = ChrominanceAmplificationComparison.compareSyntheticSkinPulse()
        val rgb = metrics.first { it.strategy == ColorAmplificationStrategy.Rgb }
        val chrominance = metrics.first { it.strategy == ColorAmplificationStrategy.Chrominance }

        assertTrue(chrominance.responseToPumpRatio > rgb.responseToPumpRatio)
        assertTrue(chrominance.backgroundLuminanceShift < rgb.backgroundLuminanceShift)
    }

    @Test
    fun chrominanceStrategyReducesBackgroundLuminanceShiftVersusGreenOnly() {
        val metrics = ChrominanceAmplificationComparison.compareSyntheticSkinPulse()
        val greenOnly = metrics.first { it.strategy == ColorAmplificationStrategy.GreenOnly }
        val chrominance = metrics.first { it.strategy == ColorAmplificationStrategy.Chrominance }

        assertTrue(chrominance.targetChromaResponse > 0.0)
        assertTrue(chrominance.backgroundLuminanceShift < greenOnly.backgroundLuminanceShift)
    }

    @Test
    fun rejectsMismatchedFrameSizes() {
        val base = frame(width = 2, height = 2)
        val current = frame(width = 3, height = 2)

        assertThrows(IllegalArgumentException::class.java) {
            ChrominanceAmplificationComparison.amplify(
                base = base,
                current = current,
                strategy = ColorAmplificationStrategy.Chrominance,
                amplification = 4.0,
            )
        }
    }

    private fun frame(width: Int, height: Int): RgbFrame {
        return RgbFrame(
            width = width,
            height = height,
            timestampNanos = 0L,
            pixels = IntArray(width * height) { rgb(128, 128, 128) },
        )
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red shl 16) or (green shl 8) or blue
    }
}
