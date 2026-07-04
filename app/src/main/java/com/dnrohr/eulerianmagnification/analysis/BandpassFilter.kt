package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.PI
import kotlin.math.exp

class BandpassFilter(
    private val lowCutHz: Double,
    private val highCutHz: Double,
) {
    init {
        require(lowCutHz > 0.0) { "lowCutHz must be positive" }
        require(highCutHz > lowCutHz) { "highCutHz must be greater than lowCutHz" }
    }

    private val lowCutSmoother = LowPass(lowCutHz)
    private val highCutSmoother = LowPass(highCutHz)
    private var lastTimestampNanos: Long? = null

    fun update(value: Double, timestampNanos: Long): Double {
        val previousTimestamp = lastTimestampNanos
        lastTimestampNanos = timestampNanos

        if (previousTimestamp == null) {
            lowCutSmoother.reset(value)
            highCutSmoother.reset(value)
            return 0.0
        }

        val dtSeconds = (timestampNanos - previousTimestamp).coerceAtLeast(1L) / NANOS_PER_SECOND
        val slow = lowCutSmoother.update(value, dtSeconds)
        val fast = highCutSmoother.update(value, dtSeconds)
        return fast - slow
    }

    private class LowPass(private val cutoffHz: Double) {
        private var state: Double? = null

        fun reset(value: Double) {
            state = value
        }

        fun update(value: Double, dtSeconds: Double): Double {
            val current = state ?: value
            val alpha = 1.0 - exp(-2.0 * PI * cutoffHz * dtSeconds)
            val next = current + alpha * (value - current)
            state = next
            return next
        }
    }

    companion object {
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
