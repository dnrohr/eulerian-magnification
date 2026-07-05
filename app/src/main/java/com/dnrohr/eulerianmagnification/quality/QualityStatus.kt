package com.dnrohr.eulerianmagnification.quality

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import kotlin.math.abs

class QualityEvaluator {
    fun evaluate(
        sample: AnalysisSample,
        settings: AnalysisSettings = AnalysisSettings(),
        lightingFlickerLikely: Boolean = false,
    ): List<QualityStatus> {
        return buildList {
            if (sample.roi == null) add(QualityStatus.FaceMissing)
            if (sample.averageGreen in 0.01..LOW_LIGHT_GREEN_THRESHOLD) add(QualityStatus.TooDark)
            if (sample.analysisFps in 0.01..LOW_FPS_THRESHOLD) add(QualityStatus.LowFps)
            if (!sample.timestampMonotonic) add(QualityStatus.TimingUnstable)
            if (lightingFlickerLikely) add(QualityStatus.LightingFlicker)
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
        private const val LOW_FPS_THRESHOLD = 24.0
        private const val WEAK_SIGNAL_THRESHOLD = 0.015
        private const val CAMERA_MOTION_THRESHOLD = 0.018f
        private const val HIGH_FREQUENCY_MOTION_THRESHOLD = 0.008f
        private const val HIGH_FREQUENCY_AMPLIFICATION_THRESHOLD = 18.0f
    }
}

private val MagnificationMode.isHighFrequencyMode: Boolean
    get() = this == MagnificationMode.Tremor || this == MagnificationMode.ObjectVibration

enum class QualityStatus(val label: String) {
    Good("Good"),
    FaceMissing("Face missing"),
    TooDark("Too dark"),
    LowFps("Low FPS"),
    TimingUnstable("Timing unstable"),
    LightingFlicker("Lighting flicker"),
    CameraMotion("ROI motion"),
    ModeMotionRisk("Mode motion risk"),
    AmplificationRisk("Amplification risk"),
    SignalWeak("Signal weak"),
}
