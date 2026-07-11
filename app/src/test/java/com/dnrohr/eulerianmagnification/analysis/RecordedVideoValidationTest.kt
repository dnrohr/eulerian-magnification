package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class RecordedVideoValidationTest {
    @Test
    fun validatesDecodedFramesWithRequestedSettings() {
        var requestedOptions: RecordedVideoDecodeOptions? = null
        val validator = RecordedVideoValidator { _, options ->
            requestedOptions = options
            syntheticClip(frequencyHz = 1.2)
        }

        val result = validator.validate(
            file = File("sample-videos/pulse.mp4"),
            settings = AnalysisSettings(mode = MagnificationMode.Pulse),
            decodeOptions = RecordedVideoDecodeOptions(targetFps = 15.0, maxFrames = 120),
        )

        assertEquals("pulse.mp4", result.sourceName)
        assertEquals(MagnificationMode.Pulse, result.settings.mode)
        assertEquals(15.0, requestedOptions?.targetFps ?: 0.0, 0.0)
        assertEquals(120, requestedOptions?.maxFrames ?: 0)
        assertEquals(300, result.report.frameCount)
        assertTrue(result.report.bandpassedEnergy > 0.0)
        assertTrue(result.report.timestampsMonotonic)
    }

    @Test
    fun summaryIncludesModeMetricsAndTimingStatus() {
        val result = RecordedVideoValidationResult(
            sourceName = "clip.mp4",
            settings = AnalysisSettings(mode = MagnificationMode.Breathing),
            report = RecordedVideoAnalysisReport(
                frameCount = 90,
                averageFps = 29.97,
                averageGreen = 128.0,
                bandpassedEnergy = 42.24,
                maxBandpassedMagnitude = 2.75,
                timestampsMonotonic = true,
                rateEstimate = GatedRateEstimate(
                    estimate = null,
                    hiddenReason = RateEstimateHiddenReason.LightingUnstable,
                ),
            ),
        )

        assertEquals(
            "Video processing: clip.mp4 Breathing 0.1-0.6 Hz, 90 frames, 30.0 fps, energy 42.2, peak 2.8, timing OK, rate hidden: lighting is not stable",
            result.summary(),
        )
    }

    @Test
    fun summaryHandlesEmptyDecodes() {
        val result = RecordedVideoValidator { _, _ -> emptyList() }.validate(
            file = File("empty.mp4"),
            settings = AnalysisSettings(),
        )

        assertEquals("Video processing: empty.mp4 produced no frames", result.summary())
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
