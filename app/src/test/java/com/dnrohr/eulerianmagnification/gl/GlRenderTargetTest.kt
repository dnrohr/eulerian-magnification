package com.dnrohr.eulerianmagnification.gl

import android.opengl.GLES30
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GlRenderTargetTest {
    @Test
    fun acceptsPositiveTextureSize() {
        val size = GlTextureSize(640, 360)

        assertEquals(640, size.width)
        assertEquals(360, size.height)
    }

    @Test
    fun rejectsInvalidTextureSize() {
        assertThrows(IllegalArgumentException::class.java) {
            GlTextureSize(0, 360)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GlTextureSize(640, -1)
        }
    }

    @Test
    fun rgba16fFormatUsesSignedFloatStorage() {
        assertEquals(GLES30.GL_RGBA16F, GlRenderTargetFormat.Rgba16f.internalFormat)
        assertEquals(GLES30.GL_HALF_FLOAT, GlRenderTargetFormat.Rgba16f.type)
        assertEquals(GLES30.GL_NEAREST, GlRenderTargetFormat.Rgba16f.filter)
    }
}
