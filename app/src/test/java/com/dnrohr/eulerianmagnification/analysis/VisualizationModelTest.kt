package com.dnrohr.eulerianmagnification.analysis

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
    fun livePulseFullFrameBridgeIsDistinctFromRoiTint() {
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

        assertEquals(RendererKind.LiveGlFullFrameColorBridge, fullFrame.renderer)
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

        assertEquals(RendererKind.LiveGlFullFrameColorBridge, model.renderer)
        assertEquals(VisualizationStyle.FullFrameDifference, model.visualizationStyle)
    }
}
