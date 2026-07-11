package com.dnrohr.eulerianmagnification.analysis

import com.dnrohr.eulerianmagnification.quality.ArtifactSuppressor
import com.dnrohr.eulerianmagnification.quality.LightingStabilityAnalyzer

data class RecordedVideoProcessedFrame(
    val frame: RgbFrame,
    val sample: AnalysisSample,
    val colorGate: ColorAmplificationGateResult = ColorAmplificationGateResult.Stable,
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
    private val colorAmplificationGate: ColorAmplificationGate = ColorAmplificationGate(),
) {
    fun process(frames: Iterable<RgbFrame>): RecordedVideoProcessingResult {
        val analyzer = RecordedVideoAnalyzer(settings, roi)
        val lightingAnalyzer = LightingStabilityAnalyzer()
        val linearRenderer = FullFrameLinearEvmRenderer(settings)
        val phaseMotionRenderer = RieszPhaseMotionRenderer(settings)
        val processed = mutableListOf<RecordedVideoProcessedFrame>()
        var sourceFrameCount = 0
        frames.forEach { frame ->
            sourceFrameCount++
            val sample = analyzer.analyze(frame)
            val lightingDiagnostic = lightingAnalyzer.update(sample)
            val colorGate = colorAmplificationGate.evaluate(
                mode = settings.mode,
                lighting = lightingDiagnostic,
                saturatedPixelFraction = saturatedPixelFraction(frame),
            )
            val fullFrameEvm = if (settings.viewMode == ViewMode.Amplified || settings.viewMode == ViewMode.Split) {
                if (settings.mode == MagnificationMode.Pulse) {
                    linearRenderer.render(
                        frame = frame,
                        amplification = settings.amplification * colorGate.effectiveAmplificationScale,
                    )
                } else {
                    phaseMotionRenderer.render(frame)
                }
            } else {
                frame
            }
            processed += RecordedVideoProcessedFrame(
                frame = render(frame, sample, fullFrameEvm),
                sample = sample,
                colorGate = colorGate,
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

    private fun saturatedPixelFraction(frame: RgbFrame): Double {
        var saturated = 0
        var total = 0
        forEachRoiPixel(frame) { index ->
            total++
            val pixel = frame.pixels[index]
            if (red(pixel) <= SATURATION_LOW || red(pixel) >= SATURATION_HIGH ||
                green(pixel) <= SATURATION_LOW || green(pixel) >= SATURATION_HIGH ||
                blue(pixel) <= SATURATION_LOW || blue(pixel) >= SATURATION_HIGH
            ) {
                saturated++
            }
        }
        return if (total == 0) 0.0 else saturated / total.toDouble()
    }

    private fun red(pixel: Int): Int = (pixel shr 16) and 0xFF
    private fun green(pixel: Int): Int = (pixel shr 8) and 0xFF
    private fun blue(pixel: Int): Int = pixel and 0xFF

    companion object {
        private const val SATURATION_LOW = 2
        private const val SATURATION_HIGH = 253
    }
}
