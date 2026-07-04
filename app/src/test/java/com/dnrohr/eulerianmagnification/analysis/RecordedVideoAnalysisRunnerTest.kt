package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class RecordedVideoAnalysisRunnerTest {
    @Test
    fun emptyFrameSequenceReturnsEmptyReport() {
        val report = RecordedVideoAnalysisRunner(AnalysisSettings()).analyze(emptyList())

        assertFalse(report.hasFrames)
        assertEquals(0, report.frameCount)
        assertEquals(0.0, report.averageFps, 0.0)
        assertEquals(0.0, report.bandpassedEnergy, 0.0)
        assertTrue(report.timestampsMonotonic)
    }

    @Test
    fun reportsPulseEnergyHigherThanSlowDrift() {
        val pulseReport = RecordedVideoAnalysisRunner(
            settings = AnalysisSettings(mode = MagnificationMode.Pulse),
        ).analyze(syntheticClip(frequencyHz = 1.2))
        val driftReport = RecordedVideoAnalysisRunner(
            settings = AnalysisSettings(mode = MagnificationMode.Pulse),
        ).analyze(syntheticClip(frequencyHz = 0.2))

        assertEquals(300, pulseReport.frameCount)
        assertTrue(pulseReport.averageFps > 29.0)
        assertTrue(pulseReport.bandpassedEnergy > driftReport.bandpassedEnergy * 1.8)
        assertTrue(pulseReport.maxBandpassedMagnitude > driftReport.maxBandpassedMagnitude)
    }

    @Test
    fun reportsTimestampProblemsAcrossClip() {
        val frames = listOf(
            syntheticFrame(timestampNanos = 2L, green = 128),
            syntheticFrame(timestampNanos = 1L, green = 129),
        )

        val report = RecordedVideoAnalysisRunner(AnalysisSettings()).analyze(frames)

        assertFalse(report.timestampsMonotonic)
    }

    private fun syntheticClip(frequencyHz: Double): List<RgbFrame> {
        return (0 until 300).map { frameIndex ->
            val seconds = frameIndex / FPS
            val timestampNanos = (seconds * NANOS_PER_SECOND).toLong()
            val green = 128 + (8.0 * sin(2.0 * PI * frequencyHz * seconds)).roundToInt()
            syntheticFrame(timestampNanos, green)
        }
    }

    private fun syntheticFrame(timestampNanos: Long, green: Int): RgbFrame {
        val pixels = IntArray(WIDTH * HEIGHT) { rgb(64) }
        val roi = RecordedVideoAnalyzer.DEFAULT_ROI
        val left = (roi.left * WIDTH).toInt()
        val top = (roi.top * HEIGHT).toInt()
        val right = (roi.right * WIDTH).toInt()
        val bottom = (roi.bottom * HEIGHT).toInt()
        for (y in top until bottom) {
            for (x in left until right) {
                pixels[y * WIDTH + x] = rgb(green)
            }
        }
        return RgbFrame(
            width = WIDTH,
            height = HEIGHT,
            timestampNanos = timestampNanos,
            pixels = pixels,
        )
    }

    private fun rgb(green: Int): Int {
        val clamped = green.coerceIn(0, 255)
        return (96 shl 16) or (clamped shl 8) or 96
    }

    companion object {
        private const val WIDTH = 64
        private const val HEIGHT = 48
        private const val FPS = 30.0
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
