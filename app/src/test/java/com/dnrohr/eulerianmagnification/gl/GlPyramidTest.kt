package com.dnrohr.eulerianmagnification.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GlPyramidTest {
    @Test
    fun computesHalvedPyramidSizes() {
        val sizes = GlPyramid.pyramidSizes(
            baseSize = GlTextureSize(640, 360),
            levelCount = 4,
        )

        assertEquals(
            listOf(
                GlTextureSize(640, 360),
                GlTextureSize(320, 180),
                GlTextureSize(160, 90),
                GlTextureSize(80, 45),
            ),
            sizes,
        )
    }

    @Test
    fun clampsTinyPyramidSizesToOnePixel() {
        val sizes = GlPyramid.pyramidSizes(
            baseSize = GlTextureSize(3, 2),
            levelCount = 4,
        )

        assertEquals(
            listOf(
                GlTextureSize(3, 2),
                GlTextureSize(1, 1),
                GlTextureSize(1, 1),
                GlTextureSize(1, 1),
            ),
            sizes,
        )
    }

    @Test
    fun rejectsNonPositiveLevelCount() {
        assertThrows(IllegalArgumentException::class.java) {
            GlPyramid.pyramidSizes(GlTextureSize(640, 360), 0)
        }
    }
}
