package com.dnrohr.eulerianmagnification.analysis

object ManualRoiSelector {
    fun fromDrag(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        width: Float,
        height: Float,
    ): NormalizedRect? {
        if (width <= 0.0f || height <= 0.0f) return null
        val left = minOf(startX, endX).coerceIn(0.0f, width) / width
        val right = maxOf(startX, endX).coerceIn(0.0f, width) / width
        val top = minOf(startY, endY).coerceIn(0.0f, height) / height
        val bottom = maxOf(startY, endY).coerceIn(0.0f, height) / height
        if (right - left < MIN_SIZE || bottom - top < MIN_SIZE) return null
        return NormalizedRect(left, top, right, bottom)
    }

    private const val MIN_SIZE = 0.02f
}
