package com.dnrohr.eulerianmagnification.recording

import com.dnrohr.eulerianmagnification.gl.GlFrameStats

data class RecordingRendererDiagnostics(
    val previewPath: String,
    val glRenderPath: String? = null,
    val glRenderPathLabel: String? = null,
    val glAverageFps: Double? = null,
    val glAverageFrameMillis: Double? = null,
    val reconstructionSummary: String? = null,
    val reconstructionFallback: String? = null,
    val phaseSummary: String? = null,
    val phaseFallback: String? = null,
) {
    companion object {
        fun from(
            usingGlPreview: Boolean,
            glFrameStats: GlFrameStats,
        ): RecordingRendererDiagnostics {
            return if (usingGlPreview) {
                RecordingRendererDiagnostics(
                    previewPath = "gl",
                    glRenderPath = glFrameStats.renderPath.name,
                    glRenderPathLabel = glFrameStats.renderPath.label,
                    glAverageFps = glFrameStats.averageFps,
                    glAverageFrameMillis = glFrameStats.averageFrameMillis,
                    reconstructionSummary = glFrameStats.reconstructionDiagnostics.summary(),
                    reconstructionFallback = glFrameStats.reconstructionDiagnostics.fallbackReason.label,
                    phaseSummary = glFrameStats.phaseDiagnostics.summary,
                    phaseFallback = glFrameStats.phaseDiagnostics.fallbackReason?.label,
                )
            } else {
                RecordingRendererDiagnostics(previewPath = "camerax")
            }
        }
    }
}
