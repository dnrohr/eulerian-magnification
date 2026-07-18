package com.dnrohr.eulerianmagnification.quality

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.RoiSource
import kotlin.math.abs

class QualityEvaluator {
    fun evaluate(
        sample: AnalysisSample,
        settings: AnalysisSettings = AnalysisSettings(),
        lightingFlickerLikely: Boolean = false,
        lightingUnstable: Boolean = false,
        cameraFrameFps: Double? = null,
        cameraFrameSampleCount: Int = 0,
        cameraFrameStalled: Boolean = false,
        roiSource: RoiSource = RoiSource.Auto,
        thermalStatus: String = THERMAL_STATUS_NONE,
    ): List<QualityStatus> {
        return buildList {
            if (sample.roi == null) add(QualityStatus.FaceMissing)
            if (sample.averageGreen in 0.01..LOW_LIGHT_GREEN_THRESHOLD) add(QualityStatus.TooDark)
            if (thermalStatus.isThermalWarning()) add(QualityStatus.ThermalHigh)
            if (sample.analysisFps in 0.01..<LOW_FPS_THRESHOLD) {
                add(if (roiSource == RoiSource.FullFrame) QualityStatus.FullFrameSlow else QualityStatus.LowFps)
            }
            if (
                cameraFrameSampleCount >= MIN_CAMERA_FRAME_SAMPLES &&
                cameraFrameFps != null &&
                cameraFrameFps in 0.01..<LOW_FPS_THRESHOLD
            ) {
                add(QualityStatus.CameraFpsLow)
            }
            if (cameraFrameStalled) add(QualityStatus.CameraFrozen)
            if (!sample.timestampMonotonic) add(QualityStatus.TimingUnstable)
            if (lightingFlickerLikely) add(QualityStatus.LightingFlicker)
            if (lightingUnstable) add(QualityStatus.LightingUnstable)
            if (sample.translation.magnitude >= CAMERA_MOTION_THRESHOLD) add(QualityStatus.CameraMotion)
            if (settings.mode.isHighFrequencyMode && sample.translation.magnitude >= HIGH_FREQUENCY_MOTION_THRESHOLD) {
                add(QualityStatus.ModeMotionRisk)
            }
            if (settings.mode.isHighFrequencyMode && settings.amplification > HIGH_FREQUENCY_AMPLIFICATION_THRESHOLD) {
                add(QualityStatus.AmplificationRisk)
            }
            if (sample.roi != null && sample.analysisFps > 0.0 && abs(sample.bandpassedGreen) < WEAK_SIGNAL_THRESHOLD) {
                add(QualityStatus.SignalWeak)
            }
        }.ifEmpty { listOf(QualityStatus.Good) }
    }

    companion object {
        private const val LOW_LIGHT_GREEN_THRESHOLD = 45.0
        private const val LOW_FPS_THRESHOLD = 23.5
        private const val MIN_CAMERA_FRAME_SAMPLES = 10
        private const val WEAK_SIGNAL_THRESHOLD = 0.015
        private const val CAMERA_MOTION_THRESHOLD = 0.018f
        private const val HIGH_FREQUENCY_MOTION_THRESHOLD = 0.008f
        private const val HIGH_FREQUENCY_AMPLIFICATION_THRESHOLD = 18.0f
        private const val THERMAL_STATUS_NONE = "none"
    }
}

private val MagnificationMode.isHighFrequencyMode: Boolean
    get() = this == MagnificationMode.Tremor || this == MagnificationMode.ObjectVibration

private fun String.isThermalWarning(): Boolean {
    return lowercase() in setOf("moderate", "severe", "critical", "emergency", "shutdown")
}

enum class QualityStatus(
    val label: String,
    val action: String,
) {
    Good("Good", "Keep this setup."),
    FaceMissing("Face missing", "Frame the face or select a manual ROI."),
    TooDark("Too dark", "Use brighter, steady light."),
    ThermalHigh("Thermal high", "Let the phone cool before validation."),
    LowFps("Low FPS", "Close apps or reduce device load."),
    FullFrameSlow("Full frame slow", "Switch to Auto ROI for live preview."),
    CameraFpsLow("Camera FPS low", "Hide controls or use Auto ROI."),
    CameraFrozen("Camera frozen", "Restart the preview or app before validating."),
    TimingUnstable("Timing unstable", "Restart the preview if timing keeps jumping."),
    LightingFlicker("Lighting flicker", "Try daylight or a non-flickering lamp."),
    LightingUnstable("Exposure unstable", "Wait for exposure to settle, then lock AE/AWB."),
    CameraMotion("ROI motion", "Mount the phone or redraw a stable ROI."),
    ModeMotionRisk("Mode motion risk", "Use a tripod for high-frequency modes."),
    AmplificationRisk("Amplification risk", "Lower amplification below 18x."),
    SignalWeak("Signal weak", "Use steadier light or choose a stronger ROI."),
}
