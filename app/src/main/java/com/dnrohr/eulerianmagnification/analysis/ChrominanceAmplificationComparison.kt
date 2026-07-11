package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

enum class ColorAmplificationStrategy {
    GreenOnly,
    Rgb,
    Chrominance,
    RoiWeightedChrominance,
}

data class ChrominanceAmplificationWeights(
    val roi: NormalizedRect,
    val backgroundGain: Double = DEFAULT_BACKGROUND_GAIN,
) {
    init {
        require(backgroundGain in 0.0..1.0) { "backgroundGain must be in 0..1" }
    }

    fun gainFor(index: Int, width: Int, height: Int): Double {
        val x = index % width
        val y = index / width
        val insideRoi = x >= (roi.left * width).toInt().coerceIn(0, width - 1) &&
            x < (roi.right * width).toInt().coerceIn(1, width) &&
            y >= (roi.top * height).toInt().coerceIn(0, height - 1) &&
            y < (roi.bottom * height).toInt().coerceIn(1, height)
        return if (insideRoi) 1.0 else backgroundGain
    }

    companion object {
        private const val DEFAULT_BACKGROUND_GAIN = 0.12
    }
}

data class ColorAmplificationMetrics(
    val strategy: ColorAmplificationStrategy,
    val targetChromaResponse: Double,
    val backgroundPumping: Double,
    val backgroundLuminanceShift: Double,
) {
    val responseToPumpRatio: Double
        get() = targetChromaResponse / backgroundPumping.coerceAtLeast(0.001)
}

object ChrominanceAmplificationComparison {
    fun compareSyntheticSkinPulse(amplification: Double = DEFAULT_AMPLIFICATION): List<ColorAmplificationMetrics> {
        val base = syntheticSkinFrame(
            targetRed = 188,
            targetGreen = 132,
            targetBlue = 112,
            backgroundRed = 72,
            backgroundGreen = 72,
            backgroundBlue = 72,
            timestampNanos = 0L,
        )
        val pulse = syntheticSkinFrame(
            targetRed = 190,
            targetGreen = 136,
            targetBlue = 114,
            backgroundRed = 75,
            backgroundGreen = 73,
            backgroundBlue = 72,
            timestampNanos = FRAME_NANOS,
        )
        return ColorAmplificationStrategy.entries.map { strategy ->
            val amplified = amplify(
                base = base,
                current = pulse,
                strategy = strategy,
                amplification = amplification,
                weights = ChrominanceAmplificationWeights(TARGET_ROI),
            )
            metrics(
                strategy = strategy,
                base = base,
                current = pulse,
                amplified = amplified,
            )
        }
    }

    fun amplify(
        base: RgbFrame,
        current: RgbFrame,
        strategy: ColorAmplificationStrategy,
        amplification: Double,
        weights: ChrominanceAmplificationWeights = ChrominanceAmplificationWeights(FULL_FRAME_ROI),
    ): RgbFrame {
        require(base.width == current.width && base.height == current.height) {
            "base and current frame dimensions must match"
        }
        require(amplification >= 0.0) { "amplification must be non-negative" }
        val pixels = IntArray(current.pixels.size) { index ->
            when (strategy) {
                ColorAmplificationStrategy.GreenOnly -> amplifyGreen(base.pixels[index], current.pixels[index], amplification)
                ColorAmplificationStrategy.Rgb -> amplifyRgb(base.pixels[index], current.pixels[index], amplification)
                ColorAmplificationStrategy.Chrominance -> amplifyChrominance(
                    base.pixels[index],
                    current.pixels[index],
                    amplification,
                )
                ColorAmplificationStrategy.RoiWeightedChrominance -> amplifyChrominance(
                    base.pixels[index],
                    current.pixels[index],
                    amplification * weights.gainFor(index, current.width, current.height),
                )
            }
        }
        return current.copy(pixels = pixels)
    }

    private fun amplifyGreen(
        base: Int,
        current: Int,
        amplification: Double,
    ): Int {
        return rgb(
            red = red(current),
            green = green(current) + ((green(current) - green(base)) * amplification).roundToInt(),
            blue = blue(current),
        )
    }

    private fun amplifyRgb(
        base: Int,
        current: Int,
        amplification: Double,
    ): Int {
        return rgb(
            red = red(current) + ((red(current) - red(base)) * amplification).roundToInt(),
            green = green(current) + ((green(current) - green(base)) * amplification).roundToInt(),
            blue = blue(current) + ((blue(current) - blue(base)) * amplification).roundToInt(),
        )
    }

