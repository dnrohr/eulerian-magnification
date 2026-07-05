package com.dnrohr.eulerianmagnification.gl

import org.junit.Assert.assertEquals
import org.junit.Test

class GlFullscreenQuadTest {
    @Test
    fun externalTextureQuadLeavesOrientationToSurfaceTextureMatrix() {
        val quad = GlFullscreenQuad.EXTERNAL_TEXTURE

        assertEquals(0.0f, quad.bottomLeftV(), 0.0f)
        assertEquals(1.0f, quad.topLeftV(), 0.0f)
    }

    @Test
    fun framebufferTextureQuadUsesOpenGlTextureOrientation() {
        val quad = GlFullscreenQuad.FRAMEBUFFER_TEXTURE

        assertEquals(0.0f, quad.bottomLeftV(), 0.0f)
        assertEquals(1.0f, quad.topLeftV(), 0.0f)
    }

    private fun FloatArray.bottomLeftV(): Float = this[3]

    private fun FloatArray.topLeftV(): Float = this[11]
}
