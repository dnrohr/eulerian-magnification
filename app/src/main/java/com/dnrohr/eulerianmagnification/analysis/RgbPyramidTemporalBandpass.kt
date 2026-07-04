package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt

data class RgbPyramidBandpass(
    val levels: List<RgbFrame>,
) {
    init {
        require(levels.isNotEmpty()) { "levels must not be empty" }
    }
}

class RgbPyramidTemporalBandpass(
    settings: AnalysisSettings,
) {
    private val lowCutHz = settings.lowCutHz
    private val highCutHz = settings.highCutHz
    private var states: List<LevelState>? = null
    private var lastTimestampNanos: Long? = null

    fun update(pyramid: RgbFramePyramid): RgbPyramidBandpass {
        val previousTimestamp = lastTimestampNanos
        lastTimestampNanos = pyramid.levels.first().timestampNanos

        val currentStates = states
        if (previousTimestamp == null || currentStates == null || !currentStates.matches(pyramid)) {
            states = pyramid.levels.map { LevelState.from(it) }
            return RgbPyramidBandpass(pyramid.levels.map { it.zeroLike() })
        }

        val dtSeconds = (pyramid.levels.first().timestampNanos - previousTimestamp)
            .coerceAtLeast(1L) / NANOS_PER_SECOND
        val lowAlpha = alpha(lowCutHz, dtSeconds)
        val highAlpha = alpha(highCutHz, dtSeconds)
        val outputLevels = pyramid.levels.mapIndexed { index, frame ->
            currentStates[index].update(frame, lowAlpha, highAlpha)
        }
        return RgbPyramidBandpass(outputLevels)
    }

    private fun alpha(cutoffHz: Double, dtSeconds: Double): Double {
        return 1.0 - exp(-2.0 * PI * cutoffHz * dtSeconds)
    }

    private fun List<LevelState>.matches(pyramid: RgbFramePyramid): Boolean {
        return size == pyramid.levels.size && zip(pyramid.levels).all { (state, frame) ->
            state.width == frame.width && state.height == frame.height
        }
    }

    private class LevelState(
        val width: Int,
        val height: Int,
        private val low: DoubleArray,
        private val high: DoubleArray,
    ) {
        fun update(frame: RgbFrame, lowAlpha: Double, highAlpha: Double): RgbFrame {
            val output = IntArray(frame.pixels.size)
            frame.pixels.forEachIndexed { pixelIndex, pixel ->
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                val channelIndex = pixelIndex * CHANNELS
                output[pixelIndex] = rgb(
                    red = updateChannel(channelIndex, red.toDouble(), lowAlpha, highAlpha),
                    green = updateChannel(channelIndex + 1, green.toDouble(), lowAlpha, highAlpha),
                    blue = updateChannel(channelIndex + 2, blue.toDouble(), lowAlpha, highAlpha),
                )
            }
            return RgbFrame(
                width = frame.width,
                height = frame.height,
                timestampNanos = frame.timestampNanos,
                pixels = output,
            )
        }

        private fun updateChannel(
            index: Int,
            value: Double,
            lowAlpha: Double,
            highAlpha: Double,
        ): Int {
            low[index] += lowAlpha * (value - low[index])
            high[index] += highAlpha * (value - high[index])
            return (high[index] - low[index]).roundToInt().coerceIn(-255, 255)
        }

        companion object {
            fun from(frame: RgbFrame): LevelState {
                val low = DoubleArray(frame.pixels.size * CHANNELS)
                val high = DoubleArray(frame.pixels.size * CHANNELS)
                frame.pixels.forEachIndexed { pixelIndex, pixel ->
                    val channelIndex = pixelIndex * CHANNELS
                    low[channelIndex] = ((pixel shr 16) and 0xFF).toDouble()
                    low[channelIndex + 1] = ((pixel shr 8) and 0xFF).toDouble()
                    low[channelIndex + 2] = (pixel and 0xFF).toDouble()
                    high[channelIndex] = low[channelIndex]
                    high[channelIndex + 1] = low[channelIndex + 1]
                    high[channelIndex + 2] = low[channelIndex + 2]
                }
                return LevelState(
                    width = frame.width,
                    height = frame.height,
                    low = low,
                    high = high,
                )
            }
        }
    }

    private fun RgbFrame.zeroLike(): RgbFrame {
        return RgbFrame(
            width = width,
            height = height,
            timestampNanos = timestampNanos,
            pixels = IntArray(pixels.size),
        )
    }

    companion object {
        private const val CHANNELS = 3
        private const val NANOS_PER_SECOND = 1_000_000_000.0

        fun rgb(red: Int, green: Int, blue: Int): Int {
            val encodedRed = red.coerceIn(-255, 255) and 0xFF
            val encodedGreen = green.coerceIn(-255, 255) and 0xFF
            val encodedBlue = blue.coerceIn(-255, 255) and 0xFF
            return (encodedRed shl 16) or (encodedGreen shl 8) or encodedBlue
        }

        fun signedRed(pixel: Int): Int = signedChannel((pixel shr 16) and 0xFF)
        fun signedGreen(pixel: Int): Int = signedChannel((pixel shr 8) and 0xFF)
        fun signedBlue(pixel: Int): Int = signedChannel(pixel and 0xFF)

        private fun signedChannel(value: Int): Int {
            return if (value >= 128) value - 256 else value
        }
    }
}
