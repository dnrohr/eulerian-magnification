package com.dnrohr.eulerianmagnification.analysis

import com.dnrohr.eulerianmagnification.quality.ArtifactSuppressor
import kotlin.math.abs

data class RecordedVideoProcessedFrame(
    val frame: RgbFrame,
    val sample: AnalysisSample,
)

data class RecordedVideoProcessingResult(
    val sourceFrameCount: Int,
    val processedFrames: List<RecordedVideoProcessedFrame>,
) {
    val hasFrames: Boolean get() = processedFrames.isNotEmpty()
}

class RecordedVideoProcessor(
    private val settings: AnalysisSettings,
    private val roi: NormalizedRect = RecordedVideoAnalyzer.DEFAULT_ROI,
    private val artifactSuppressor: ArtifactSuppressor = ArtifactSuppressor(),
) {
    fun process(frames: Iterable<RgbFrame>): RecordedVideoProcessingResult {
        val analyzer = RecordedVideoAnalyzer(settings, roi)
        val processed = mutableListOf<RecordedVideoProcessedFrame>()
        var sourceFrameCount = 0
        frames.forEach { frame ->
            sourceFrameCount++
            val sample = analyzer.analyze(frame)
            processed += RecordedVideoProcessedFrame(
                frame = render(frame, sample),
                sample = sample,
            )
        }
        return RecordedVideoProcessingResult(
            sourceFrameCount = sourceFrameCount,
            processedFrames = processed,
        )
    }

    private fun render(
        frame: RgbFrame,
        sample: AnalysisSample,
    ): RgbFrame {
        val processed = when (settings.viewMode) {
            ViewMode.Raw -> frame.copy(pixels = frame.pixels.copyOf())
            ViewMode.Amplified -> amplified(frame, sample)
            ViewMode.Difference -> difference(frame, sample)
            ViewMode.Split -> sideBySide(frame, amplified(frame, sample))
        }
        return processed
    }

    private fun amplified(
        frame: RgbFrame,
        sample: AnalysisSample,
    ): RgbFrame {
        val pixels = frame.pixels.copyOf()
        val signal = normalizedSignal(sample)
        forEachRoiPixel(frame) { index ->
            pixels[index] = addDelta(frame.pixels[index], signal)
        }
        return frame.copy(pixels = pixels)
    }

    private fun difference(
        frame: RgbFrame,
        sample: AnalysisSample,
    ): RgbFrame {
        val pixels = IntArray(frame.pixels.size) { 0xFF000000.toInt() }
        val signal = abs(normalizedSignal(sample))
        val red = (signal * 255.0).toInt().coerceIn(0, 255)
        val green = (signal * 140.0).toInt().coerceIn(0, 255)
        val blue = (signal * 90.0).toInt().coerceIn(0, 255)
        val color = argb(red, green, blue)
        forEachRoiPixel(frame) { index ->
            pixels[index] = color
        }
        return frame.copy(pixels = pixels)
    }

    private fun sideBySide(
        raw: RgbFrame,
        processed: RgbFrame,
    ): RgbFrame {
        val outputPixels = IntArray(raw.width * 2 * raw.height)
        for (y in 0 until raw.height) {
            val rawRow = y * raw.width
            val outputRow = y * raw.width * 2
            for (x in 0 until raw.width) {
                outputPixels[outputRow + x] = raw.pixels[rawRow + x]
                outputPixels[outputRow + raw.width + x] = processed.pixels[rawRow + x]
            }
        }
        return RgbFrame(
            width = raw.width * 2,
            height = raw.height,
            timestampNanos = raw.timestampNanos,
            pixels = outputPixels,
        )
    }

    private fun normalizedSignal(sample: AnalysisSample): Double {
        if (settings.viewMode == ViewMode.Raw) return 0.0
        return artifactSuppressor
            .amplify(sample.bandpassedGreen, settings.amplification)
            .value
            .div(ArtifactSuppressor.DEFAULT_MAX_AMPLIFIED_MAGNITUDE)
            .coerceIn(-1.0, 1.0)
    }

    private fun addDelta(pixel: Int, signal: Double): Int {
        val red = red(pixel) + signal * 255.0
        val green = green(pixel) + signal * 255.0 * 0.55
        val blue = blue(pixel) + signal * 255.0 * 0.35
        return argb(
            red = red.toInt().coerceIn(0, 255),
            green = green.toInt().coerceIn(0, 255),
            blue = blue.toInt().coerceIn(0, 255),
        )
    }

    private fun forEachRoiPixel(
        frame: RgbFrame,
        block: (Int) -> Unit,
    ) {
        val left = (roi.left * frame.width).toInt().coerceIn(0, frame.width - 1)
        val top = (roi.top * frame.height).toInt().coerceIn(0, frame.height - 1)
        val right = (roi.right * frame.width).toInt().coerceIn(left + 1, frame.width)
        val bottom = (roi.bottom * frame.height).toInt().coerceIn(top + 1, frame.height)
        for (y in top until bottom) {
            for (x in left until right) {
                block(y * frame.width + x)
            }
        }
    }

    private fun red(pixel: Int): Int = (pixel shr 16) and 0xFF
    private fun green(pixel: Int): Int = (pixel shr 8) and 0xFF
    private fun blue(pixel: Int): Int = pixel and 0xFF

    private fun argb(
        red: Int,
        green: Int,
        blue: Int,
    ): Int = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}
