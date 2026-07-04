package com.dnrohr.eulerianmagnification.gl

import org.junit.Assert.assertTrue
import org.junit.Test

class RgbTextureShaderSourceTest {
    @Test
    fun fragmentShaderSamplesTexture2d() {
        assertTrue(RgbTextureShaderSource.FRAGMENT.contains("sampler2D"))
        assertTrue(RgbTextureShaderSource.FRAGMENT.contains("uInputTexture"))
    }

    @Test
    fun vertexShaderPassesTexCoord() {
        assertTrue(RgbTextureShaderSource.VERTEX.contains("aTexCoord"))
        assertTrue(RgbTextureShaderSource.VERTEX.contains("vTexCoord"))
    }
}
