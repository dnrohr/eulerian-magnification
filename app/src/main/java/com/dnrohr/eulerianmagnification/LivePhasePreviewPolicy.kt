package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import com.dnrohr.eulerianmagnification.analysis.RoiSource
import com.dnrohr.eulerianmagnification.gl.GlFrameStats
import com.dnrohr.eulerianmagnification.gl.GlTextureSize
import com.dnrohr.eulerianmagnification.gl.LivePhaseDiagnostics
import com.dnrohr.eulerianmagnification.gl.LivePhaseFallbackReason
import com.dnrohr.eulerianmagnification.gl.LivePhaseRoiPlan
import com.dnrohr.eulerianmagnification.gl.LivePhaseWarmupStatus

object LivePhasePreviewPolicy {
    fun decide(
        settings: AnalysisSettings,
        usingGlPreview: Boolean,
        glFrameStats: GlFrameStats,
        surfaceSize: GlTextureSize?,
        phaseRoi: NormalizedRect?,
        roiSource: RoiSource = RoiSource.Manual,
        phaseResourcesAvailable: Boolean = true,
    ): LivePhasePreviewDecision {
        if (!settings.mode.supportsLivePhase()) {
            return decision(
                requested = false,
                fallbackReason = LivePhaseFallbackReason.NotRequested,
            )
        }
        if (!usingGlPreview) {
            return decision(
                requested = true,
                fallbackReason = LivePhaseFallbackReason.UnsupportedGl,
            )
        }
        if (!phaseResourcesAvailable || surfaceSize == null) {
            return decision(
                requested = true,
                fallbackReason = LivePhaseFallbackReason.UnsupportedGl,
            )
        }
        if (roiSource == RoiSource.FullFrame) {
            return decision(
                requested = true,
                fallbackReason = LivePhaseFallbackReason.FullFrameUnsupported,
            )
        }
        if (phaseRoi == null) {
            return decision(
                requested = true,
                fallbackReason = if (roiSource == RoiSource.Manual) {
                    LivePhaseFallbackReason.MissingManualRoi
                } else {
                    LivePhaseFallbackReason.MissingAutoRoi
                },
            )
        }
        val roiPlan = LivePhaseRoiPlan(surfaceSize = surfaceSize, roi = phaseRoi)
        if (!roiPlan.fitsMemoryBudget) {
            return decision(
                requested = true,
                roiPlan = roiPlan,
                fallbackReason = LivePhaseFallbackReason.MemoryBudgetExceeded,
            )
        }
        if (hasSettledStats(glFrameStats) && timingUnhealthy(glFrameStats)) {
            return decision(
                requested = true,
                roiPlan = roiPlan,
                fallbackReason = LivePhaseFallbackReason.TimingUnhealthy,
            )
        }
        return LivePhasePreviewDecision(
            useLivePhase = true,
            roiPlan = roiPlan,
            diagnostics = LivePhaseDiagnostics(
                requested = true,
                warmupStatus = if (hasSettledStats(glFrameStats)) {
                    LivePhaseWarmupStatus.Warming
                } else {
                    LivePhaseWarmupStatus.Uninitialized
                },
                processingSize = roiPlan.processingSize,
            ),
        )
    }

    private fun decision(
        requested: Boolean,
        roiPlan: LivePhaseRoiPlan? = null,
        fallbackReason: LivePhaseFallbackReason,
    ): LivePhasePreviewDecision {
        return LivePhasePreviewDecision(
            useLivePhase = false,
            roiPlan = roiPlan,
            diagnostics = LivePhaseDiagnostics(
                requested = requested,
                processingSize = roiPlan?.processingSize,
                fallbackReason = if (requested) fallbackReason else null,
            ),
        )
    }

    private fun MagnificationMode.supportsLivePhase(): Boolean {
        return this == MagnificationMode.Tremor || this == MagnificationMode.ObjectVibration
    }

    private fun hasSettledStats(glFrameStats: GlFrameStats): Boolean {
        return glFrameStats.sampleCount >= MIN_SETTLED_SAMPLES
    }

    private fun timingUnhealthy(glFrameStats: GlFrameStats): Boolean {
        return glFrameStats.averageFps < MIN_GL_FPS || glFrameStats.averageFrameMillis > MAX_GL_FRAME_MILLIS
    }

    private const val MIN_SETTLED_SAMPLES = 10
    private const val MIN_GL_FPS = 23.5
    private const val MAX_GL_FRAME_MILLIS = 42.0
}

data class LivePhasePreviewDecision(
    val useLivePhase: Boolean,
    val roiPlan: LivePhaseRoiPlan? = null,
    val diagnostics: LivePhaseDiagnostics,
)
