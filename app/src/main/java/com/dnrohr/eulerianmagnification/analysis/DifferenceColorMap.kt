package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.abs

object DifferenceColorMap {
    fun signalColor(signal: Double): Int {
        val strength = abs(signal).coerceIn(0.0, 1.0)
        val base = 16
        val positive = signal >= 0.0
        val red = if (positive) base + strength * 239.0 else base + strength * 12.0
        val green = if (positive) base + strength * 72.0 else base + strength * 110.0
        val blue = if (positive) base + strength * 8.0 else base + strength * 239.0
        return argb(red.toInt(), green.toInt(), blue.toInt())
    }

    fun dimContext(pixel: Int): Int {
        return argb(
            red = (red(pixel) * CONTEXT_SCALE).toInt(),
            green = (green(pixel) * CONTEXT_SCALE).toInt(),
            blue = (blue(pixel) * CONTEXT_SCALE).toInt(),
        )
    }

    private fun red(pixel: Int): Int = (pixel shr 16) and 0xFF
    private fun green(pixel: Int): Int = (pixel shr 8) and 0xFF
    private fun blue(pixel: Int): Int = pixel and 0xFF

    private fun argb(
        red: Int,
        green: Int,
        blue: Int,
    ): Int = (0xFF shl 24) or
        (red.coerceIn(0, 255) shl 16) or
        (green.coerceIn(0, 255) shl 8) or
        blue.coerceIn(0, 255)

    private const val CONTEXT_SCALE = 0.18
}
