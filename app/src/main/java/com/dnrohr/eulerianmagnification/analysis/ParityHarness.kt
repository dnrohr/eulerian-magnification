package com.dnrohr.eulerianmagnification.analysis

import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

data class ParityHarnessSample(
    val id: String,
    val displayName: String,
    val targetClass: String,
    val frames: List<RgbFrame>,
    val settings: AnalysisSettings,
    val sourcePath: String? = null,
    val sourceSha256: String? = null,
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(frames.isNotEmpty()) { "frames must not be empty" }
    }
}

data class ParityHarnessRun(
    val sample: ParityHarnessSample,
    val viewResults: List<ParityHarnessViewResult>,
) {
    fun manifestJson(): String {
        return buildString {
            appendLine("{")
            appendLine("  \"sampleId\": ${sample.id.quoteJson()},")
            appendLine("  \"displayName\": ${sample.displayName.quoteJson()},")
            appendLine("  \"targetClass\": ${sample.targetClass.quoteJson()},")
            appendLine("  \"sourcePath\": ${sample.sourcePath?.quoteJson() ?: "null"},")
            appendLine("  \"sourceSha256\": ${sample.sourceSha256?.quoteJson() ?: "null"},")
            appendLine("  \"mode\": ${sample.settings.mode.label.quoteJson()},")
            appendLine("  \"viewModeCount\": ${viewResults.size},")
            appendLine("  \"lowCutHz\": ${sample.settings.lowCutHz.formatMetric()},")
            appendLine("  \"highCutHz\": ${sample.settings.highCutHz.formatMetric()},")
            appendLine("  \"amplification\": ${sample.settings.amplification.toDouble().formatMetric()},")
            appendLine("  \"frameCount\": ${sample.frames.size},")
            appendLine("  \"views\": [")
            viewResults.forEachIndexed { index, result ->
                appendLine("    {")
                appendLine("      \"viewMode\": ${result.viewMode.label.quoteJson()},")
                appendLine("      \"renderer\": ${result.rendererKind.id.quoteJson()},")
                appendLine("      \"visualization\": ${result.visualizationStyle.id.quoteJson()},")
                appendLine("      \"outputWidth\": ${result.outputWidth},")
                appendLine("      \"outputHeight\": ${result.outputHeight},")
                appendLine("      \"meanAbsDelta\": ${result.metrics.meanAbsDelta.formatMetric()},")
                appendLine("      \"maxAbsDelta\": ${result.metrics.maxAbsDelta.formatMetric()},")
                appendLine("      \"changedPixelFraction\": ${result.metrics.changedPixelFraction.formatMetric()},")
                appendLine("      \"clippedPixelFraction\": ${result.metrics.clippedPixelFraction.formatMetric()},")
                appendLine("      \"inBandSignalEnergy\": ${result.metrics.inBandSignalEnergy.formatMetric()},")
                appendLine("      \"localizedDifferenceEnergy\": ${result.metrics.localizedDifferenceEnergy.formatMetric()},")
                appendLine("      \"backgroundPumpingScore\": ${result.metrics.backgroundPumpingScore.formatMetric()}")
                append("    }")
                if (index != viewResults.lastIndex) append(',')
                appendLine()
            }
            appendLine("  ]")
            appendLine("}")
        }
    }

    fun htmlReport(): String {
        return buildString {
            appendLine("<!doctype html>")
            appendLine("<html>")
            appendLine("<head>")
            appendLine("  <meta charset=\"utf-8\">")
            appendLine("  <title>Parity Harness Report</title>")
            appendLine("  <style>")
            appendLine("    body { font-family: sans-serif; margin: 24px; background: #101418; color: #eef4f8; }")
            appendLine("    table { border-collapse: collapse; width: 100%; }")
            appendLine("    th, td { border: 1px solid #34424d; padding: 6px 8px; text-align: left; }")
            appendLine("    th { background: #1d2933; }")
            appendLine("  </style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("  <h1>Parity Harness Report</h1>")
            appendLine("  <p>Sample: ${sample.displayName.escapeHtml()} (${sample.id.escapeHtml()})</p>")
            appendLine("  <p>Mode: ${sample.settings.mode.label.escapeHtml()} / ${sample.settings.lowCutHz.formatMetric()}-${sample.settings.highCutHz.formatMetric()} Hz / ${sample.settings.amplification.toDouble().formatMetric()}x</p>")
            appendLine("  <table>")
            appendLine("    <tr><th>View</th><th>Renderer</th><th>Mean delta</th><th>Changed pixels</th><th>Clipping</th><th>Signal energy</th><th>Background pumping</th></tr>")
            viewResults.forEach { result ->
                appendLine(
                    "    <tr><td>${result.viewMode.label.escapeHtml()}</td>" +
                        "<td>${result.rendererKind.label.escapeHtml()}</td>" +
                        "<td>${result.metrics.meanAbsDelta.formatMetric()}</td>" +
                        "<td>${result.metrics.changedPixelFraction.formatMetric()}</td>" +
                        "<td>${result.metrics.clippedPixelFraction.formatMetric()}</td>" +
                        "<td>${result.metrics.inBandSignalEnergy.formatMetric()}</td>" +
                        "<td>${result.metrics.backgroundPumpingScore.formatMetric()}</td></tr>",
                )
            }
            appendLine("  </table>")
            appendLine("</body>")
            appendLine("</html>")
        }
    }
}

data class ParityHarnessViewResult(
    val viewMode: ViewMode,
    val rendererKind: RendererKind,
    val visualizationStyle: VisualizationStyle,
    val outputWidth: Int,
    val outputHeight: Int,
    val processedFrames: List<RecordedVideoProcessedFrame>,
    val metrics: ParityHarnessMetrics,
) {
    val timelineCsv: String get() = RecordedVideoEvidenceTimeline.toCsv(processedFrames)
}

data class ParityHarnessMetrics(
    val meanAbsDelta: Double,
    val maxAbsDelta: Double,
    val changedPixelFraction: Double,
    val clippedPixelFraction: Double,
    val inBandSignalEnergy: Double,
    val localizedDifferenceEnergy: Double,
    val backgroundPumpingScore: Double,
)

class ParityHarness(
    private val roi: NormalizedRect = RecordedVideoAnalyzer.DEFAULT_ROI,
) {
    fun run(
        sample: ParityHarnessSample,
        viewModes: List<ViewMode> = ViewMode.entries,
    ): ParityHarnessRun {
        val results = viewModes.map { viewMode ->
            val settings = sample.settings.copy(viewMode = viewMode)
            val processed = RecordedVideoProcessor(settings, roi).process(sample.frames).processedFrames
            require(processed.isNotEmpty()) { "processor produced no frames for ${sample.id}" }
            val model = VisualizationModel.recorded(settings)
            ParityHarnessViewResult(
                viewMode = viewMode,
                rendererKind = model.renderer,
                visualizationStyle = model.visualizationStyle,
                outputWidth = processed.first().frame.width,
                outputHeight = processed.first().frame.height,
                processedFrames = processed,
                metrics = metrics(sample.frames, processed),
            )
        }
        return ParityHarnessRun(sample = sample, viewResults = results)
    }

    private fun metrics(
        rawFrames: List<RgbFrame>,
        processedFrames: List<RecordedVideoProcessedFrame>,
    ): ParityHarnessMetrics {
        var deltaSum = 0.0
        var maxDelta = 0.0
        var changedPixels = 0L
        var clippedPixels = 0L
        var totalPixels = 0L
        var roiDeltaSum = 0.0
        var roiPixels = 0L
        var backgroundDeltaSum = 0.0
        var backgroundPixels = 0L

        rawFrames.zip(processedFrames).forEach { (raw, processed) ->
            val comparablePixels = comparableProcessedPixels(raw, processed.frame)
            raw.pixels.forEachIndexed { index, rawPixel ->
                val outputPixel = comparablePixels[index]
                val delta = meanChannelDelta(rawPixel, outputPixel)
                deltaSum += delta
                maxDelta = maxOf(maxDelta, delta)
                if (delta > 0.0) changedPixels++
                if (isNewlyClipped(rawPixel, outputPixel)) clippedPixels++
                totalPixels++
                if (index.isInsideRoi(raw)) {
                    roiDeltaSum += delta
                    roiPixels++
                } else {
                    backgroundDeltaSum += delta
                    backgroundPixels++
                }
            }
        }

        val samples = processedFrames.map { it.sample }
        val signalEnergy = samples.sumOf { abs(it.bandpassedGreen) }
        val roiMean = if (roiPixels == 0L) 0.0 else roiDeltaSum / roiPixels.toDouble()
        val backgroundMean = if (backgroundPixels == 0L) 0.0 else backgroundDeltaSum / backgroundPixels.toDouble()
        return ParityHarnessMetrics(
            meanAbsDelta = if (totalPixels == 0L) 0.0 else deltaSum / totalPixels.toDouble(),
            maxAbsDelta = maxDelta,
            changedPixelFraction = if (totalPixels == 0L) 0.0 else changedPixels / totalPixels.toDouble(),
            clippedPixelFraction = if (totalPixels == 0L) 0.0 else clippedPixels / totalPixels.toDouble(),
            inBandSignalEnergy = signalEnergy,
            localizedDifferenceEnergy = roiMean,
            backgroundPumpingScore = backgroundMean,
        )
    }

    private fun comparableProcessedPixels(raw: RgbFrame, processed: RgbFrame): IntArray {
        if (processed.width == raw.width && processed.height == raw.height) {
            return processed.pixels
        }
        if (processed.width == raw.width * 2 && processed.height == raw.height) {
            return IntArray(raw.width * raw.height) { index ->
                val x = index % raw.width
                val y = index / raw.width
                processed.pixels[y * processed.width + raw.width + x]
            }
        }
        error("processed frame dimensions ${processed.width}x${processed.height} are not comparable with raw ${raw.width}x${raw.height}")
    }

    private fun Int.isInsideRoi(frame: RgbFrame): Boolean {
        val x = this % frame.width
        val y = this / frame.width
        val left = (roi.left * frame.width).toInt().coerceIn(0, frame.width - 1)
        val top = (roi.top * frame.height).toInt().coerceIn(0, frame.height - 1)
        val right = (roi.right * frame.width).toInt().coerceIn(left + 1, frame.width)
        val bottom = (roi.bottom * frame.height).toInt().coerceIn(top + 1, frame.height)
        return x in left until right && y in top until bottom
    }

    private fun meanChannelDelta(inputPixel: Int, outputPixel: Int): Double {
        return (
            abs(red(outputPixel) - red(inputPixel)) +
                abs(green(outputPixel) - green(inputPixel)) +
                abs(blue(outputPixel) - blue(inputPixel))
            ) / CHANNELS.toDouble()
    }

    private fun isNewlyClipped(inputPixel: Int, outputPixel: Int): Boolean {
        return isNewlyClippedChannel(red(inputPixel), red(outputPixel)) ||
            isNewlyClippedChannel(green(inputPixel), green(outputPixel)) ||
            isNewlyClippedChannel(blue(inputPixel), blue(outputPixel))
    }

    private fun isNewlyClippedChannel(input: Int, output: Int): Boolean {
        return (output == 0 || output == 255) && input != output
    }

    private fun red(pixel: Int): Int = (pixel shr 16) and 0xFF
    private fun green(pixel: Int): Int = (pixel shr 8) and 0xFF
    private fun blue(pixel: Int): Int = pixel and 0xFF

    private companion object {
        private const val CHANNELS = 3
    }
}

object ParitySyntheticSamples {
    fun colorPulse(
        frameCount: Int = DEFAULT_FRAME_COUNT,
        frequencyHz: Double = 1.2,
    ): ParityHarnessSample {
        return ParityHarnessSample(
            id = "synthetic-color-pulse",
            displayName = "Synthetic Color Pulse",
            targetClass = "Color pulse",
            frames = (0 until frameCount).map { frameIndex ->
                val seconds = frameIndex / FPS
                val green = 128 + (8.0 * sin(2.0 * PI * frequencyHz * seconds)).roundToInt()
                flatFrame(timestampNanos = (seconds * NANOS_PER_SECOND).toLong(), green = green)
            },
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 8.0f,
            ),
        )
    }

    fun movingEdge(
        frameCount: Int = DEFAULT_FRAME_COUNT,
        frequencyHz: Double = 6.0,
    ): ParityHarnessSample {
        return ParityHarnessSample(
            id = "synthetic-moving-edge",
            displayName = "Synthetic Moving Edge",
            targetClass = "Small edge/object motion",
            frames = (0 until frameCount).map { frameIndex ->
                val seconds = frameIndex / FPS
                val edgeOffset = (2.0 * sin(2.0 * PI * frequencyHz * seconds)).roundToInt()
                val edgeX = WIDTH / 2 + edgeOffset
                RgbFrame(
                    width = WIDTH,
                    height = HEIGHT,
                    timestampNanos = (seconds * NANOS_PER_SECOND).toLong(),
                    pixels = IntArray(WIDTH * HEIGHT) { index ->
                        val x = index % WIDTH
                        if (x < edgeX) rgb(48, 48, 48) else rgb(208, 208, 208)
                    },
                )
            },
            settings = AnalysisSettings(
                mode = MagnificationMode.Tremor,
                amplification = 0.5f,
            ),
        )
    }

    private fun flatFrame(timestampNanos: Long, green: Int): RgbFrame {
        return RgbFrame(
            width = WIDTH,
            height = HEIGHT,
            timestampNanos = timestampNanos,
            pixels = IntArray(WIDTH * HEIGHT) { rgb(96, green, 96) },
        )
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }

    private const val WIDTH = 16
    private const val HEIGHT = 12
    private const val FPS = 30.0
    private const val DEFAULT_FRAME_COUNT = 300
    private const val NANOS_PER_SECOND = 1_000_000_000.0
}

private fun Double.formatMetric(): String = String.format(Locale.US, "%.6f", this)

private fun String.quoteJson(): String = "\"${escapeJson()}\""

private fun String.escapeJson(): String {
    return buildString {
        this@escapeJson.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}

private fun String.escapeHtml(): String {
    return replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
