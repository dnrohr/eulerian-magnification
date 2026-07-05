package com.dnrohr.eulerianmagnification.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlEncoderSurfaceRendererTest {
    @Test
    fun configRequestsRecordableGles3RgbaSurface() {
        val attributes = GlEncoderSurfaceConfig.configAttributes().toList()

        assertAttribute(attributes, GlEncoderSurfaceConfig.EGL_RECORDABLE_ANDROID, 1)
        assertAttribute(
            attributes,
            android.opengl.EGL14.EGL_RENDERABLE_TYPE,
            GlEncoderSurfaceConfig.EGL_OPENGL_ES3_BIT_KHR,
        )
        assertAttribute(attributes, android.opengl.EGL14.EGL_RED_SIZE, 8)
        assertAttribute(attributes, android.opengl.EGL14.EGL_GREEN_SIZE, 8)
        assertAttribute(attributes, android.opengl.EGL14.EGL_BLUE_SIZE, 8)
        assertAttribute(attributes, android.opengl.EGL14.EGL_ALPHA_SIZE, 8)
        assertEquals(android.opengl.EGL14.EGL_NONE, attributes.last())
    }

    @Test
    fun contextRequestsGles3() {
        val attributes = GlEncoderSurfaceConfig.contextAttributes().toList()

        assertAttribute(attributes, android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION, 3)
        assertEquals(android.opengl.EGL14.EGL_NONE, attributes.last())
    }

    private fun assertAttribute(attributes: List<Int>, key: Int, value: Int?) {
        val index = attributes.indexOf(key)
        assertTrue("missing EGL attribute 0x${key.toString(16)}", index >= 0)
        if (value != null) {
            assertEquals(value, attributes[index + 1])
        }
    }
}
