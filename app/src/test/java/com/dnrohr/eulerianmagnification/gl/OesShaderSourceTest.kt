package com.dnrohr.eulerianmagnification.gl

import org.junit.Assert.assertTrue
import org.junit.Test

class OesShaderSourceTest {
    @Test
    fun vertexShaderDeclaresTransformAndTexCoord() {
        assertTrue(OesShaderSource.VERTEX.contains("uTexTransform"))
        assertTrue(OesShaderSource.VERTEX.contains("aTexCoord"))
        assertTrue(OesShaderSource.VERTEX.contains("vTexCoord"))
    }

    @Test
    fun fragmentShaderUsesExternalOesTexture() {
        assertTrue(OesShaderSource.FRAGMENT.contains("samplerExternalOES"))
        assertTrue(OesShaderSource.FRAGMENT.contains("GL_OES_EGL_image_external_essl3"))
    }
}
