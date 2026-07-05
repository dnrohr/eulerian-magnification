package com.dnrohr.eulerianmagnification.analysis

data class AnalysisSample(
    val analysisFps: Double = 0.0,
    val roi: NormalizedRect? = null,
    val averageGreen: Double = 0.0,
    val bandpassedGreen: Double = 0.0,
    val latencyMillis: Double = 0.0,
    val timestampMonotonic: Boolean = true,
    val translation: TranslationEstimate = TranslationEstimate(),
    val frameTimestampNanos: Long = 0L,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val rotationDegrees: Int = 0,
)

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}
