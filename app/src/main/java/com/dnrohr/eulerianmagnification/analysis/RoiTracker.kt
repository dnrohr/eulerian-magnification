package com.dnrohr.eulerianmagnification.analysis

class RoiTracker(
    private val damping: Float = 0.72f,
    private val maxStep: Float = 0.035f,
) {
    private var current: NormalizedRect? = null
    private var velocityX = 0.0f
    private var velocityY = 0.0f

    fun updateDetection(detected: NormalizedRect): NormalizedRect {
        val previous = current
        if (previous != null) {
            velocityX = ((detected.centerX - previous.centerX) * damping).coerceIn(-maxStep, maxStep)
            velocityY = ((detected.centerY - previous.centerY) * damping).coerceIn(-maxStep, maxStep)
        }
        current = detected.clamped()
        return current ?: detected
    }

    fun predict(): NormalizedRect? {
        val previous = current ?: return null
        if (velocityX == 0.0f && velocityY == 0.0f) return previous

        val predicted = previous.translated(velocityX, velocityY).clamped()
        current = predicted
        velocityX *= damping
        velocityY *= damping
        return predicted
    }

    private val NormalizedRect.centerX: Float get() = (left + right) / 2.0f
    private val NormalizedRect.centerY: Float get() = (top + bottom) / 2.0f

    private fun NormalizedRect.translated(dx: Float, dy: Float): NormalizedRect {
        return NormalizedRect(
            left = left + dx,
            top = top + dy,
            right = right + dx,
            bottom = bottom + dy,
        )
    }

    private fun NormalizedRect.clamped(): NormalizedRect {
        val width = width.coerceIn(0.01f, 1.0f)
        val height = height.coerceIn(0.01f, 1.0f)
        val left = left.coerceIn(0.0f, 1.0f - width)
        val top = top.coerceIn(0.0f, 1.0f - height)
        return NormalizedRect(
            left = left,
            top = top,
            right = left + width,
            bottom = top + height,
        )
    }
}
