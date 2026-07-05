package com.dnrohr.eulerianmagnification.analysis

import com.dnrohr.eulerianmagnification.profiling.FpsMeter

data class RgbFrame(
    val width: Int,
    val height: Int,
    val timestampNanos: Long,
    val pixels: IntArray,
) {
    init {
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
        require(pixels.size == width * height) { "pixels must match width * height" }
    }
}

class RecordedVideoAnalyzer(
    settings: AnalysisSettings,
    private val roi: NormalizedRect = DEFAULT_ROI,
) {
    private val fpsMeter = FpsMeter()
    private val bandpassFilter = BandpassFilter(
        lowCutHz = settings.lowCutHz,
        highCutHz = settings.highCutHz,
    )
    private val timestampTracker = TimestampTracker()

    fun analyze(frame: RgbFrame): AnalysisSample {
        val timestampStatus = timestampTracker.record(frame.timestampNanos)
        fpsMeter.recordFrame(frame.timestampNanos)
        val averageGreen = averageGreen(frame)
        return AnalysisSample(
            analysisFps = fpsMeter.framesPerSecond(),
            roi = roi,
            averageGreen = averageGreen,
            bandpassedGreen = bandpassFilter.update(averageGreen, frame.timestampNanos),
            latencyMillis = 0.0,
            timestampMonotonic = timestampStatus.isMonotonic,
            frameTimestampNanos = frame.timestampNanos,
            frameWidth = frame.width,
            frameHeight = frame.height,
        )
    }

    private fun averageGreen(frame: RgbFrame): Double {
        val left = (roi.left * frame.width).toInt().coerceIn(0, frame.width - 1)
        val top = (roi.top * frame.height).toInt().coerceIn(0, frame.height - 1)
        val right = (roi.right * frame.width).toInt().coerceIn(left + 1, frame.width)
        val bottom = (roi.bottom * frame.height).toInt().coerceIn(top + 1, frame.height)
        val stepX = ((right - left) / SAMPLE_GRID).coerceAtLeast(1)
        val stepY = ((bottom - top) / SAMPLE_GRID).coerceAtLeast(1)
        var sum = 0.0
        var count = 0

        var y = top
        while (y < bottom) {
            var x = left
            while (x < right) {
                val pixel = frame.pixels[y * frame.width + x]
                sum += (pixel shr 8) and 0xFF
                count++
                x += stepX
            }
            y += stepY
        }

        return if (count == 0) 0.0 else sum / count
    }

    companion object {
        private const val SAMPLE_GRID = 16
        val DEFAULT_ROI = NormalizedRect(
            left = 0.36f,
            top = 0.32f,
            right = 0.64f,
            bottom = 0.52f,
        )
    }
}
