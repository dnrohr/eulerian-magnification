package com.dnrohr.eulerianmagnification.analysis

data class YuvColorSample(
    val red: Double,
    val green: Double,
    val blue: Double,
) {
    val saturated: Boolean
        get() = red <= SATURATION_LOW ||
            green <= SATURATION_LOW ||
            blue <= SATURATION_LOW ||
            red >= SATURATION_HIGH ||
            green >= SATURATION_HIGH ||
            blue >= SATURATION_HIGH

    companion object {
        fun fromYuv(
            y: Double,
            u: Double,
            v: Double,
        ): YuvColorSample {
            val shiftedU = u - CHROMA_CENTER
            val shiftedV = v - CHROMA_CENTER
            return YuvColorSample(
                red = (y + 1.402 * shiftedV).coerceIn(0.0, CHANNEL_MAX),
                green = (y - 0.344136 * shiftedU - 0.714136 * shiftedV).coerceIn(0.0, CHANNEL_MAX),
                blue = (y + 1.772 * shiftedU).coerceIn(0.0, CHANNEL_MAX),
            )
        }

        private const val CHROMA_CENTER = 128.0
        private const val CHANNEL_MAX = 255.0
        private const val SATURATION_LOW = 4.0
        private const val SATURATION_HIGH = 251.0
    }
}
