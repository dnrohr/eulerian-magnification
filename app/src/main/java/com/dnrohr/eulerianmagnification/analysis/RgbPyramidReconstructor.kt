package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.roundToInt

class RgbPyramidReconstructor(
    private val outputClamp: ColorOutputClamp = ColorOutputClamp(),
) {
    fun reconstruct(
        base: RgbFrame,
        bandpass: RgbPyramidBandpass,
        amplification: Float,
        startLevel: Int = 0,
    ): RgbFrame {
        require(amplification >= 0.0f) { "amplification must be non-negative" }
        require(startLevel >= 0) { "startLevel must be non-negative" }
        if (amplification == 0.0f || bandpass.levels.isEmpty() || startLevel >= bandpass.levels.size) {
            return base.copy(pixels = base.pixels.copyOf())
        }

        val outputPixels = IntArray(base.pixels.size) { index ->
            val basePixel = base.pixels[index]
            var red = (basePixel shr 16) and 0xFF
            var green = (basePixel shr 8) and 0xFF
            var blue = basePixel and 0xFF
            val x = index % base.width
            val y = index / base.width

            for (levelIndex in startLevel until bandpass.levels.size) {
                val level = bandpass.levels[levelIndex]
                val levelX = (x * level.width / base.width).coerceIn(0, level.width - 1)
                val levelY = (y * level.height / base.height).coerceIn(0, level.height - 1)
                val delta = level.pixels[levelY * level.width + levelX]
                red += (RgbPyramidTemporalBandpass.signedRed(delta) * amplification).roundToInt()
                green += (RgbPyramidTemporalBandpass.signedGreen(delta) * amplification).roundToInt()
                blue += (RgbPyramidTemporalBandpass.signedBlue(delta) * amplification).roundToInt()
            }

            rgb(red, green, blue)
        }

        val candidate = RgbFrame(
            width = base.width,
            height = base.height,
            timestampNanos = base.timestampNanos,
            pixels = outputPixels,
        )
        return outputClamp.clampFrame(base, candidate)
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }
}
