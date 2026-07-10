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
        if (settings.mode != MagnificationMode.Pulse) {
            return LiveEvmPreviewDecision(
                fullFrameColorPreview = false,
                label = "ROI signal preview",
                reason = "Full-frame live preview is currently limited to Pulse color",
            )
        }
        if (settings.viewMode != ViewMode.Amplified && settings.viewMode != ViewMode.Split) {
            return LiveEvmPreviewDecision(
                fullFrameColorPreview = false,
                label = "ROI diagnostic preview",
                reason = "Raw and Difference keep their diagnostic behavior",
            )
        }
        if (!hasSettledStats(glFrameStats)) {
            return LiveEvmPreviewDecision(
                fullFrameColorPreview = true,
                label = "Full-frame color preview",
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
            label = "Full-frame color preview",
            reason = "Pulse Amplified/Split can use GL full-frame color output",
        )
    }

    private fun hasSettledStats(glFrameStats: GlFrameStats): Boolean {
        return glFrameStats.sampleCount >= MIN_SETTLED_SAMPLES
    }

    private const val MIN_SETTLED_SAMPLES = 10
    private const val MIN_GL_FPS = 24.0
    private const val MAX_GL_FRAME_MILLIS = 42.0
}
