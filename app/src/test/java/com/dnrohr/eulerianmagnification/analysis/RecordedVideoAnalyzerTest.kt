package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

class RecordedVideoAnalyzerTest {
    @Test
    fun extractsAverageGreenFromFixedRoi() {
        val analyzer = RecordedVideoAnalyzer(
            settings = AnalysisSettings(),
            roi = NormalizedRect(0.25f, 0.25f, 0.75f, 0.75f),
        )

        val sample = analyzer.analyze(
            syntheticFrame(
                timestampNanos = 0L,
                roi = NormalizedRect(0.25f, 0.25f, 0.75f, 0.75f),
                roiGreen = 160,
                backgroundGreen = 80,
            ),
        )

        assertEquals(160.0, sample.averageGreen, 0.0)
        assertEquals(0.25f, sample.roi?.left ?: 0.0f, 0.0f)
    }

    @Test
    fun pulseBandRecordedClipProducesMoreEnergyThanSlowDrift() {
        val pulseAnalyzer = RecordedVideoAnalyzer(AnalysisSettings(mode = MagnificationMode.Pulse))
        val driftAnalyzer = RecordedVideoAnalyzer(AnalysisSettings(mode = MagnificationMode.Pulse))
        var pulseEnergy = 0.0
        var driftEnergy = 0.0

        for (frameIndex in 0 until 300) {
            val seconds = frameIndex / FPS
            val timestampNanos = (seconds * NANOS_PER_SECOND).toLong()
            val pulseGreen = 128 + (8.0 * sin(2.0 * PI * 1.2 * seconds)).roundToInt()
            val driftGreen = 128 + (8.0 * sin(2.0 * PI * 0.2 * seconds)).roundToInt()

            pulseEnergy += abs(
                pulseAnalyzer.analyze(syntheticFrame(timestampNanos, RecordedVideoAnalyzer.DEFAULT_ROI, pulseGreen)).bandpassedGreen,
            )
            driftEnergy += abs(
                driftAnalyzer.analyze(syntheticFrame(timestampNanos, RecordedVideoAnalyzer.DEFAULT_ROI, driftGreen)).bandpassedGreen,
            )
        }

        assertTrue(pulseEnergy > driftEnergy * 1.8)
    }

    @Test
    fun detectsNonMonotonicRecordedFrameTimestamps() {
        val analyzer = RecordedVideoAnalyzer(AnalysisSettings())

        analyzer.analyze(syntheticFrame(timestampNanos = 2L, roi = RecordedVideoAnalyzer.DEFAULT_ROI))
        val sample = analyzer.analyze(syntheticFrame(timestampNanos = 1L, roi = RecordedVideoAnalyzer.DEFAULT_ROI))

        assertFalse(sample.timestampMonotonic)
    }

    private fun syntheticFrame(
        timestampNanos: Long,
        roi: NormalizedRect,
        roiGreen: Int = 128,
        backgroundGreen: Int = 64,
    ): RgbFrame {
        val pixels = IntArray(WIDTH * HEIGHT) { rgb(backgroundGreen) }
        val left = (roi.left * WIDTH).toInt()
        val top = (roi.top * HEIGHT).toInt()
        val right = (roi.right * WIDTH).toInt()
        val bottom = (roi.bottom * HEIGHT).toInt()
        for (y in top until bottom) {
            for (x in left until right) {
                pixels[y * WIDTH + x] = rgb(roiGreen)
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
