package com.dnrohr.eulerianmagnification.gl

import android.opengl.GLES30

class GlTemporalState(
    levelSizes: List<GlTextureSize>,
) {
    val levels: List<GlTemporalLevel> = levelSizes.map(::GlTemporalLevel)

    fun swap() {
        levels.forEach(GlTemporalLevel::swap)
    }

    fun release() {
        levels.forEach(GlTemporalLevel::release)
    }
}

class GlTemporalLevel(size: GlTextureSize) {
    private val lowpassTargets = listOf(
        GlRenderTarget(size, GlRenderTargetFormat.Rgba16f),
        GlRenderTarget(size, GlRenderTargetFormat.Rgba16f),
    )
    private val highpassTargets = listOf(
        GlRenderTarget(size, GlRenderTargetFormat.Rgba16f),
        GlRenderTarget(size, GlRenderTargetFormat.Rgba16f),
    )
    val bandpass: GlRenderTarget = GlRenderTarget(size, GlRenderTargetFormat.Rgba16f)
    private var currentIndex = 0

    val currentLowpass: GlRenderTarget get() = lowpassTargets[currentIndex]
    val previousLowpass: GlRenderTarget get() = lowpassTargets[1 - currentIndex]
    val currentHighpass: GlRenderTarget get() = highpassTargets[currentIndex]
    val previousHighpass: GlRenderTarget get() = highpassTargets[1 - currentIndex]
    val size: GlTextureSize get() = bandpass.size
    private val temporalFramebufferId: Int

    init {
        val framebuffers = IntArray(1)
        GLES30.glGenFramebuffers(1, framebuffers, 0)
        temporalFramebufferId = framebuffers[0]
        bindForTemporalUpdate()
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GlProgram.checkNoGlError("GlTemporalLevel")
    }

    fun bindForTemporalUpdate() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, temporalFramebufferId)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D,
            currentLowpass.textureId,
            0,
        )
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT1,
            GLES30.GL_TEXTURE_2D,
            currentHighpass.textureId,
            0,
        )
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT2,
            GLES30.GL_TEXTURE_2D,
            bandpass.textureId,
            0,
        )
        GLES30.glDrawBuffers(
            TEMPORAL_OUTPUT_ATTACHMENTS.size,
            TEMPORAL_OUTPUT_ATTACHMENTS,
            0,
        )
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            throw GlException("Temporal framebuffer incomplete: 0x${status.toString(16)}")
        }
        GLES30.glViewport(0, 0, size.width, size.height)
    }

    fun swap() {
        currentIndex = 1 - currentIndex
    }

    fun release() {
        GLES30.glDeleteFramebuffers(1, intArrayOf(temporalFramebufferId), 0)
        lowpassTargets.forEach(GlRenderTarget::release)
        highpassTargets.forEach(GlRenderTarget::release)
        bandpass.release()
    }

    companion object {
        private val TEMPORAL_OUTPUT_ATTACHMENTS = intArrayOf(
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_COLOR_ATTACHMENT1,
            GLES30.GL_COLOR_ATTACHMENT2,
        )
    }
}

object GlTemporalStateLayout {
    fun levelSizesFor(pyramid: GlPyramid): List<GlTextureSize> {
        return pyramid.levels.map { it.size }
    }
}

data class GlTemporalStatePlan(
    val levelSizes: List<GlTextureSize>,
) {
    val renderTargetCount: Int get() = levelSizes.size * TARGETS_PER_LEVEL

    companion object {
        const val TARGETS_PER_LEVEL = 5
    }
}
