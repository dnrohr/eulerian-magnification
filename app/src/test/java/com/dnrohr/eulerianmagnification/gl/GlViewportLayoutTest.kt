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

    @Test
    fun aspectFillCropsWideContentHorizontallyForPortraitSurface() {
        val viewport = GlViewportLayout.aspectFill(
            surfaceSize = GlTextureSize(1080, 2400),
            contentSize = GlTextureSize(640, 480),
        )

        assertEquals(2400, viewport.height)
        assertEquals(3200, viewport.width)
        assertEquals(-1060, viewport.x)
        assertEquals(0, viewport.y)
    }

    @Test
    fun orientContentToSurfaceSwapsLandscapeBufferForPortraitSurface() {
        val oriented = GlViewportLayout.orientContentToSurface(
            surfaceSize = GlTextureSize(1080, 2400),
            contentSize = GlTextureSize(640, 480),
        )

        assertEquals(480, oriented.width)
        assertEquals(640, oriented.height)
    }

    @Test
    fun aspectFillUsesOrientedCameraBufferForPortraitSurface() {
        val surfaceSize = GlTextureSize(1080, 2400)
        val orientedContentSize = GlViewportLayout.orientContentToSurface(
            surfaceSize = surfaceSize,
            contentSize = GlTextureSize(640, 480),
        )
        val viewport = GlViewportLayout.aspectFill(surfaceSize, orientedContentSize)

        assertEquals(2400, viewport.height)
        assertEquals(1800, viewport.width)
        assertEquals(-360, viewport.x)
        assertEquals(0, viewport.y)
    }

    @Test
    fun aspectFillCropsTallContentVerticallyForWideSurface() {
        val viewport = GlViewportLayout.aspectFill(
            surfaceSize = GlTextureSize(1920, 1080),
            contentSize = GlTextureSize(720, 1280),
        )

        assertEquals(1920, viewport.width)
        assertEquals(3413, viewport.height)
        assertEquals(0, viewport.x)
        assertEquals(-1166, viewport.y)
    }
}