    private fun amplifyChrominance(
        base: Int,
        current: Int,
        amplification: Double,
    ): Int {
        val baseYiq = YiqColor.fromRgb(base)
        val currentYiq = YiqColor.fromRgb(current)
        return YiqColor.toRgb(
            luminance = currentYiq.luminance,
            inPhase = currentYiq.inPhase + (currentYiq.inPhase - baseYiq.inPhase) * amplification,
            quadrature = currentYiq.quadrature + (currentYiq.quadrature - baseYiq.quadrature) * amplification,
        )
    }

    private fun metrics(
        strategy: ColorAmplificationStrategy,
        base: RgbFrame,
        current: RgbFrame,
        amplified: RgbFrame,
    ): ColorAmplificationMetrics {
        var targetChroma = 0.0
        var targetCount = 0
        var backgroundPumping = 0.0
        var backgroundLuminance = 0.0
        var backgroundCount = 0
        amplified.pixels.forEachIndexed { index, pixel ->
            val baseYiq = YiqColor.fromRgb(base.pixels[index])
            val currentYiq = YiqColor.fromRgb(current.pixels[index])
            val amplifiedYiq = YiqColor.fromRgb(pixel)
            val targetChromaDelta = hypot(
                amplifiedYiq.inPhase - baseYiq.inPhase,
                amplifiedYiq.quadrature - baseYiq.quadrature,
            )
            val backgroundChromaDelta = hypot(
                amplifiedYiq.inPhase - currentYiq.inPhase,
                amplifiedYiq.quadrature - currentYiq.quadrature,
            )
            val backgroundLuminanceDelta = abs(amplifiedYiq.luminance - currentYiq.luminance)
            if (isTargetIndex(index, amplified.width)) {
                targetChroma += targetChromaDelta
                targetCount++
            } else {
                backgroundPumping += backgroundChromaDelta + backgroundLuminanceDelta
                backgroundLuminance += backgroundLuminanceDelta
                backgroundCount++
            }
        }
        return ColorAmplificationMetrics(
            strategy = strategy,
            targetChromaResponse = targetChroma / targetCount.coerceAtLeast(1),
            backgroundPumping = backgroundPumping / backgroundCount.coerceAtLeast(1),
            backgroundLuminanceShift = backgroundLuminance / backgroundCount.coerceAtLeast(1),
        )
    }

    private fun syntheticSkinFrame(
        targetRed: Int,
        targetGreen: Int,
        targetBlue: Int,
        backgroundRed: Int,
        backgroundGreen: Int,
        backgroundBlue: Int,
        timestampNanos: Long,
    ): RgbFrame {
        return RgbFrame(
            width = WIDTH,
            height = HEIGHT,
            timestampNanos = timestampNanos,
            pixels = IntArray(WIDTH * HEIGHT) { index ->
                if (isTargetIndex(index, WIDTH)) {
                    rgb(targetRed, targetGreen, targetBlue)
                } else {
                    rgb(backgroundRed, backgroundGreen, backgroundBlue)
                }
            },
        )
    }

    private fun isTargetIndex(index: Int, width: Int): Boolean {
        val x = index % width
        val y = index / width
        return x in TARGET_LEFT until TARGET_RIGHT && y in TARGET_TOP until TARGET_BOTTOM
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }

    private fun red(pixel: Int): Int = (pixel shr 16) and 0xFF
    private fun green(pixel: Int): Int = (pixel shr 8) and 0xFF
    private fun blue(pixel: Int): Int = pixel and 0xFF

    private const val WIDTH = 64
    private const val HEIGHT = 48
    private const val TARGET_LEFT = 22
    private const val TARGET_RIGHT = 42
    private const val TARGET_TOP = 14
    private const val TARGET_BOTTOM = 34
    private val TARGET_ROI = NormalizedRect(
        left = TARGET_LEFT / WIDTH.toFloat(),
        top = TARGET_TOP / HEIGHT.toFloat(),
        right = TARGET_RIGHT / WIDTH.toFloat(),
        bottom = TARGET_BOTTOM / HEIGHT.toFloat(),
    )
    private val FULL_FRAME_ROI = NormalizedRect(0.0f, 0.0f, 1.0f, 1.0f)
    private const val FRAME_NANOS = 33_333_333L
    private const val DEFAULT_AMPLIFICATION = 6.0
}
