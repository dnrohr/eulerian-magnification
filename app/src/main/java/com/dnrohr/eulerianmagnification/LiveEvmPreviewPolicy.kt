package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import com.dnrohr.eulerianmagnification.gl.GlFrameStats

data class LiveEvmPreviewDecision(
    val fullFrameColorPreview: Boolean,
    val label: String,
    val reason: String,
)

object LiveEvmPreviewPolicy {
    fun decide(
        settings: AnalysisSettings,
        usingGlPreview: Boolean,
        glFrameStats: GlFrameStats,
    ): LiveEvmPreviewDecision {
        if (!usingGlPreview) {
            return LiveEvmPreviewDecision(
                fullFrameColorPreview = false,
                label = "ROI signal preview",
                reason = "GL preview is not active",
            )
        }
        if (!settings.mode.supportsLiveLinearEvm()) {
            return LiveEvmPreviewDecision(
                fullFrameColorPreview = false,
                label = "ROI signal preview",
                reason = "Full-frame live linear EVM is currently limited to Pulse and Breathing",
            )
        }
        if (
            settings.viewMode != ViewMode.Amplified &&
            settings.viewMode != ViewMode.Split &&
            settings.viewMode != ViewMode.Difference
        ) {
            return LiveEvmPreviewDecision(
                fullFrameColorPreview = false,
                label = "ROI diagnostic preview",
                reason = "Raw keeps its diagnostic behavior",
            )
        }
        if (!hasSettledStats(glFrameStats)) {
            return LiveEvmPreviewDecision(
                fullFrameColorPreview = true,
                label = "Full-frame linear EVM preview",
                reason = "GL timing is still settling",
            )
        }
        if (glFrameStats.averageFps < MIN_GL_FPS || glFrameStats.averageFrameMillis > MAX_GL_FRAME_MILLIS) {
            return LiveEvmPreviewDecision(
                fullFrameColorPreview = false,
                label = "ROI signal preview",
                reason = "GL preview timing is below the live full-frame threshold",
            )
        }
        return LiveEvmPreviewDecision(
            fullFrameColorPreview = true,
            label = "Full-frame linear EVM preview",
            reason = "Pulse/Breathing Amplified/Difference/Split can use GL full-frame reconstruction",
        )
    }

    private fun MagnificationMode.supportsLiveLinearEvm(): Boolean {
        return this == MagnificationMode.Pulse || this == MagnificationMode.Breathing
    }

    private fun hasSettledStats(glFrameStats: GlFrameStats): Boolean {
        return glFrameStats.sampleCount >= MIN_SETTLED_SAMPLES
    }

    private const val MIN_SETTLED_SAMPLES = 10
    private const val MIN_GL_FPS = 24.0
    private const val MAX_GL_FRAME_MILLIS = 42.0
}
