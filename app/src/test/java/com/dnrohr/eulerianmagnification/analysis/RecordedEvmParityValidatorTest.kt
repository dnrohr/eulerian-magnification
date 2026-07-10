package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class RecordedEvmParityValidatorTest {
    @Test
    fun stationarySamplePassesStabilityExpectation() {
        val report = RecordedEvmParityValidator().validate(
            sampleName = "stationary flat field",
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 20.0f,
            ),
            frames = List(90) { frame(timestampNanos = it * FRAME_NANOS, green = 128) },
            expectation = RecordedEvmParityExpectation(
                maxMeanAbsDelta = 0.0,
                maxClippedPixelFraction = 0.0,
            ),
        )

        assertTrue(report.summary(), report.passed)
        assertEquals(0, report.changedFrameCount)
    }

    @Test
    fun colorPulseSamplePassesRecordedParityExpectation() {
        val report = RecordedEvmParityValidator().validate(
            sampleName = "synthetic pulse color",
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 4.0f,
            ),
            frames = colorPulseClip(frequencyHz = 1.2),
            expectation = RecordedEvmParityExpectation(
                minMeanAbsDelta = 1.0,
                minChangedPixelFraction = 0.5,
                maxClippedPixelFraction = 0.0,
            ),
        )

        assertTrue(report.summary(), report.passed)
        assertTrue(report.changedFrameCount > FRAME_COUNT / 2)
    }

    @Test
    fun translatingEdgeSamplePassesMotionExpectation() {
        val report = RecordedEvmParityValidator().validate(
            sampleName = "synthetic translating edge",
            settings = AnalysisSettings(
                mode = MagnificationMode.Tremor,
                amplification = 0.5f,
            ),
            frames = translatingEdgeClip(frequencyHz = 6.0),
            expectation = RecordedEvmParityExpectation(
                minMeanAbsDelta = 0.2,
                minChangedPixelFraction = 0.02,
                maxClippedPixelFraction = 0.001,
            ),
        )

        assertTrue(report.summary(), report.passed)
    }

    @Test
    fun reportsFailedExpectationWithReason() {
        val report = RecordedEvmParityValidator().validate(
            sampleName = "stationary flat field",
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 20.0f,
            ),
            frames = List(90) { frame(timestampNanos = it * FRAME_NANOS, green = 128) },
            expectation = RecordedEvmParityExpectation(
                minMeanAbsDelta = 1.0,
            ),
        )

        assertFalse(report.passed)
        assertTrue(report.failureReasons.single().contains("meanAbsDelta"))
    }

    private fun colorPulseClip(frequencyHz: Double): List<RgbFrame> {
        return (0 until FRAME_COUNT).map { frameIndex ->
            val seconds = frameIndex / FPS
            val green = 128 + (8.0 * sin(2.0 * PI * frequencyHz * seconds)).roundToInt()
            frame(timestampNanos = (seconds * NANOS_PER_SECOND).toLong(), green = green)
        }
    }

    private fun translatingEdgeClip(frequencyHz: Double): List<RgbFrame> {
        return (0 until FRAME_COUNT).map { frameIndex ->
            val seconds = frameIndex / FPS
            val edgeOffset = (2.0 * sin(2.0 * PI * frequencyHz * seconds)).roundToInt()
            val edgeX = WIDTH / 2 + edgeOffset
            RgbFrame(
                width = WIDTH,
                height = HEIGHT,
                timestampNanos = (seconds * NANOS_PER_SECOND).toLong(),
                pixels = IntArray(WIDTH * HEIGHT) { index ->
                    val x = index % WIDTH
                    if (x < edgeX) {
                        rgb(48, 48, 48)
                    } else {
                        rgb(208, 208, 208)
                    }
                },
            )
        }
    }

    private fun frame(timestampNanos: Long, green: Int): RgbFrame {
        return RgbFrame(
            width = WIDTH,
            height = HEIGHT,
            timestampNanos = timestampNanos,
            pixels = IntArray(WIDTH * HEIGHT) { rgb(96, green, 96) },
        )
    }

    private fun rgb(
        red: Int,
        green: Int,
        blue: Int,
    ): Int {
        return (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }

    companion object {
        private const val WIDTH = 16
        private const val HEIGHT = 12
        private const val FPS = 30.0
        private const val FRAME_COUNT = 300
        private const val FRAME_NANOS = 33_333_333L
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
