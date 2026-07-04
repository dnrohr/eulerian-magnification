package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class RgbFramePyramidBuilderTest {
    @Test
    fun buildsRequestedPyramidLevelsUntilOnePixel() {
        val pyramid = RgbFramePyramidBuilder(levelCount = 4).build(
            RgbFrame(
                width = 8,
                height = 4,
                timestampNanos = 123L,
                pixels = IntArray(8 * 4) { rgb(10, 20, 30) },
            ),
        )

        assertEquals(4, pyramid.levelCount)
        assertEquals(8, pyramid.levels[0].width)
        assertEquals(4, pyramid.levels[0].height)
        assertEquals(4, pyramid.levels[1].width)
        assertEquals(2, pyramid.levels[1].height)
        assertEquals(2, pyramid.levels[2].width)
        assertEquals(1, pyramid.levels[2].height)
        assertEquals(1, pyramid.levels[3].width)
        assertEquals(1, pyramid.levels[3].height)
        assertEquals(123L, pyramid.levels[3].timestampNanos)
    }

    @Test
    fun averagesTwoByTwoBlocks() {
        val pyramid = RgbFramePyramidBuilder(levelCount = 2).build(
            RgbFrame(
                width = 2,
                height = 2,
                timestampNanos = 0L,
                pixels = intArrayOf(
                    rgb(10, 20, 30),
                    rgb(30, 40, 50),
                    rgb(50, 60, 70),
                    rgb(70, 80, 90),
                ),
            ),
        )

        assertEquals(rgb(40, 50, 60), pyramid.levels[1].pixels.single())
    }

    @Test
    fun handlesOddDimensionsByAveragingAvailablePixels() {
        val pyramid = RgbFramePyramidBuilder(levelCount = 2).build(
            RgbFrame(
                width = 3,
                height = 3,
                timestampNanos = 0L,
                pixels = intArrayOf(
                    rgb(10, 10, 10),
                    rgb(30, 30, 30),
                    rgb(90, 90, 90),
                    rgb(50, 50, 50),
                    rgb(70, 70, 70),
                    rgb(110, 110, 110),
                    rgb(130, 130, 130),
                    rgb(150, 150, 150),
                    rgb(170, 170, 170),
                ),
            ),
        )

        assertEquals(1, pyramid.levels[1].width)
        assertEquals(1, pyramid.levels[1].height)
        assertEquals(rgb(40, 40, 40), pyramid.levels[1].pixels.single())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidLevelCount() {
        RgbFramePyramidBuilder(levelCount = 0)
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red shl 16) or (green shl 8) or blue
    }
}
