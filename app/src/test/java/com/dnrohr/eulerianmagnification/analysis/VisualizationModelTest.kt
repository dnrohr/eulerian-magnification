package com.dnrohr.eulerianmagnification.analysis

import com.dnrohr.eulerianmagnification.LivePhasePreviewDecision
import com.dnrohr.eulerianmagnification.gl.GlTextureSize
import com.dnrohr.eulerianmagnification.gl.LivePhaseDiagnostics
import com.dnrohr.eulerianmagnification.gl.LivePhaseRoiPlan
import com.dnrohr.eulerianmagnification.gl.LivePhaseWarmupStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class VisualizationModelTest {
    @Test
    fun recordedPulseAmplifiedUsesLinearEvmRenderer() {
        val model = VisualizationModel.recorded(
            AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Amplified,
            ),
        )

        assertEquals(SignalSource.RecordedRoiGreenBandpass, model.signalSource)
        assertEquals(RendererKind.RecordedLinearEvm, model.renderer)
        assertEquals(VisualizationStyle.FullFrameAmplified, model.visualizationStyle)
    }

    @Test
    fun recordedMotionAmplifiedUsesRieszRenderer() {
        val model = VisualizationModel.recorded(
            AnalysisSettings(
                mode = MagnificationMode.Tremor,
                viewMode = ViewMode.Amplified,
            ),
        )

        assertEquals(SignalSource.RecordedRoiGreenBandpass, model.signalSource)
        assertEquals(RendererKind.RecordedRieszPhaseMotion, model.renderer)
        assertEquals(VisualizationStyle.FullFrameAmplified, model.visualizationStyle)
    }

    @Test
    fun recordedDifferenceKeepsDiagnosticRenderer() {
        val model = VisualizationModel.recorded(
            AnalysisSettings(
                mode = MagnificationMode.Tremor,
                viewMode = ViewMode.Difference,
            ),
        )

        assertEquals(RendererKind.RoiSignalDiagnostic, model.renderer)
        assertEquals(VisualizationStyle.RoiDifference, model.visualizationStyle)
    }

    @Test
    fun livePulseFullFrameReconstructionIsDistinctFromRoiTint() {
        val fullFrame = VisualizationModel.live(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Amplified,
            ),
            fullFrameColorPreview = true,
        )
        val roiTint = VisualizationModel.live(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Amplified,
            ),
            fullFrameColorPreview = false,
        )

        assertEquals(RendererKind.LiveLinearEvmReconstruction, fullFrame.renderer)
        assertEquals(VisualizationStyle.FullFrameAmplified, fullFrame.visualizationStyle)
        assertEquals(RendererKind.LiveRoiSignalTint, roiTint.renderer)
        assertEquals(VisualizationStyle.RoiSignalOverlay, roiTint.visualizationStyle)
    }

    @Test
    fun liveBreathingSeparatesTranslationSignalFromDiagnosticView() {
        val model = VisualizationModel.live(
            settings = AnalysisSettings(
                mode = MagnificationMode.Breathing,
                viewMode = ViewMode.Difference,
            ),
            fullFrameColorPreview = false,
        )

        assertEquals(SignalSource.RoiVerticalTranslation, model.signalSource)
        assertEquals(RendererKind.RoiSignalDiagnostic, model.renderer)
        assertEquals(VisualizationStyle.RoiDifference, model.visualizationStyle)
    }

    @Test
    fun livePulseDifferenceUsesFullFrameDifferenceWhenGlPreviewIsActive() {
        val model = VisualizationModel.live(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                viewMode = ViewMode.Difference,
            ),
            fullFrameColorPreview = true,
        )

        assertEquals(RendererKind.LiveLinearEvmReconstruction, model.renderer)
        assertEquals(VisualizationStyle.FullFrameDifference, model.visualizationStyle)
    }

    @Test
    fun liveMotionSplitUsesPhaseRendererWhenPhaseIsEnabled() {
        val model = VisualizationModel.live(
            settings = AnalysisSettings(
                mode = MagnificationMode.Tremor,
                viewMode = ViewMode.Split,
            ),
            fullFrameColorPreview = false,
            livePhasePreviewDecision = enabledPhaseDecision(),
        )

        assertEquals(SignalSource.RoiMotionEstimate, model.signalSource)
        assertEquals(RendererKind.LivePhaseMotion, model.renderer)
        assertEquals(VisualizationStyle.SplitComparison, model.visualizationStyle)
    }

    @Test
    fun liveMotionDifferenceUsesFullFrameDifferenceWhenPhaseIsEnabled() {
        val model = VisualizationModel.live(
            settings = AnalysisSettings(
                mode = MagnificationMode.Tremor,
                viewMode = ViewMode.Difference,
            ),
            fullFrameColorPreview = false,
            livePhasePreviewDecision = enabledPhaseDecision(),
        )

        assertEquals(RendererKind.LivePhaseMotion, model.renderer)
        assertEquals(VisualizationStyle.FullFrameDifference, model.visualizationStyle)
    }

    private fun enabledPhaseDecision(): LivePhasePreviewDecision {
        val plan = LivePhaseRoiPlan(
            surfaceSize = GlTextureSize(1080, 2400),
            roi = NormalizedRect(0.25f, 0.25f, 0.75f, 0.75f),
        )
        return LivePhasePreviewDecision(
            useLivePhase = true,
            roiPlan = plan,
            diagnostics = LivePhaseDiagnostics(
                requested = true,
                warmupStatus = LivePhaseWarmupStatus.Ready,
                processingSize = plan.processingSize,
            ),
        )
    }
}
