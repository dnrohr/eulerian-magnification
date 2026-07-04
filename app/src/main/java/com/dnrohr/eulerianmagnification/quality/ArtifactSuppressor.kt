package com.dnrohr.eulerianmagnification.quality

import kotlin.math.abs

class ArtifactSuppressor(
    private val noiseFloor: Double = DEFAULT_NOISE_FLOOR,
    private val maxAmplifiedMagnitude: Double = DEFAULT_MAX_AMPLIFIED_MAGNITUDE,
) {
    fun amplify(
        bandpassedSignal: Double,
        amplification: Float,
    ): SuppressedSignal {
        val rawAmplified = bandpassedSignal * amplification
        val suppressed = if (abs(bandpassedSignal) < noiseFloor) {
            0.0
        } else {
            rawAmplified.coerceIn(-maxAmplifiedMagnitude, maxAmplifiedMagnitude)
        }
        return SuppressedSignal(
            value = suppressed,
            wasNoiseSuppressed = suppressed == 0.0 && bandpassedSignal != 0.0,
            wasClamped = abs(rawAmplified) > maxAmplifiedMagnitude,
            normalizedMagnitude = (abs(suppressed) / maxAmplifiedMagnitude).coerceIn(0.0, 1.0),
        )
    }

    companion object {
        const val DEFAULT_NOISE_FLOOR = 0.015
        const val DEFAULT_MAX_AMPLIFIED_MAGNITUDE = 64.0
    }
}

data class SuppressedSignal(
    val value: Double,
    val wasNoiseSuppressed: Boolean,
    val wasClamped: Boolean,
    val normalizedMagnitude: Double,
)
