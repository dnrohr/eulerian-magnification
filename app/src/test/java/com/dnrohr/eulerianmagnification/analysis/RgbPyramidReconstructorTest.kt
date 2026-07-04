package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RgbPyramidReconstructorTest {
    @Test
    fun zeroAmplificationReturnsBaseCopy() {
        val base = frame(width = 2, height = 2, green = 100)
        val bandpass = RgbPyramidBandpass(
            levels = listOf(
                deltaFrame(width = 2, height = 2, green = 8),
            ),
        )

        val output = RgbPyramidReconstructor().reconstruct(
            base = base,
            bandpass = bandpass,
            amplification = 0.0f,
        )

        assertEquals(base.width, output.width)
        assertEquals(base.height, output.height)
        assertEquals(base.pixels.toList(), output.pixels.toList())
        assertTrue(base.pixels !== output.pixels)
    }

    @Test
    fun addsAmplifiedBandpassDeltaToBaseFrame() {
        val base = frame(width = 2, height = 2, green = 100)
        val bandpass = RgbPyramidBandpass(
            levels = listOf(
                deltaFrame(width = 2, height = 2, green = 3),
            ),
        )

        val output = RgbPyramidReconstructor().reconstruct(
            base = base,
            bandpass = bandpass,
            amplification = 4.0f,
        )

        assertEquals(rgb(96, 112, 96), output.pixels[0])
        assertEquals(base.timestampNanos, output.timestampNanos)
    }

    @Test
    fun upsamplesCoarserLevelAcrossBasePixels() {
        val base = frame(width = 4, height = 4, green = 100)
        val coarse = RgbFrame(
            width = 2,
            height = 2,
            timestampNanos = base.timestampNanos,
            pixels = intArrayOf(
                rgb(0, 1, 0),
                rgb(0, 2, 0),
                rgb(0, 3, 0),
                rgb(0, 4, 0),
            ),
        )

        val output = RgbPyramidReconstructor().reconstruct(
            base = base,
            bandpass = RgbPyramidBandpass(listOf(coarse)),
            amplification = 10.0f,
        )

        assertEquals(rgb(96, 110, 96), output.pixels[0])
        assertEquals(rgb(96, 120, 96), output.pixels[2])
        assertEquals(rgb(96, 130, 96), output.pixels[8])
        assertEquals(rgb(96, 140, 96), output.pixels[10])
    }

    @Test
    fun clampsAmplifiedOutputToDisplayRange() {
        val base = frame(width = 1, height = 1, green = 250)
        val bandpass = RgbPyramidBandpass(
            levels = listOf(deltaFrame(width = 1, height = 1, red = 10, green = 10, blue = 10)),
        )

        val output = RgbPyramidReconstructor().reconstruct(
            base = base,
            bandpass = bandpass,
            amplification = 4.0f,
        )

        assertEquals(rgb(136, 255, 136), output.pixels.single())
    }

    @Test
    fun canSkipFineLevels() {
        val base = frame(width = 1, height = 1, green = 100)
        val fine = deltaFrame(width = 1, height = 1, green = 10)
        val coarse = deltaFrame(width = 1, height = 1, green = 2)

        val output = RgbPyramidReconstructor().reconstruct(
            base = base,
            bandpass = RgbPyramidBandpass(listOf(fine, coarse)),
            amplification = 5.0f,
            startLevel = 1,
        )

        assertEquals(rgb(96, 110, 96), output.pixels.single())
    }

    private fun frame(width: Int, height: Int, green: Int): RgbFrame {
        return RgbFrame(
            width = width,
            height = height,
            timestampNanos = 123L,
            pixels = IntArray(width * height) { rgb(96, green, 96) },
        )
    }

    private fun deltaFrame(
        width: Int,
        height: Int,
        red: Int = 0,
        green: Int,
        blue: Int = 0,
    ): RgbFrame {
        return RgbFrame(
            width = width,
            height = height,
            timestampNanos = 123L,
            pixels = IntArray(width * height) {
                RgbPyramidTemporalBandpass.rgb(red, green, blue)
            },
        )
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }
}
