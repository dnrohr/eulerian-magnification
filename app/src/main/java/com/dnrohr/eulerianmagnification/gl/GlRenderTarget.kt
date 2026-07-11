package com.dnrohr.eulerianmagnification.gl

import android.opengl.GLES30

class GlRenderTarget(
    val size: GlTextureSize,
    private val format: GlRenderTargetFormat = GlRenderTargetFormat.Rgba8,
) {
    val textureId: Int
    val framebufferId: Int

    init {
        val textures = IntArray(1)
        val framebuffers = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, format.filter)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, format.filter)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            format.internalFormat,
            size.width,
            size.height,
            0,
            format.format,
            format.type,
            null,
        )

        GLES30.glGenFramebuffers(1, framebuffers, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffers[0])
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D,
            textures[0],
            0,
        )
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            throw GlException("Framebuffer incomplete: 0x${status.toString(16)}")
        }
        GLES30.glClearBufferfv(GLES30.GL_COLOR, 0, CLEAR_COLOR, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GlProgram.checkNoGlError("GlRenderTarget")

        textureId = textures[0]
        framebufferId = framebuffers[0]
    }

    fun bind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId)
        GLES30.glDrawBuffers(1, COLOR_ATTACHMENT0, 0)
        GLES30.glViewport(0, 0, size.width, size.height)
    }

    fun release() {
        GLES30.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
        GLES30.glDeleteTextures(1, intArrayOf(textureId), 0)
    }

    companion object {
        private val COLOR_ATTACHMENT0 = intArrayOf(GLES30.GL_COLOR_ATTACHMENT0)
        private val CLEAR_COLOR = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
    }
}

enum class GlRenderTargetFormat(
    val internalFormat: Int,
    val format: Int,
    val type: Int,
    val filter: Int,
) {
    Rgba8(
        internalFormat = GLES30.GL_RGBA,
        format = GLES30.GL_RGBA,
        type = GLES30.GL_UNSIGNED_BYTE,
        filter = GLES30.GL_LINEAR,
    ),
    Rgba16f(
        internalFormat = GLES30.GL_RGBA16F,
        format = GLES30.GL_RGBA,
        type = GLES30.GL_HALF_FLOAT,
        filter = GLES30.GL_NEAREST,
    ),
}

data class GlTextureSize(
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
    }
}

object GlRenderTargetCapabilities {
    fun supportsHalfFloatColorBuffer(extensions: String?): Boolean {
        val tokens = extensions
            ?.split(' ')
            ?.filter(String::isNotBlank)
            ?.toSet()
            .orEmpty()
        return tokens.any { it in HALF_FLOAT_COLOR_BUFFER_EXTENSIONS }
    }

    private val HALF_FLOAT_COLOR_BUFFER_EXTENSIONS = setOf(
        "GL_EXT_color_buffer_half_float",
        "GL_EXT_color_buffer_float",
        "GL_KHR_color_buffer_float",
    )
}
