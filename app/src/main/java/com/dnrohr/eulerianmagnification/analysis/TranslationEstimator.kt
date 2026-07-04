package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.hypot

class TranslationEstimator(
    private val smoothing: Float = 0.70f,
) {
    private var previousRoi: NormalizedRect? = null
    private var smoothedDx = 0.0f
    private var smoothedDy = 0.0f

    fun update(roi: NormalizedRect?): TranslationEstimate {
        val previous = previousRoi
        previousRoi = roi
        if (roi == null || previous == null) {
            smoothedDx = 0.0f
            smoothedDy = 0.0f
            return TranslationEstimate()
        }

        val dx = roi.centerX - previous.centerX
        val dy = roi.centerY - previous.centerY
        smoothedDx = smoothedDx * smoothing + dx * (1.0f - smoothing)
        smoothedDy = smoothedDy * smoothing + dy * (1.0f - smoothing)
        return TranslationEstimate(
            dx = smoothedDx,
            dy = smoothedDy,
        )
    }

    private val NormalizedRect.centerX: Float get() = (left + right) / 2.0f
    private val NormalizedRect.centerY: Float get() = (top + bottom) / 2.0f
}

data class TranslationEstimate(
    val dx: Float = 0.0f,
    val dy: Float = 0.0f,
) {
    val magnitude: Float get() = hypot(dx, dy)
}
