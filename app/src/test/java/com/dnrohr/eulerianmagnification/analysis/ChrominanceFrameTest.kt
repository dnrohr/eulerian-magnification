package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ChrominanceFrameTest {
    @Test
    fun convertsRgbFrameToChrominanceAndBack() {
        val frame = RgbFrame(
            width = 2,
            height = 2,
            timestampNanos = 123L,
            pixels = intArrayOf(
                rgb(96, 128, 160),
                rgb(180, 96, 112),
                rgb(64, 200, 120),
                rgb(240, 220, 180),
            ),
        )

        val roundTrip = ChrominanceFrame.from(frame).toRgbFrame()

        assertEquals(frame.width, roundTrip.width)
        assertEquals(frame.height, roundTrip.height)
        assertEquals(frame.timestampNanos, roundTrip.timestampNanos)
        frame.pixels.zip(roundTrip.pixels).forEach { (expected, actual) ->
            assertChannelClose(expected, actual)
        }
    }

    @Test
    fun zeroChrominanceProducesGrayLuminanceImage() {
        val chroma = ChrominanceFrame.from(
            RgbFrame(
                width = 1,
                height = 1,
                timestampNanos = 0L,
                pixels = intArrayOf(rgb(96, 160, 224)),
            ),
        )

        val gray = chroma.copy(
            inPhase = doubleArrayOf(0.0),
            quadrature = doubleArrayOf(0.0),
        ).toRgbFrame().pixels.single()

        assertWithin(red(gray), green(gray), tolerance = 1)
        assertWithin(green(gray), blue(gray), tolerance = 1)
    }

    @Test
    fun changingChrominancePreservesLuminanceBetterThanChangingRgbChannels() {
        val yiq = YiqColor.fromRgb(rgb(128, 112, 96))
        val shifted = YiqColor.toRgb(
            luminance = yiq.luminance,
            inPhase = yiq.inPhase + 0.04,
            quadrature = yiq.quadrature - 0.02,
        )
        val shiftedYiq = YiqColor.fromRgb(shifted)

        assertTrue(abs(yiq.luminance - shiftedYiq.luminance) < 0.01)
        assertTrue(abs(yiq.inPhase - shiftedYiq.inPhase) > 0.02)
    }

    @Test
    fun rejectsPlaneSizesThatDoNotMatchFrameSize() {
        assertThrows(IllegalArgumentException::class.java) {
            ChrominanceFrame(
                width = 2,
                height = 2,
                timestampNanos = 0L,
                luminance = DoubleArray(4),
                inPhase = DoubleArray(3),
                quadrature = DoubleArray(4),
            )
        }
    }

    private fun assertChannelClose(expected: Int, actual: Int) {
        assertWithin(red(expected), red(actual), tolerance = 1)
        assertWithin(green(expected), green(actual), tolerance = 1)
        assertWithin(blue(expected), blue(actual), tolerance = 1)
    }

    private fun assertWithin(expected: Int, actual: Int, tolerance: Int) {
        assertTrue(abs(expected - actual) <= tolerance)
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red shl 16) or (green shl 8) or blue
    }

    private fun red(pixel: Int): Int = (pixel shr 16) and 0xFF
    private fun green(pixel: Int): Int = (pixel shr 8) and 0xFF
    private fun blue(pixel: Int): Int = pixel and 0xFF
}
