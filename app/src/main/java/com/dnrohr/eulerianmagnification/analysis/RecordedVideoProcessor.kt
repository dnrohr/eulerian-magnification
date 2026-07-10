package com.dnrohr.eulerianmagnification.analysis

import com.dnrohr.eulerianmagnification.quality.ArtifactSuppressor

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
        val fullFrameRenderer = FullFrameLinearEvmRenderer(settings)
        val processed = mutableListOf<RecordedVideoProcessedFrame>()
        var sourceFrameCount = 0
        frames.forEach { frame ->
            sourceFrameCount++
            val sample = analyzer.analyze(frame)
            val fullFrameEvm = if (settings.viewMode == ViewMode.Amplified || settings.viewMode == ViewMode.Split) {
                fullFrameRenderer.render(frame)
            } else {
                frame
            }
            processed += RecordedVideoProcessedFrame(
                frame = render(frame, sample, fullFrameEvm),
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
        fullFrameEvm: RgbFrame,
    ): RgbFrame {
        val processed = when (settings.viewMode) {
            ViewMode.Raw -> frame.copy(pixels = frame.pixels.copyOf())
            ViewMode.Amplified -> fullFrameEvm
            ViewMode.Difference -> difference(frame, sample)
            ViewMode.Split -> sideBySide(frame, fullFrameEvm)
        }
        return processed
    }

    private fun difference(
        frame: RgbFrame,
        sample: AnalysisSample,
    ): RgbFrame {
        val pixels = IntArray(frame.pixels.size) { index ->
            DifferenceColorMap.dimContext(frame.pixels[index])
        }
        val color = DifferenceColorMap.signalColor(normalizedSignal(sample))
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
}
