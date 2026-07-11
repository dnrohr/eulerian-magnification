package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.hypot
import kotlin.math.roundToInt

data class ColorOutputClampConfig(
    val maxChannelDelta: Int = 80,
    val maxChromaDelta: Double = 0.20,
    val fullFramePulseChangedFraction: Double = 0.85,
    val fullFramePulseMeanDelta: Double = 0.10,
    val fullFramePulseGain: Double = 0.45,
)

class ColorOutputClamp(
    private val config: ColorOutputClampConfig = ColorOutputClampConfig(),
) {
    fun clampFrame(
        base: RgbFrame,
        candidate: RgbFrame,
    ): RgbFrame {
        require(base.width == candidate.width && base.height == candidate.height) {
            "base and candidate frame dimensions must match"
        }
        val pixels = IntArray(candidate.pixels.size) { index ->
            clampPixel(base.pixels[index], candidate.pixels[index])
        }
        val pulseGain = fullFramePulseGain(base.pixels, pixels)
        val output = if (pulseGain < 1.0) {
            IntArray(pixels.size) { index ->
                lerpPixel(base.pixels[index], pixels[index], pulseGain)
            }
        } else {
            pixels
        }
        return candidate.copy(pixels = output)
    }

    fun clampPixel(
        basePixel: Int,
        candidatePixel: Int,
    ): Int {
        val channelClamped = rgb(
            red = clampChannel(base = red(basePixel), candidate = red(candidatePixel)),
            green = clampChannel(base = green(basePixel), candidate = green(candidatePixel)),
            blue = clampChannel(base = blue(basePixel), candidate = blue(candidatePixel)),
        )
        return clampChroma(basePixel, channelClamped)
    }

    private fun clampChroma(
        basePixel: Int,
        candidatePixel: Int,
    ): Int {
        val base = YiqColor.fromRgb(basePixel)
        val candidate = YiqColor.fromRgb(candidatePixel)
        val inPhaseDelta = candidate.inPhase - base.inPhase
        val quadratureDelta = candidate.quadrature - base.quadrature
        val chromaDelta = hypot(inPhaseDelta, quadratureDelta)
        if (chromaDelta <= config.maxChromaDelta) return candidatePixel

        val scale = config.maxChromaDelta / chromaDelta
        return rgbWithHeadroom(
            basePixel = basePixel,
            pixel = YiqColor.toRgb(
                luminance = candidate.luminance,
                inPhase = base.inPhase + inPhaseDelta * scale,
                quadrature = base.quadrature + quadratureDelta * scale,
            ),
        )
    }

    private fun fullFramePulseGain(
        basePixels: IntArray,
        candidatePixels: IntArray,
    ): Double {
        var changed = 0
        var totalDelta = 0.0
        basePixels.indices.forEach { index ->
            val delta = channelDelta(basePixels[index], candidatePixels[index])
            if (delta > CHANGED_PIXEL_THRESHOLD) changed++
            totalDelta += delta
        }
        val changedFraction = changed / basePixels.size.coerceAtLeast(1).toDouble()
        val meanDelta = totalDelta / (basePixels.size.coerceAtLeast(1) * CHANNELS * CHANNEL_MAX)
        return if (
            changedFraction >= config.fullFramePulseChangedFraction &&
            meanDelta >= config.fullFramePulseMeanDelta
        ) {
            config.fullFramePulseGain
        } else {
            1.0
        }
    }

    private fun channelDelta(
        basePixel: Int,
        candidatePixel: Int,
    ): Int {
        return kotlin.math.abs(red(candidatePixel) - red(basePixel)) +
            kotlin.math.abs(green(candidatePixel) - green(basePixel)) +
            kotlin.math.abs(blue(candidatePixel) - blue(basePixel))
    }

    private fun lerpPixel(
        basePixel: Int,
        candidatePixel: Int,
        gain: Double,
    ): Int {
        return rgbWithHeadroom(
            basePixel = basePixel,
            red = red(basePixel) + ((red(candidatePixel) - red(basePixel)) * gain).roundToInt(),
            green = green(basePixel) + ((green(candidatePixel) - green(basePixel)) * gain).roundToInt(),
            blue = blue(basePixel) + ((blue(candidatePixel) - blue(basePixel)) * gain).roundToInt(),
        )
    }

    private fun clampChannel(
        base: Int,
        candidate: Int,
    ): Int {
        val delta = (candidate - base).coerceIn(-config.maxChannelDelta, config.maxChannelDelta)
        return displayHeadroom(base = base, value = base + delta)
    }

    private fun rgbWithHeadroom(
        basePixel: Int,
        pixel: Int,
    ): Int {
        return rgbWithHeadroom(
            basePixel = basePixel,
            red = red(pixel),
            green = green(pixel),
            blue = blue(pixel),
        )
    }

    private fun rgbWithHeadroom(
        basePixel: Int,
        red: Int,
        green: Int,
        blue: Int,
    ): Int {
        return rgb(
            red = displayHeadroom(base = red(basePixel), value = red),
            green = displayHeadroom(base = green(basePixel), value = green),
            blue = displayHeadroom(base = blue(basePixel), value = blue),
        )
    }

    private fun displayHeadroom(
        base: Int,
        value: Int,
    ): Int {
        val bounded = value.coerceIn(0, 255)
        return if (bounded == base) {
            bounded
        } else {
            bounded.coerceIn(DISPLAY_LOW_HEADROOM, DISPLAY_HIGH_HEADROOM)
        }
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }

    private fun red(pixel: Int): Int = (pixel shr 16) and 0xFF
    private fun green(pixel: Int): Int = (pixel shr 8) and 0xFF
    private fun blue(pixel: Int): Int = pixel and 0xFF

    companion object {
        private const val CHANNELS = 3.0
        private const val CHANNEL_MAX = 255.0
        private const val CHANGED_PIXEL_THRESHOLD = 3
        private const val DISPLAY_LOW_HEADROOM = 4
        private const val DISPLAY_HIGH_HEADROOM = 251
    }
}
