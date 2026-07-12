package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.RoiSource

data class FullFrameRoiFallbackState(
    val lowFpsSampleCount: Int = 0,
)

data class FullFrameRoiFallbackDecision(
    val shouldFallbackToAuto: Boolean,
    val nextState: FullFrameRoiFallbackState,
)

object FullFrameRoiFallbackPolicy {
    fun observe(
        roiSource: RoiSource,
        analysisFps: Double,
        state: FullFrameRoiFallbackState,
    ): FullFrameRoiFallbackDecision {
        if (roiSource != RoiSource.FullFrame || analysisFps <= 0.0) {
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
        )
    }

    private const val MIN_ANALYSIS_FPS = 23.5
    private const val MIN_LOW_FPS_SAMPLES = 5
}
