package com.dnrohr.eulerianmagnification.gl

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
}
