package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

class FullFrameLinearEvmRendererTest {
    @Test
    fun stationaryFramesRemainStable() {
        val renderer = FullFrameLinearEvmRenderer(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 20.0f,
            ),
        )
        val frames = List(60) { frame(timestampNanos = it * FRAME_NANOS, green = 128) }

        val outputs = frames.map(renderer::render)

        outputs.forEach { output ->
            assertEquals(frames.first().pixels.toList(), output.pixels.toList())
        }
    }

    @Test
    fun inBandColorSignalChangesFullFrameOutput() {
        val renderer = FullFrameLinearEvmRenderer(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 20.0f,
            ),
        )
        val frames = syntheticClip(frequencyHz = 1.2)

        val outputs = frames.map(renderer::render)

        assertTrue(
            outputs.zip(frames).any { (output, input) ->
                output.pixels[0] != input.pixels[0]
            },
        )
    }

    @Test
    fun inBandPulseProducesMoreChangeThanOutOfBandDrift() {
        val pulseEnergy = outputEnergy(syntheticClip(frequencyHz = 1.2))
        val driftEnergy = outputEnergy(syntheticClip(frequencyHz = 0.2))

        assertTrue(pulseEnergy > driftEnergy * 1.5)
    }

    @Test
    fun inBandTranslatingEdgeChangesRenderedOutput() {
        val renderer = FullFrameLinearEvmRenderer(
            settings = AnalysisSettings(
                mode = MagnificationMode.Tremor,
                amplification = 12.0f,
            ),
        )
        val frames = translatingEdgeClip(frequencyHz = 6.0)

        val outputs = frames.map(renderer::render)

        assertTrue(
            outputs.zip(frames).any { (output, input) ->
                output.pixels.toList() != input.pixels.toList()
            },
        )
    }

    private fun outputEnergy(frames: List<RgbFrame>): Double {
        val renderer = FullFrameLinearEvmRenderer(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 20.0f,
            ),
        )
        return frames
            .map(renderer::render)
            .zip(frames)
            .sumOf { (output, input) ->
                abs(green(output.pixels[0]) - green(input.pixels[0])).toDouble()
            }
    }

    private fun syntheticClip(frequencyHz: Double): List<RgbFrame> {
        return (0 until FRAME_COUNT).map { frameIndex ->
            val seconds = frameIndex / FPS
            val green = 128 + (8.0 * sin(2.0 * PI * frequencyHz * seconds)).roundToInt()
            frame(timestampNanos = (seconds * NANOS_PER_SECOND).toLong(), green = green)
        }
    }

    private fun translatingEdgeClip(frequencyHz: Double): List<RgbFrame> {
        return (0 until FRAME_COUNT).map { frameIndex ->
            val seconds = frameIndex / FPS
            val edgeOffset = (2.0 * sin(2.0 * PI * frequencyHz * seconds)).roundToInt()
            val edgeX = WIDTH / 2 + edgeOffset
            RgbFrame(
                width = WIDTH,
                height = HEIGHT,
                timestampNanos = (seconds * NANOS_PER_SECOND).toLong(),
                pixels = IntArray(WIDTH * HEIGHT) { index ->
                    val x = index % WIDTH
                    if (x < edgeX) {
                        rgb(48, 48, 48)
                    } else {
                        rgb(208, 208, 208)
                    }
                },
            )
        }
    }

    private fun frame(timestampNanos: Long, green: Int): RgbFrame {
        return RgbFrame(
            width = WIDTH,
            height = HEIGHT,
            timestampNanos = timestampNanos,
            pixels = IntArray(WIDTH * HEIGHT) { rgb(96, green, 96) },
        )
    }

    private fun rgb(
        red: Int,
        green: Int,
        blue: Int,
    ): Int {
        return (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }

    private fun green(pixel: Int): Int = (pixel shr 8) and 0xFF

    companion object {
        private const val WIDTH = 16
        private const val HEIGHT = 12
        private const val FPS = 30.0
        private const val FRAME_COUNT = 300
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val FRAME_NANOS = 33_333_333L
    }
}
