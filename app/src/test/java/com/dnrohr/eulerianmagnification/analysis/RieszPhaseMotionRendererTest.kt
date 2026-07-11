package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class RieszPhaseMotionRendererTest {
    @Test
    fun stationaryFramesRemainStable() {
        val renderer = RieszPhaseMotionRenderer(
            settings = AnalysisSettings(
                mode = MagnificationMode.Tremor,
                amplification = 8.0f,
            ),
        )
        val frames = List(90) { frame(timestampNanos = it * FRAME_NANOS, edgeX = WIDTH / 2) }

        val outputs = frames.map(renderer::render)

        outputs.forEach { output ->
            assertEquals(frames.first().pixels.toList(), output.pixels.toList())
        }
    }

    @Test
    fun inBandTranslatingEdgeChangesOutput() {
        val renderer = RieszPhaseMotionRenderer(
            settings = AnalysisSettings(
                mode = MagnificationMode.Tremor,
                amplification = 8.0f,
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

    @Test
    fun inBandMotionProducesMoreOutputThanOutOfBandDrift() {
        val inBandEnergy = outputEnergy(translatingEdgeClip(frequencyHz = 6.0))
        val driftEnergy = outputEnergy(translatingEdgeClip(frequencyHz = 0.5))

        assertTrue(inBandEnergy > driftEnergy * 1.5)
    }

    @Test
    fun phaseMotionClipsLessThanLinearEvmOnHighContrastEdge() {
        val settings = AnalysisSettings(
            mode = MagnificationMode.Tremor,
            amplification = 8.0f,
        )
        val frames = translatingEdgeClip(frequencyHz = 6.0)
        val phaseRenderer = RieszPhaseMotionRenderer(settings)
        val linearRenderer = FullFrameLinearEvmRenderer(settings)

        val phaseOutputs = frames.map(phaseRenderer::render)
        val linearOutputs = frames.map(linearRenderer::render)

        assertTrue(outputEnergy(frames, phaseOutputs) > 0.0)
        assertTrue(clippedFraction(frames, phaseOutputs) <= clippedFraction(frames, linearOutputs))
    }

    private fun outputEnergy(frames: List<RgbFrame>): Double {
        val renderer = RieszPhaseMotionRenderer(
            settings = AnalysisSettings(
                mode = MagnificationMode.Tremor,
                amplification = 8.0f,
            ),
        )
        return frames
            .map(renderer::render)
            .zip(frames)
            .sumOf { (output, input) ->
                output.pixels.indices.sumOf { index ->
                    kotlin.math.abs(luminance(output.pixels[index]) - luminance(input.pixels[index]))
                }
            }
    }

    private fun outputEnergy(
        inputs: List<RgbFrame>,
        outputs: List<RgbFrame>,
    ): Double {
        return outputs
            .zip(inputs)
            .sumOf { (output, input) ->
                output.pixels.indices.sumOf { index ->
                    kotlin.math.abs(luminance(output.pixels[index]) - luminance(input.pixels[index]))
                }
            }
    }

    private fun clippedFraction(
        inputs: List<RgbFrame>,
        outputs: List<RgbFrame>,
    ): Double {
        var clipped = 0
        var total = 0
        outputs.zip(inputs).forEach { (output, input) ->
            output.pixels.indices.forEach { index ->
                if (newlyClipped(input.pixels[index], output.pixels[index])) {
                    clipped++
                }
                total++
            }
        }
        return clipped / total.toDouble()
    }

    private fun newlyClipped(input: Int, output: Int): Boolean {
        return newlyClippedChannel(red(input), red(output)) ||
            newlyClippedChannel(green(input), green(output)) ||
            newlyClippedChannel(blue(input), blue(output))
    }

    private fun newlyClippedChannel(input: Int, output: Int): Boolean {
        return (output == 0 || output == 255) && input != output
    }

    private fun translatingEdgeClip(frequencyHz: Double): List<RgbFrame> {
        return (0 until FRAME_COUNT).map { frameIndex ->
            val seconds = frameIndex / FPS
            val edgeOffset = (2.0 * sin(2.0 * PI * frequencyHz * seconds)).roundToInt()
            frame(
                timestampNanos = (seconds * NANOS_PER_SECOND).toLong(),
                edgeX = WIDTH / 2 + edgeOffset,
            )
        }
    }

    private fun frame(timestampNanos: Long, edgeX: Int): RgbFrame {
        return RgbFrame(
            width = WIDTH,
            height = HEIGHT,
            timestampNanos = timestampNanos,
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

    private fun luminance(pixel: Int): Double {
        return red(pixel) * 0.2126 + green(pixel) * 0.7152 + blue(pixel) * 0.0722
    }

    private fun red(pixel: Int): Int = (pixel shr 16) and 0xFF
    private fun green(pixel: Int): Int = (pixel shr 8) and 0xFF
    private fun blue(pixel: Int): Int = pixel and 0xFF

    private fun rgb(
        red: Int,
        green: Int,
        blue: Int,
    ): Int {
        return (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }

    companion object {
        private const val WIDTH = 32
        private const val HEIGHT = 24
        private const val FPS = 30.0
        private const val FRAME_COUNT = 300
        private const val FRAME_NANOS = 33_333_333L
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
