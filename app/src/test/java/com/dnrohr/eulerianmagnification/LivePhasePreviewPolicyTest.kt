package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import com.dnrohr.eulerianmagnification.gl.GlFrameStats
import com.dnrohr.eulerianmagnification.gl.GlTextureSize
import com.dnrohr.eulerianmagnification.gl.LivePhaseFallbackReason
import com.dnrohr.eulerianmagnification.gl.LivePhaseWarmupStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LivePhasePreviewPolicyTest {
    @Test
    fun enablesLivePhaseForMotionWithGlAndManualRoi() {
        val decision = LivePhasePreviewPolicy.decide(
            settings = AnalysisSettings(mode = MagnificationMode.Tremor),
            usingGlPreview = true,
            glFrameStats = healthyStats(),
            surfaceSize = GlTextureSize(1080, 2400),
            manualRoi = roi(),
        )

        assertTrue(decision.useLivePhase)
        assertNotNull(decision.roiPlan)
        assertEquals(LivePhaseWarmupStatus.Warming, decision.diagnostics.warmupStatus)
        assertEquals(GlTextureSize(144, 320), decision.roiPlan!!.processingSize)
    }

    @Test
    fun nonMotionModesDoNotRequestLivePhase() {
        val decision = LivePhasePreviewPolicy.decide(
            settings = AnalysisSettings(mode = MagnificationMode.Pulse),
            usingGlPreview = true,
            glFrameStats = healthyStats(),
            surfaceSize = GlTextureSize(1080, 2400),
            manualRoi = roi(),
        )

        assertFalse(decision.useLivePhase)
        assertFalse(decision.diagnostics.requested)
        assertEquals("phase: not requested", decision.diagnostics.summary)
    }

    @Test
    fun requiresGlPreview() {
        val decision = LivePhasePreviewPolicy.decide(
            settings = AnalysisSettings(mode = MagnificationMode.Tremor),
            usingGlPreview = false,
            glFrameStats = healthyStats(),
            surfaceSize = GlTextureSize(1080, 2400),
            manualRoi = roi(),
        )

        assertFalse(decision.useLivePhase)
        assertEquals(LivePhaseFallbackReason.UnsupportedGl, decision.diagnostics.fallbackReason)
    }

    @Test
    fun requiresManualRoi() {
        val decision = LivePhasePreviewPolicy.decide(
            settings = AnalysisSettings(mode = MagnificationMode.ObjectVibration),
            usingGlPreview = true,
            glFrameStats = healthyStats(),
            surfaceSize = GlTextureSize(1080, 2400),
            manualRoi = null,
        )

        assertFalse(decision.useLivePhase)
        assertEquals(LivePhaseFallbackReason.MissingManualRoi, decision.diagnostics.fallbackReason)
    }

    @Test
    fun rejectsUnhealthySettledGlTiming() {
        val decision = LivePhasePreviewPolicy.decide(
            settings = AnalysisSettings(mode = MagnificationMode.Tremor),
            usingGlPreview = true,
            glFrameStats = GlFrameStats(
                averageFrameMillis = 55.0,
                averageFps = 18.0,
                sampleCount = 60,
            ),
            surfaceSize = GlTextureSize(1080, 2400),
            manualRoi = roi(),
        )

        assertFalse(decision.useLivePhase)
        assertEquals(LivePhaseFallbackReason.TimingUnhealthy, decision.diagnostics.fallbackReason)
    }

    @Test
    fun allowsTimingToSettleBeforeRejectingPhase() {
        val decision = LivePhasePreviewPolicy.decide(
            settings = AnalysisSettings(mode = MagnificationMode.Tremor),
            usingGlPreview = true,
            glFrameStats = GlFrameStats(
                averageFrameMillis = 55.0,
                averageFps = 18.0,
                sampleCount = 2,
            ),
            surfaceSize = GlTextureSize(1080, 2400),
            manualRoi = roi(),
        )

        assertTrue(decision.useLivePhase)
        assertEquals(LivePhaseWarmupStatus.Uninitialized, decision.diagnostics.warmupStatus)
    }

    @Test
    fun reportsUnavailablePhaseResources() {
        val decision = LivePhasePreviewPolicy.decide(
            settings = AnalysisSettings(mode = MagnificationMode.Tremor),
            usingGlPreview = true,
            glFrameStats = healthyStats(),
            surfaceSize = GlTextureSize(1080, 2400),
            manualRoi = roi(),
            phaseResourcesAvailable = false,
        )

        assertFalse(decision.useLivePhase)
        assertEquals(LivePhaseFallbackReason.UnsupportedGl, decision.diagnostics.fallbackReason)
    }

    private fun healthyStats(): GlFrameStats {
        return GlFrameStats(
            averageFrameMillis = 30.0,
            averageFps = 30.0,
            sampleCount = 60,
        )
    }

    private fun roi(): NormalizedRect {
        return NormalizedRect(0.25f, 0.25f, 0.5f, 0.5f)
    }
}
