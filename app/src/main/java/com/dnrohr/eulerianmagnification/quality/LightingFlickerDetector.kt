package com.dnrohr.eulerianmagnification.quality

import kotlin.math.abs

class LightingFlickerDetector(
    private val windowSize: Int = 18,
    private val deltaThreshold: Double = 2.5,
    private val alternationRatioThreshold: Double = 0.62,
) {
    private val samples = ArrayDeque<Double>()

    fun update(value: Double): Boolean {
        samples.addLast(value)
        while (samples.size > windowSize) {
            samples.removeFirst()
        }
        return isFlickerLikely()
    }

    fun isFlickerLikely(): Boolean {
        if (samples.size < MIN_SAMPLES) return false
        val deltas = samples.zipWithNext { previous, current -> current - previous }
            .filter { abs(it) >= deltaThreshold }
        if (deltas.size < MIN_DELTAS) return false

        val alternations = deltas.zipWithNext().count { (previous, current) ->
            previous.sign() != current.sign()
        }
        val possibleAlternations = deltas.size - 1
        return possibleAlternations > 0 &&
            alternations / possibleAlternations.toDouble() >= alternationRatioThreshold
    }

    private fun Double.sign(): Int = if (this >= 0.0) 1 else -1

    companion object {
        private const val MIN_SAMPLES = 8
        private const val MIN_DELTAS = 6
    }
}
