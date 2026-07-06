package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.abs

class RoiSmoother(
    private val smoothing: Float = 0.78f,
    private val jumpResetThreshold: Float = 0.28f,
) {
    private var current: NormalizedRect? = null

    fun update(next: NormalizedRect): NormalizedRect {
        val previous = current
        val smoothed = if (previous == null || previous.distanceTo(next) > jumpResetThreshold) {
            next
        } else {
            previous.lerp(next, 1.0f - smoothing)
        }
        current = smoothed
        return smoothed
    }

    fun reset() {
        current = null
    }

    private fun NormalizedRect.distanceTo(other: NormalizedRect): Float {
        return abs(left - other.left) +
            abs(top - other.top) +
            abs(right - other.right) +
            abs(bottom - other.bottom)
    }

    private fun NormalizedRect.lerp(other: NormalizedRect, amount: Float): NormalizedRect {
        return NormalizedRect(
            left = left + (other.left - left) * amount,
            top = top + (other.top - top) * amount,
            right = right + (other.right - right) * amount,
            bottom = bottom + (other.bottom - bottom) * amount,
        )
    }
}
