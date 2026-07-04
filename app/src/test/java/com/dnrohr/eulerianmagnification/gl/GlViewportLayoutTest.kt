package com.dnrohr.eulerianmagnification.gl

import org.junit.Assert.assertEquals
import org.junit.Test

class GlViewportLayoutTest {
    @Test
    fun fullViewportMatchesTextureSize() {
        val viewport = GlViewportLayout.full(GlTextureSize(1280, 720))

        assertEquals(0, viewport.x)
        assertEquals(0, viewport.y)
        assertEquals(1280, viewport.width)
        assertEquals(720, viewport.height)
    }

    @Test
    fun horizontalSplitPreservesOddWidth() {
        val (left, right) = GlViewportLayout.splitHorizontal(GlTextureSize(641, 480))

        assertEquals(0, left.x)
        assertEquals(320, left.width)
        assertEquals(320, right.x)
        assertEquals(321, right.width)
        assertEquals(480, left.height)
        assertEquals(480, right.height)
    }

    @Test
    fun horizontalSplitKeepsTinySurfaceDrawable() {
        val (left, right) = GlViewportLayout.splitHorizontal(GlTextureSize(1, 1))

        assertEquals(1, left.width)
        assertEquals(1, right.width)
        assertEquals(1, left.height)
        assertEquals(1, right.height)
    }
}
