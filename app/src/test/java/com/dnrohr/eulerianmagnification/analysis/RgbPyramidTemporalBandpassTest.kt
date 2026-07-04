package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

class RgbPyramidTemporalBandpassTest {
    @Test
    fun firstFrameReturnsZeroBandpassAtEachLevel() {
        val pyramid = pyramidFor(green = 128, timestampNanos = 0L)

        val output = RgbPyramidTemporalBandpass(AnalysisSettings()).update(pyramid)

        assertEquals(pyramid.levelCount, output.levels.size)
        output.levels.forEach { level ->
            assertTrue(level.pixels.all { it == 0 })
        }
    }

    @Test
    fun preservesLevelSizesAndTimestamp() {
        val filter = RgbPyramidTemporalBandpass(AnalysisSettings())
        filter.update(pyramidFor(green = 128, timestampNanos = 0L))

        val output = filter.update(pyramidFor(green = 132, timestampNanos = 33_333_333L))

        assertEquals(4, output.levels[0].width)
        assertEquals(4, output.levels[0].height)
        assertEquals(2, output.levels[1].width)
        assertEquals(2, output.levels[1].height)
        assertEquals(33_333_333L, output.levels[1].timestampNanos)
    }

    @Test
    fun pulseBandProducesMoreEnergyForPulseThanDrift() {
        val pulseFilter = RgbPyramidTemporalBandpass(AnalysisSettings(mode = MagnificationMode.Pulse))
        val driftFilter = RgbPyramidTemporalBandpass(AnalysisSettings(mode = MagnificationMode.Pulse))
        var pulseEnergy = 0.0
        var driftEnergy = 0.0

        for (frame in 0 until 300) {
            val seconds = frame / FPS
            val timestampNanos = (seconds * NANOS_PER_SECOND).toLong()
            val pulseGreen = 128 + (8.0 * sin(2.0 * PI * 1.2 * seconds)).roundToInt()
            val driftGreen = 128 + (8.0 * sin(2.0 * PI * 0.2 * seconds)).roundToInt()

            pulseEnergy += pulseFilter.update(pyramidFor(pulseGreen, timestampNanos)).greenEnergy()
            driftEnergy += driftFilter.update(pyramidFor(driftGreen, timestampNanos)).greenEnergy()
        }

        assertTrue(pulseEnergy > driftEnergy * 1.5)
    }

    @Test
    fun resetsStateWhenPyramidShapeChanges() {
        val filter = RgbPyramidTemporalBandpass(AnalysisSettings())
        filter.update(pyramidFor(green = 128, timestampNanos = 0L))
        filter.update(pyramidFor(green = 160, timestampNanos = 33_333_333L))

        val output = filter.update(
            RgbFramePyramidBuilder(levelCount = 2).build(
                frame(width = 2, height = 2, green = 160, timestampNanos = 66_666_666L),
            ),
        )

        assertTrue(output.levels.all { level -> level.pixels.all { it == 0 } })
    }

    private fun pyramidFor(green: Int, timestampNanos: Long): RgbFramePyramid {
        return RgbFramePyramidBuilder(levelCount = 2).build(
            frame(width = 4, height = 4, green = green, timestampNanos = timestampNanos),
        )
    }

    private fun frame(width: Int, height: Int, green: Int, timestampNanos: Long): RgbFrame {
        return RgbFrame(
            width = width,
            height = height,
            timestampNanos = timestampNanos,
            pixels = IntArray(width * height) { rgb(96, green, 96) },
        )
    }

    private fun RgbPyramidBandpass.greenEnergy(): Double {
        return levels.sumOf { level ->
            level.pixels.sumOf { pixel ->
                abs(RgbPyramidTemporalBandpass.signedGreen(pixel)).toDouble()
            }
        }
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red shl 16) or (green shl 8) or blue
    }

    companion object {
        private const val FPS = 30.0
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
