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
                ViewMode.Difference -> if (fullFrameColorPreview) {
                    RendererKind.LiveLinearEvmReconstruction
                } else {
                    RendererKind.RoiSignalDiagnostic
                }
                ViewMode.Amplified,
                ViewMode.Split,
                -> if (fullFrameColorPreview) {
                    RendererKind.LiveLinearEvmReconstruction
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
                ViewMode.Difference -> when (renderer) {
                    RendererKind.LiveLinearEvmReconstruction,
                    RendererKind.RecordedLinearEvm,
                    RendererKind.RecordedRieszPhaseMotion,
                    -> VisualizationStyle.FullFrameDifference
                    else -> VisualizationStyle.RoiDifference
                }
                ViewMode.Split -> VisualizationStyle.SplitComparison
                ViewMode.Amplified -> when (renderer) {
                    RendererKind.RecordedLinearEvm,
                    RendererKind.RecordedRieszPhaseMotion,
                    RendererKind.LiveLinearEvmReconstruction,
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
    LiveLinearEvmReconstruction("live_linear_evm_reconstruction", "Live linear EVM reconstruction"),
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
    FullFrameDifference("full_frame_difference", "Full-frame difference"),
    SplitComparison("split_comparison", "Split comparison"),
}
