package com.dnrohr.eulerianmagnification.analysis

data class VisualizationModel(
    val signalSource: SignalSource,
    val renderer: RendererKind,
    val visualizationStyle: VisualizationStyle,
) {
    companion object {
        fun recorded(settings: AnalysisSettings): VisualizationModel {
            val renderer = when (settings.viewMode) {
                ViewMode.Raw -> RendererKind.RawPassthrough
                ViewMode.Difference -> RendererKind.RoiSignalDiagnostic
                ViewMode.Amplified,
                ViewMode.Split,
                -> if (settings.mode == MagnificationMode.Pulse) {
                    RendererKind.RecordedLinearEvm
                } else {
                    RendererKind.RecordedRieszPhaseMotion
                }
            }
            return VisualizationModel(
                signalSource = SignalSource.RecordedRoiGreenBandpass,
                renderer = renderer,
                visualizationStyle = visualizationStyleFor(settings.viewMode, renderer),
            )
        }

        fun live(
            settings: AnalysisSettings,
            fullFrameColorPreview: Boolean,
        ): VisualizationModel {
            val renderer = when (settings.viewMode) {
                ViewMode.Raw -> RendererKind.RawPassthrough
                ViewMode.Difference -> RendererKind.RoiSignalDiagnostic
                ViewMode.Amplified,
                ViewMode.Split,
                -> if (fullFrameColorPreview) {
                    RendererKind.LiveGlFullFrameColorBridge
                } else {
                    RendererKind.LiveRoiSignalTint
                }
            }
            return VisualizationModel(
                signalSource = liveSignalSource(settings.mode),
                renderer = renderer,
                visualizationStyle = visualizationStyleFor(settings.viewMode, renderer),
            )
        }

        private fun liveSignalSource(mode: MagnificationMode): SignalSource {
            return when (mode) {
                MagnificationMode.Pulse -> SignalSource.RoiGreenBandpass
                MagnificationMode.Breathing -> SignalSource.RoiVerticalTranslation
                MagnificationMode.Tremor,
                MagnificationMode.ObjectVibration,
                -> SignalSource.RoiMotionEstimate
            }
        }

        private fun visualizationStyleFor(
            viewMode: ViewMode,
            renderer: RendererKind,
        ): VisualizationStyle {
            return when (viewMode) {
                ViewMode.Raw -> VisualizationStyle.Raw
                ViewMode.Difference -> VisualizationStyle.RoiDifference
                ViewMode.Split -> VisualizationStyle.SplitComparison
                ViewMode.Amplified -> when (renderer) {
                    RendererKind.RecordedLinearEvm,
                    RendererKind.RecordedRieszPhaseMotion,
                    RendererKind.LiveGlFullFrameColorBridge,
                    -> VisualizationStyle.FullFrameAmplified
                    else -> VisualizationStyle.RoiSignalOverlay
                }
            }
        }
    }
}

enum class SignalSource(
    val id: String,
    val label: String,
) {
    RoiGreenBandpass("roi_green_bandpass", "ROI green bandpass"),
    RecordedRoiGreenBandpass("recorded_roi_green_bandpass", "Recorded ROI green bandpass"),
    RoiVerticalTranslation("roi_vertical_translation", "ROI vertical translation"),
    RoiMotionEstimate("roi_motion_estimate", "ROI motion estimate"),
}

enum class RendererKind(
    val id: String,
    val label: String,
) {
    RawPassthrough("raw_passthrough", "Raw passthrough"),
    RoiSignalDiagnostic("roi_signal_diagnostic", "ROI signal diagnostic"),
    LiveRoiSignalTint("live_roi_signal_tint", "Live ROI signal tint"),
    LiveGlFullFrameColorBridge("live_gl_full_frame_color_bridge", "Live GL full-frame color bridge"),
    RecordedLinearEvm("recorded_linear_evm", "Recorded full-frame linear EVM"),
    RecordedRieszPhaseMotion("recorded_riesz_phase_motion", "Recorded Riesz phase motion"),
}

enum class VisualizationStyle(
    val id: String,
    val label: String,
) {
    Raw("raw", "Raw"),
    RoiSignalOverlay("roi_signal_overlay", "ROI signal overlay"),
    RoiDifference("roi_difference", "ROI difference"),
    FullFrameAmplified("full_frame_amplified", "Full-frame amplified"),
    SplitComparison("split_comparison", "Split comparison"),
}
