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
        thermalStatus: String = THERMAL_STATUS_NONE,
    ): FullFrameRoiFallbackDecision {
        if (roiSource != RoiSource.FullFrame) {
            return FullFrameRoiFallbackDecision(
                shouldFallbackToAuto = false,
                nextState = FullFrameRoiFallbackState(),
            )
        }
        if (thermalStatus.isThermalCriticalOrWorse()) {
            return FullFrameRoiFallbackDecision(
                shouldFallbackToAuto = true,
                nextState = FullFrameRoiFallbackState(),
                reason = "thermal",
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
    private const val THERMAL_STATUS_NONE = "none"
}

private fun String.isThermalCriticalOrWorse(): Boolean {
    return lowercase() in setOf("critical", "emergency", "shutdown")
}
