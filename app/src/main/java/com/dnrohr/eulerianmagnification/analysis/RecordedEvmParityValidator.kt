package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.abs

data class RecordedEvmParityExpectation(
    val minMeanAbsDelta: Double = 0.0,
    val maxMeanAbsDelta: Double? = null,
    val minChangedPixelFraction: Double = 0.0,
    val maxClippedPixelFraction: Double = 0.05,
)

data class RecordedEvmParityReport(
    val sampleName: String,
    val frameCount: Int,
    val changedFrameCount: Int,
    val meanAbsDelta: Double,
    val maxAbsDelta: Double,
    val changedPixelFraction: Double,
    val clippedPixelFraction: Double,
    val failureReasons: List<String>,
) {
    val passed: Boolean get() = failureReasons.isEmpty()

    fun summary(): String {
        val status = if (passed) "PASS" else "FAIL"
        return "$status $sampleName: frames=$frameCount, changedFrames=$changedFrameCount, " +
            "meanAbsDelta=${meanAbsDelta.formatMetric()}, maxAbsDelta=${maxAbsDelta.formatMetric()}, " +
            "changedPixels=${changedPixelFraction.formatMetric()}, clippedPixels=${clippedPixelFraction.formatMetric()}"
    }
}

class RecordedEvmParityValidator(
    private val rendererFactory: (AnalysisSettings) -> FullFrameLinearEvmRenderer = { settings ->
        FullFrameLinearEvmRenderer(settings)
    },
) {
    fun validate(
        sampleName: String,
        settings: AnalysisSettings,
        frames: List<RgbFrame>,
        expectation: RecordedEvmParityExpectation,
    ): RecordedEvmParityReport {
        require(frames.isNotEmpty()) { "frames must not be empty" }
        val renderer = rendererFactory(settings)
        var changedFrameCount = 0
        var deltaSum = 0.0
        var maxDelta = 0.0
        var changedPixels = 0L
        var clippedPixels = 0L
        var totalPixels = 0L

        frames.forEach { input ->
            val output = renderer.render(input)
            require(output.width == input.width && output.height == input.height) {
                "renderer must preserve frame dimensions"
            }
            var frameChanged = false
            input.pixels.forEachIndexed { index, inputPixel ->
                val outputPixel = output.pixels[index]
                val delta = meanChannelDelta(inputPixel, outputPixel)
                deltaSum += delta
                maxDelta = maxOf(maxDelta, delta)
                if (delta > 0.0) {
                    changedPixels++
                    frameChanged = true
                }
                if (isNewlyClipped(inputPixel, outputPixel)) {
                    clippedPixels++
                }
                totalPixels++
            }
            if (frameChanged) {
                changedFrameCount++
            }
        }

        val meanAbsDelta = deltaSum / totalPixels.toDouble()
        val changedPixelFraction = changedPixels / totalPixels.toDouble()
        val clippedPixelFraction = clippedPixels / totalPixels.toDouble()
        val failures = buildList {
            if (meanAbsDelta < expectation.minMeanAbsDelta) {
                add("meanAbsDelta ${meanAbsDelta.formatMetric()} below ${expectation.minMeanAbsDelta.formatMetric()}")
            }
            val maxAllowedMean = expectation.maxMeanAbsDelta
            if (maxAllowedMean != null && meanAbsDelta > maxAllowedMean) {
                add("meanAbsDelta ${meanAbsDelta.formatMetric()} above ${maxAllowedMean.formatMetric()}")
            }
            if (changedPixelFraction < expectation.minChangedPixelFraction) {
                add(
                    "changedPixelFraction ${changedPixelFraction.formatMetric()} below " +
                        expectation.minChangedPixelFraction.formatMetric(),
                )
            }
            if (clippedPixelFraction > expectation.maxClippedPixelFraction) {
                add(
                    "clippedPixelFraction ${clippedPixelFraction.formatMetric()} above " +
                        expectation.maxClippedPixelFraction.formatMetric(),
                )
            }
        }

        return RecordedEvmParityReport(
            sampleName = sampleName,
            frameCount = frames.size,
            changedFrameCount = changedFrameCount,
            meanAbsDelta = meanAbsDelta,
            maxAbsDelta = maxDelta,
            changedPixelFraction = changedPixelFraction,
            clippedPixelFraction = clippedPixelFraction,
            failureReasons = failures,
        )
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

private fun Double.formatMetric(): String = "%.6f".format(this)
