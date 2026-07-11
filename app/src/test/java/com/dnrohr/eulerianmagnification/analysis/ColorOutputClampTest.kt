package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class ColorOutputClampTest {
    @Test
    fun limitsChannelDeltaAndKeepsChangedPixelsAwayFromDisplayRails() {
        val output = ColorOutputClamp(
            ColorOutputClampConfig(
                maxChannelDelta = 48,
                maxChromaDelta = 0.5,
            ),
        ).clampPixel(
            basePixel = rgb(120, 120, 120),
            candidatePixel = rgb(255, 0, 255),
        )

        assertTrue(red(output) <= 168)
        assertTrue(green(output) >= 72)
        assertTrue(blue(output) <= 168)
        assertTrue(red(output) in 4..251)
        assertTrue(green(output) in 4..251)
        assertTrue(blue(output) in 4..251)
    }

    @Test
    fun limitsChromaShiftToAvoidColorInversion() {
        val clamp = ColorOutputClamp(
            ColorOutputClampConfig(
                maxChannelDelta = 255,
                maxChromaDelta = 0.04,
            ),
        )
        val base = rgb(180, 126, 108)

        val output = clamp.clampPixel(
            basePixel = base,
            candidatePixel = rgb(60, 210, 220),
        )

        val baseYiq = YiqColor.fromRgb(base)
        val outputYiq = YiqColor.fromRgb(output)
        val chromaDelta = hypot(
            outputYiq.inPhase - baseYiq.inPhase,
            outputYiq.quadrature - baseYiq.quadrature,
        )
        assertTrue(chromaDelta <= 0.045)
    }

    @Test
    fun dampensLargeFullFrameColorPulse() {
        val base = frame(rgb(96, 128, 96))
        val candidate = frame(rgb(160, 192, 160))

        val output = ColorOutputClamp().clampFrame(base, candidate)

        assertTrue(green(output.pixels.single()) < green(candidate.pixels.single()))
        assertTrue(green(output.pixels.single()) > green(base.pixels.single()))
    }

    @Test
    fun preservesSmallLocalizedChange() {
        val base = RgbFrame(
            width = 2,
            height = 1,
            timestampNanos = 0L,
            pixels = intArrayOf(rgb(96, 128, 96), rgb(96, 128, 96)),
        )
        val candidate = base.copy(
            pixels = intArrayOf(rgb(96, 136, 96), rgb(96, 128, 96)),
        )

        val output = ColorOutputClamp().clampFrame(base, candidate)

        assertEquals(candidate.pixels.toList(), output.pixels.toList())
    }

    private fun frame(pixel: Int): RgbFrame {
        return RgbFrame(
            width = 1,
            height = 1,
            timestampNanos = 0L,
            pixels = intArrayOf(pixel),
        )
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }

    private fun red(pixel: Int): Int = (pixel shr 16) and 0xFF
    private fun green(pixel: Int): Int = (pixel shr 8) and 0xFF
    private fun blue(pixel: Int): Int = pixel and 0xFF
}
