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
        analysisFps: Double = 0.0,
        thermalStatus: String = THERMAL_STATUS_NONE,
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
        if (thermalStatus.isThermalSevereOrWorse()) {
            return LiveEvmPreviewDecision(
                fullFrameColorPreview = false,
                label = "ROI signal preview",
                reason = "Device thermal state is too high for live full-frame reconstruction",
            )
        }
        if (!hasSettledStats(glFrameStats)) {
            return LiveEvmPreviewDecision(
                fullFrameColorPreview = true,
                label = settings.fullFramePreviewLabel(),
                reason = if (settings.viewMode == ViewMode.Raw) {
                    "Raw uses the GL reconstruction graph with zero amplification"
                } else {
                    "GL timing is still settling"
                },
            )
        }
        if (glFrameStats.averageFps < MIN_GL_FPS || glFrameStats.averageFrameMillis > MAX_GL_FRAME_MILLIS) {
            return LiveEvmPreviewDecision(
                fullFrameColorPreview = false,
                label = "ROI signal preview",
                reason = "GL preview timing is below the live full-frame threshold",
            )
        }
        if (analysisFps in 0.01..<MIN_ANALYSIS_FPS) {
            return LiveEvmPreviewDecision(
                fullFrameColorPreview = false,
                label = "ROI signal preview",
                reason = "Analysis FPS is below the live full-frame threshold",
            )
        }
        return LiveEvmPreviewDecision(
            fullFrameColorPreview = true,
            label = settings.fullFramePreviewLabel(),
            reason = if (settings.viewMode == ViewMode.Raw) {
                "Raw uses the GL reconstruction graph with zero amplification"
            } else {
                "Pulse/Breathing Raw/Amplified/Difference/Split can use GL full-frame reconstruction"
            },
        )
    }

    private fun AnalysisSettings.fullFramePreviewLabel(): String {
        return if (viewMode == ViewMode.Raw) "Raw full-frame preview" else "Full-frame linear EVM preview"
    }

    private fun MagnificationMode.supportsLiveLinearEvm(): Boolean {
        return this == MagnificationMode.Pulse || this == MagnificationMode.Breathing
    }

    private fun hasSettledStats(glFrameStats: GlFrameStats): Boolean {
        return glFrameStats.sampleCount >= MIN_SETTLED_SAMPLES
    }

    private const val MIN_SETTLED_SAMPLES = 10
    private const val MIN_GL_FPS = 23.5
    private const val MIN_ANALYSIS_FPS = 23.5
    private const val MAX_GL_FRAME_MILLIS = 42.0
    private const val THERMAL_STATUS_NONE = "none"
}

private fun String.isThermalSevereOrWorse(): Boolean {
    return lowercase() in setOf("severe", "critical", "emergency", "shutdown")
}
