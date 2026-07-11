package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

class RieszPhaseMotionRenderer(
    private val settings: AnalysisSettings,
    private val amplitudeThreshold: Double = DEFAULT_AMPLITUDE_THRESHOLD,
    private val outputClamp: ColorOutputClamp = ColorOutputClamp(),
) {
    private var state: PhaseState? = null

    fun render(frame: RgbFrame): RgbFrame {
        val luminance = DoubleArray(frame.pixels.size) { index ->
            luminance(frame.pixels[index]) / CHANNEL_MAX
        }
        val rieszX = convolve(frame.width, frame.height, luminance, RIESZ_X)
        val rieszY = convolve(frame.width, frame.height, luminance, RIESZ_Y)
        val orientation = dominantOrientation(rieszX, rieszY)
        val projected = projectPhase(luminance, rieszX, rieszY, orientation)
        val currentState = state

        if (currentState == null || !currentState.matches(frame)) {
            state = PhaseState.from(frame, projected.phase)
            return frame.copy(pixels = frame.pixels.copyOf())
        }

        val dtSeconds = (frame.timestampNanos - currentState.lastTimestampNanos)
            .coerceAtLeast(1L) / NANOS_PER_SECOND
        val lowAlpha = alpha(settings.lowCutHz, dtSeconds)
        val highAlpha = alpha(settings.highCutHz, dtSeconds)
        val outputPixels = IntArray(frame.pixels.size)

        frame.pixels.forEachIndexed { index, pixel ->
            val unwrappedPhase = currentState.unwrappedPhase[index] +
                wrapPhase(projected.phase[index] - currentState.previousWrappedPhase[index])
            currentState.previousWrappedPhase[index] = projected.phase[index]
            currentState.unwrappedPhase[index] = unwrappedPhase
            currentState.lowPhase[index] += lowAlpha * (unwrappedPhase - currentState.lowPhase[index])
            currentState.highPhase[index] += highAlpha * (unwrappedPhase - currentState.highPhase[index])

            val bandpassedPhase = currentState.highPhase[index] - currentState.lowPhase[index]
            val gatedAmplification = if (projected.amplitude[index] >= amplitudeThreshold) {
                settings.amplification
            } else {
                0.0f
            }
            val amplifiedPhase = projected.phase[index] + bandpassedPhase * gatedAmplification
            val reconstructedLuminance = (projected.amplitude[index] * cos(amplifiedPhase)).coerceIn(0.0, 1.0)
            val luminanceDelta = ((reconstructedLuminance - luminance[index]) * CHANNEL_MAX).roundToInt()
            outputPixels[index] = addLuminanceDelta(pixel, luminanceDelta)
        }

        currentState.lastTimestampNanos = frame.timestampNanos
        return outputClamp.clampFrame(
            base = frame,
            candidate = frame.copy(pixels = outputPixels),
        )
    }

    private fun dominantOrientation(rieszX: DoubleArray, rieszY: DoubleArray): Double {
        val sumX = rieszX.sum()
        val sumY = rieszY.sum()
        return if (kotlin.math.abs(sumX) < EPSILON && kotlin.math.abs(sumY) < EPSILON) {
            0.0
        } else {
            atan2(sumY, sumX)
        }
    }

    private fun projectPhase(
        luminance: DoubleArray,
        rieszX: DoubleArray,
        rieszY: DoubleArray,
        orientation: Double,
    ): PhaseProjection {
        val cosTheta = cos(orientation)
        val sinTheta = sin(orientation)
        val amplitude = DoubleArray(luminance.size)
        val phase = DoubleArray(luminance.size)
        for (index in luminance.indices) {
            val orientedRiesz = rieszX[index] * cosTheta + rieszY[index] * sinTheta
            amplitude[index] = hypot(luminance[index], orientedRiesz)
            phase[index] = atan2(orientedRiesz, luminance[index])
        }
        return PhaseProjection(amplitude, phase)
    }

    private fun convolve(
        width: Int,
        height: Int,
        source: DoubleArray,
        kernel: DoubleArray,
    ): DoubleArray {
        val output = DoubleArray(source.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0.0
                for (ky in -1..1) {
                    val sampleY = (y + ky).coerceIn(0, height - 1)
                    for (kx in -1..1) {
                        val sampleX = (x + kx).coerceIn(0, width - 1)
                        val kernelIndex = (ky + 1) * 3 + (kx + 1)
                        sum += source[sampleY * width + sampleX] * kernel[kernelIndex]
                    }
                }
                output[y * width + x] = sum
            }
        }
        return output
    }

    private fun alpha(cutoffHz: Double, dtSeconds: Double): Double {
        return 1.0 - exp(-2.0 * PI * cutoffHz * dtSeconds)
    }

    private fun wrapPhase(value: Double): Double {
        var wrapped = value
        while (wrapped <= -PI) wrapped += TWO_PI
        while (wrapped > PI) wrapped -= TWO_PI
        return wrapped
    }

    private fun luminance(pixel: Int): Double {
        return red(pixel) * 0.2126 + green(pixel) * 0.7152 + blue(pixel) * 0.0722
    }

    private fun addLuminanceDelta(pixel: Int, delta: Int): Int {
        return rgb(
            red = red(pixel) + delta,
            green = green(pixel) + delta,
            blue = blue(pixel) + delta,
        )
    }

    private fun red(pixel: Int): Int = (pixel shr 16) and 0xFF
    private fun green(pixel: Int): Int = (pixel shr 8) and 0xFF
    private fun blue(pixel: Int): Int = pixel and 0xFF

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }

    private data class PhaseProjection(
        val amplitude: DoubleArray,
        val phase: DoubleArray,
    )

    private class PhaseState(
        val width: Int,
        val height: Int,
        var lastTimestampNanos: Long,
        val previousWrappedPhase: DoubleArray,
        val unwrappedPhase: DoubleArray,
        val lowPhase: DoubleArray,
        val highPhase: DoubleArray,
    ) {
        fun matches(frame: RgbFrame): Boolean {
            return width == frame.width && height == frame.height
        }

        companion object {
            fun from(frame: RgbFrame, phase: DoubleArray): PhaseState {
                return PhaseState(
                    width = frame.width,
                    height = frame.height,
                    lastTimestampNanos = frame.timestampNanos,
                    previousWrappedPhase = phase.copyOf(),
                    unwrappedPhase = phase.copyOf(),
                    lowPhase = phase.copyOf(),
                    highPhase = phase.copyOf(),
                )
            }
        }
    }

    companion object {
        private const val CHANNEL_MAX = 255.0
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val TWO_PI = 2.0 * PI
        private const val EPSILON = 1e-12
        private const val DEFAULT_AMPLITUDE_THRESHOLD = 0.03
        private val RIESZ_X = doubleArrayOf(
            0.0, 0.0, 0.0,
            -0.5, 0.0, 0.5,
            0.0, 0.0, 0.0,
        )
        private val RIESZ_Y = doubleArrayOf(
            0.0, -0.5, 0.0,
            0.0, 0.0, 0.0,
            0.0, 0.5, 0.0,
        )
    }
}
