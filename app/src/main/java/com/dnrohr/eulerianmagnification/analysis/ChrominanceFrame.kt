package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.roundToInt

data class ChrominanceFrame(
    val width: Int,
    val height: Int,
    val timestampNanos: Long,
    val luminance: DoubleArray,
    val inPhase: DoubleArray,
    val quadrature: DoubleArray,
) {
    init {
        val expectedSize = width * height
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
        require(luminance.size == expectedSize) { "luminance must match width * height" }
        require(inPhase.size == expectedSize) { "inPhase must match width * height" }
        require(quadrature.size == expectedSize) { "quadrature must match width * height" }
    }

    fun toRgbFrame(): RgbFrame {
        return RgbFrame(
            width = width,
            height = height,
            timestampNanos = timestampNanos,
            pixels = IntArray(luminance.size) { index ->
                YiqColor.toRgb(
                    luminance = luminance[index],
                    inPhase = inPhase[index],
                    quadrature = quadrature[index],
                )
            },
        )
    }

    companion object {
        fun from(frame: RgbFrame): ChrominanceFrame {
            val luminance = DoubleArray(frame.pixels.size)
            val inPhase = DoubleArray(frame.pixels.size)
            val quadrature = DoubleArray(frame.pixels.size)
            frame.pixels.forEachIndexed { index, pixel ->
                val yiq = YiqColor.fromRgb(pixel)
                luminance[index] = yiq.luminance
                inPhase[index] = yiq.inPhase
                quadrature[index] = yiq.quadrature
            }
            return ChrominanceFrame(
                width = frame.width,
                height = frame.height,
                timestampNanos = frame.timestampNanos,
                luminance = luminance,
                inPhase = inPhase,
                quadrature = quadrature,
            )
        }
    }
}

data class YiqColor(
    val luminance: Double,
    val inPhase: Double,
    val quadrature: Double,
) {
    companion object {
        fun fromRgb(pixel: Int): YiqColor {
            val red = ((pixel shr 16) and 0xFF) / CHANNEL_MAX
            val green = ((pixel shr 8) and 0xFF) / CHANNEL_MAX
            val blue = (pixel and 0xFF) / CHANNEL_MAX
            return YiqColor(
                luminance = 0.299 * red + 0.587 * green + 0.114 * blue,
                inPhase = 0.596 * red - 0.274 * green - 0.322 * blue,
                quadrature = 0.211 * red - 0.523 * green + 0.312 * blue,
            )
        }

        fun toRgb(
            luminance: Double,
            inPhase: Double,
            quadrature: Double,
        ): Int {
            val red = luminance + 0.956 * inPhase + 0.621 * quadrature
            val green = luminance - 0.272 * inPhase - 0.647 * quadrature
            val blue = luminance - 1.106 * inPhase + 1.703 * quadrature
            return rgb(
                red = (red * CHANNEL_MAX).roundToInt(),
                green = (green * CHANNEL_MAX).roundToInt(),
                blue = (blue * CHANNEL_MAX).roundToInt(),
            )
        }

        private fun rgb(red: Int, green: Int, blue: Int): Int {
            return (red.coerceIn(0, 255) shl 16) or
                (green.coerceIn(0, 255) shl 8) or
                blue.coerceIn(0, 255)
        }

        private const val CHANNEL_MAX = 255.0
    }
}
