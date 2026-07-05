package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.abs

data class BreathingMotionSample(
    val bandpassedDy: Double = 0.0,
    val amplifiedDy: Double = 0.0,
) {
    val magnitude: Double get() = abs(amplifiedDy)
}

class BreathingMotionFilter(
    private val amplification: Float,
    lowCutHz: Double = MagnificationMode.Breathing.lowCutHz,
    highCutHz: Double = MagnificationMode.Breathing.highCutHz,
) {
    private val verticalFilter = BandpassFilter(lowCutHz, highCutHz)

    init {
        require(amplification >= 0.0f) { "amplification must be non-negative" }
    }

    fun update(
        translation: TranslationEstimate,
        timestampNanos: Long,
    ): BreathingMotionSample {
        val bandpassed = verticalFilter.update(translation.dy.toDouble(), timestampNanos)
        return BreathingMotionSample(
            bandpassedDy = bandpassed,
            amplifiedDy = bandpassed * amplification,
        )
    }
}
