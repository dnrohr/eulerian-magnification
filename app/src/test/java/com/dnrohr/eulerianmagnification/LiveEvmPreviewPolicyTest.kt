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
    fun keepsMotionModesOnRoiSignalPreview() {
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
    fun rawRemainsDiagnosticView() {
        assertFalse(
            LiveEvmPreviewPolicy.decide(
                settings = AnalysisSettings(viewMode = ViewMode.Raw),
                usingGlPreview = true,
                glFrameStats = healthyStats(),
            ).fullFrameColorPreview,
        )
    }

    private fun healthyStats(): GlFrameStats {
        return GlFrameStats(
            averageFrameMillis = 30.0,
            averageFps = 30.0,
            sampleCount = 60,
        )
    }
}
