package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.RoiSource

data class FullFrameRoiFallbackState(
    val lowFpsSampleCount: Int = 0,
)

data class FullFrameRoiFallbackDecision(
    val shouldFallbackToAuto: Boolean,
    val nextState: FullFrameRoiFallbackState,
    val reason: String? = null,
)

object FullFrameRoiFallbackPolicy {
    fun observe(
        roiSource: RoiSource,
        analysisFps: Double,
        state: FullFrameRoiFallbackState,
        cameraFrameFps: Double? = null,
        cameraFrameSampleCount: Int = 0,
        thermalStatus: String = THERMAL_STATUS_NONE,
    ): FullFrameRoiFallbackDecision {
        if (roiSource != RoiSource.FullFrame) {
            return FullFrameRoiFallbackDecision(
                shouldFallbackToAuto = false,
                nextState = FullFrameRoiFallbackState(),
            )
        }
        if (thermalStatus.isThermalSevereOrWorse()) {
            return FullFrameRoiFallbackDecision(
                shouldFallbackToAuto = true,
                nextState = FullFrameRoiFallbackState(),
                reason = "thermal",
            )
        }

        val cameraFpsIsSettledLow = cameraFrameSampleCount >= MIN_CAMERA_FRAME_SAMPLES &&
            cameraFrameFps != null &&
            cameraFrameFps in 0.01..<MIN_ANALYSIS_FPS
        if (cameraFpsIsSettledLow) {
            return FullFrameRoiFallbackDecision(
                shouldFallbackToAuto = true,
                nextState = FullFrameRoiFallbackState(),
                reason = "camera_fps",
            )
        }

        if (analysisFps <= 0.0) {
            return FullFrameRoiFallbackDecision(
                shouldFallbackToAuto = false,
                nextState = FullFrameRoiFallbackState(),
            )
        }

        val nextLowFpsSampleCount = if (analysisFps < MIN_ANALYSIS_FPS) {
            state.lowFpsSampleCount + 1
        } else {
            0
        }

        return FullFrameRoiFallbackDecision(
            shouldFallbackToAuto = nextLowFpsSampleCount >= MIN_LOW_FPS_SAMPLES,
            nextState = FullFrameRoiFallbackState(nextLowFpsSampleCount),
            reason = if (nextLowFpsSampleCount >= MIN_LOW_FPS_SAMPLES) "low_fps" else null,
        )
    }

    private const val MIN_ANALYSIS_FPS = 23.5
    private const val MIN_LOW_FPS_SAMPLES = 5
    private const val MIN_CAMERA_FRAME_SAMPLES = 10
    private const val THERMAL_STATUS_NONE = "none"
}

private fun String.isThermalSevereOrWorse(): Boolean {
    return lowercase() in setOf("severe", "critical", "emergency", "shutdown")
}
