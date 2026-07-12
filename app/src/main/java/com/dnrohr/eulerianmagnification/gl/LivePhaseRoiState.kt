package com.dnrohr.eulerianmagnification.gl

import android.opengl.GLES30

class LivePhaseRoiState(
    val plan: LivePhaseRoiPlan,
) {
    private val size = plan.processingSize
    private val wrappedPhaseTargets = pingPongTargets()
    private val unwrappedPhaseTargets = pingPongTargets()
    private val lowpassTargets = pingPongTargets()
    private val highpassTargets = pingPongTargets()
    val extractedRoi: GlRenderTarget = target()
    val rieszComponents: GlRenderTarget = target()
    val projectedPhase: GlRenderTarget = target()
    val amplifiedPhase: GlRenderTarget = target()
    val reconstructedRoi: GlRenderTarget = target()
    private var currentIndex = 0
    private val temporalFramebufferId: Int

    val currentWrappedPhase: GlRenderTarget get() = wrappedPhaseTargets[currentIndex]
    val previousWrappedPhase: GlRenderTarget get() = wrappedPhaseTargets[1 - currentIndex]
    val currentUnwrappedPhase: GlRenderTarget get() = unwrappedPhaseTargets[currentIndex]
    val previousUnwrappedPhase: GlRenderTarget get() = unwrappedPhaseTargets[1 - currentIndex]
    val currentLowpass: GlRenderTarget get() = lowpassTargets[currentIndex]
    val previousLowpass: GlRenderTarget get() = lowpassTargets[1 - currentIndex]
    val currentHighpass: GlRenderTarget get() = highpassTargets[currentIndex]
    val previousHighpass: GlRenderTarget get() = highpassTargets[1 - currentIndex]

    init {
        val framebuffers = IntArray(1)
        GLES30.glGenFramebuffers(1, framebuffers, 0)
        temporalFramebufferId = framebuffers[0]
        bindForTemporalUpdate()
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GlProgram.checkNoGlError("LivePhaseRoiState")
    }

    fun bindForTemporalUpdate() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, temporalFramebufferId)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D,
            currentWrappedPhase.textureId,
            0,
        )
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT1,
            GLES30.GL_TEXTURE_2D,
            currentUnwrappedPhase.textureId,
            0,
        )
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT2,
            GLES30.GL_TEXTURE_2D,
            currentLowpass.textureId,
            0,
        )
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT3,
            GLES30.GL_TEXTURE_2D,
            currentHighpass.textureId,
            0,
        )
        GLES30.glDrawBuffers(TEMPORAL_OUTPUT_ATTACHMENTS.size, TEMPORAL_OUTPUT_ATTACHMENTS, 0)
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            throw GlException("Live phase temporal framebuffer incomplete: 0x${status.toString(16)}")
        }
        GLES30.glViewport(0, 0, size.width, size.height)
    }

    fun swap() {
        currentIndex = 1 - currentIndex
    }

    fun matches(plan: LivePhaseRoiPlan): Boolean {
        return this.plan.processingSize == plan.processingSize &&
            this.plan.roi == plan.roi &&
            this.plan.surfaceSize == plan.surfaceSize
    }

    fun release() {
        GLES30.glDeleteFramebuffers(1, intArrayOf(temporalFramebufferId), 0)
        wrappedPhaseTargets.forEach(GlRenderTarget::release)
        unwrappedPhaseTargets.forEach(GlRenderTarget::release)
        lowpassTargets.forEach(GlRenderTarget::release)
        highpassTargets.forEach(GlRenderTarget::release)
        extractedRoi.release()
        rieszComponents.release()
        projectedPhase.release()
        amplifiedPhase.release()
        reconstructedRoi.release()
    }

    private fun pingPongTargets(): List<GlRenderTarget> = listOf(target(), target())

    private fun target(): GlRenderTarget = GlRenderTarget(size, GlRenderTargetFormat.Rgba16f)

    companion object {
        private val TEMPORAL_OUTPUT_ATTACHMENTS = intArrayOf(
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_COLOR_ATTACHMENT1,
            GLES30.GL_COLOR_ATTACHMENT2,
            GLES30.GL_COLOR_ATTACHMENT3,
        )
    }
}
