package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import com.dnrohr.eulerianmagnification.gl.GlFrameStats
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveEvmPreviewPolicyTest {
    @Test
    fun enablesFullFrameColorForPulseAmplifiedOnHealthyGlPreview() {
        val decision = LiveEvmPreviewPolicy.decide(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Amplified,
            ),
            usingGlPreview = true,
            glFrameStats = healthyStats(),
        )

        assertTrue(decision.fullFrameColorPreview)
    }

    @Test
    fun enablesFullFrameLinearEvmForBreathingAmplifiedOnHealthyGlPreview() {
        val decision = LiveEvmPreviewPolicy.decide(
            settings = AnalysisSettings(
                mode = MagnificationMode.Breathing,
                viewMode = ViewMode.Amplified,
            ),
            usingGlPreview = true,
            glFrameStats = healthyStats(),
        )

        assertTrue(decision.fullFrameColorPreview)
        assertTrue(decision.label.contains("linear EVM"))
        assertTrue(decision.reason.contains("Pulse/Breathing"))
    }

    @Test
    fun enablesFullFrameColorForPulseSplitWhileStatsSettle() {
        val decision = LiveEvmPreviewPolicy.decide(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Split,
            ),
            usingGlPreview = true,
            glFrameStats = GlFrameStats(),
        )

        assertTrue(decision.fullFrameColorPreview)
    }

    @Test
    fun keepsHighFrequencyMotionModesOnRoiSignalPreview() {
        val decision = LiveEvmPreviewPolicy.decide(
            settings = AnalysisSettings(
                mode = MagnificationMode.Tremor,
                viewMode = ViewMode.Split,
            ),
            usingGlPreview = true,
            glFrameStats = healthyStats(),
        )

        assertFalse(decision.fullFrameColorPreview)
    }

    @Test
    fun disablesFullFrameColorWhenGlTimingFallsBelowThreshold() {
        val decision = LiveEvmPreviewPolicy.decide(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Amplified,
            ),
            usingGlPreview = true,
            glFrameStats = GlFrameStats(
                averageFrameMillis = 55.0,
                averageFps = 18.0,
                sampleCount = 60,
            ),
        )

        assertFalse(decision.fullFrameColorPreview)
    }

    @Test
    fun disablesFullFrameColorWhenCameraCadenceFallsBelowThresholdDespiteFastRendering() {
        val decision = LiveEvmPreviewPolicy.decide(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Amplified,
            ),
            usingGlPreview = true,
            glFrameStats = GlFrameStats(
                averageFrameMillis = 10.0,
                averageFps = 8.0,
                sampleCount = 60,
                renderSampleCount = 60,
            ),
        )

        assertFalse(decision.fullFrameColorPreview)
    }

    @Test
    fun disablesFullFrameColorWhenAnalysisCadenceFallsBelowThresholdDespiteFastRendering() {
        val decision = LiveEvmPreviewPolicy.decide(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Amplified,
            ),
            usingGlPreview = true,
            glFrameStats = healthyStats(),
            analysisFps = 12.0,
        )

        assertFalse(decision.fullFrameColorPreview)
        assertTrue(decision.reason.contains("Analysis FPS"))
    }

    @Test
    fun disablesFullFrameColorWhenThermalStateIsCritical() {
        val decision = LiveEvmPreviewPolicy.decide(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Amplified,
            ),
            usingGlPreview = true,
            glFrameStats = healthyStats(),
            analysisFps = 30.0,
            thermalStatus = "critical",
        )

        assertFalse(decision.fullFrameColorPreview)
        assertTrue(decision.reason.contains("thermal"))
    }

    @Test
    fun disablesFullFrameColorWhenThermalStateIsSevere() {
        val decision = LiveEvmPreviewPolicy.decide(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Amplified,
            ),
            usingGlPreview = true,
            glFrameStats = healthyStats(),
            analysisFps = 30.0,
            thermalStatus = "severe",
        )

        assertFalse(decision.fullFrameColorPreview)
        assertTrue(decision.reason.contains("thermal"))
    }

    @Test
    fun keepsFullFrameColorEnabledForModerateThermalState() {
        val decision = LiveEvmPreviewPolicy.decide(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Amplified,
            ),
            usingGlPreview = true,
            glFrameStats = healthyStats(),
            analysisFps = 30.0,
            thermalStatus = "moderate",
        )

        assertTrue(decision.fullFrameColorPreview)
    }

    @Test
    fun keepsFullFrameColorEnabledForTwentyFourFpsCameraCadence() {
        val decision = LiveEvmPreviewPolicy.decide(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Amplified,
            ),
            usingGlPreview = true,
            glFrameStats = GlFrameStats(
                averageFrameMillis = 41.0,
                averageFps = 24.0,
                sampleCount = 60,
            ),
        )

        assertTrue(decision.fullFrameColorPreview)
    }

    @Test
    fun enablesFullFrameColorForPulseDifferenceOnHealthyGlPreview() {
        val decision = LiveEvmPreviewPolicy.decide(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Difference,
            ),
            usingGlPreview = true,
            glFrameStats = healthyStats(),
        )

        assertTrue(decision.fullFrameColorPreview)
    }

    @Test
    fun enablesFullFrameRawPassthroughOnHealthyGlPreview() {
        val decision = LiveEvmPreviewPolicy.decide(
            settings = AnalysisSettings(viewMode = ViewMode.Raw),
            usingGlPreview = true,
            glFrameStats = healthyStats(),
        )

        assertTrue(decision.fullFrameColorPreview)
        assertTrue(decision.label.contains("Raw"))
        assertTrue(decision.reason.contains("zero amplification"))
    }

    private fun healthyStats(): GlFrameStats {
        return GlFrameStats(
            averageFrameMillis = 30.0,
            averageFps = 30.0,
            sampleCount = 60,
        )
    }
}
