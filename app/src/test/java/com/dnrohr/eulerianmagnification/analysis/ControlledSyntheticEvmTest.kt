package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

class ControlledSyntheticEvmTest {
    @Test
    fun cpuReferenceEvmMagnifiesPulseBandMoreThanSlowDrift() {
        val pulseMetrics = runSyntheticSequence(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 10.0f,
            ),
            frequencyHz = 1.2,
        )
        val driftMetrics = runSyntheticSequence(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 10.0f,
            ),
            frequencyHz = 0.2,
        )

        assertTrue(pulseMetrics.processedVariation > pulseMetrics.rawVariation * 1.6)
        assertTrue(pulseMetrics.processedGain > driftMetrics.processedGain * 1.5)
    }

    @Test
    fun zeroAmplificationDoesNotIncreaseSyntheticVariation() {
        val metrics = runSyntheticSequence(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 0.0f,
            ),
            frequencyHz = 1.2,
        )

        assertTrue(abs(metrics.processedVariation - metrics.rawVariation) < 0.001)
    }

    @Test
    fun breathingPresetMagnifiesBreathingBandMoreThanPulseBand() {
        val breathingMetrics = runSyntheticSequence(
            settings = AnalysisSettings(
                mode = MagnificationMode.Breathing,
                amplification = 12.0f,
            ),
            frequencyHz = 0.25,
            frameCount = 900,
        )
        val pulseMetrics = runSyntheticSequence(
            settings = AnalysisSettings(
                mode = MagnificationMode.Breathing,
                amplification = 12.0f,
            ),
            frequencyHz = 1.2,
            frameCount = 900,
        )

        assertTrue(breathingMetrics.processedGain > pulseMetrics.processedGain * 1.4)
    }

    private fun runSyntheticSequence(
        settings: AnalysisSettings,
        frequencyHz: Double,
        frameCount: Int = 300,
    ): SyntheticEvmMetrics {
        val pyramidBuilder = RgbFramePyramidBuilder(levelCount = 3)
        val temporalBandpass = RgbPyramidTemporalBandpass(settings)
        val reconstructor = RgbPyramidReconstructor()
        val rawValues = mutableListOf<Double>()
        val processedValues = mutableListOf<Double>()

        for (frameIndex in 0 until frameCount) {
            val seconds = frameIndex / FPS
            val timestampNanos = (seconds * NANOS_PER_SECOND).toLong()
            val green = 128 + (4.0 * sin(2.0 * PI * frequencyHz * seconds)).roundToInt()
            val frame = syntheticFrame(green = green, timestampNanos = timestampNanos)
            val pyramid = pyramidBuilder.build(frame)
            val bandpass = temporalBandpass.update(pyramid)
            val processed = reconstructor.reconstruct(
                base = frame,
                bandpass = bandpass,
                amplification = settings.amplification,
                startLevel = 0,
            )

            if (frameIndex >= WARMUP_FRAMES) {
                rawValues.add(frame.centerGreen())
                processedValues.add(processed.centerGreen())
            }
        }

        val rawVariation = rawValues.variation()
        val processedVariation = processedValues.variation()
        return SyntheticEvmMetrics(
            rawVariation = rawVariation,
            processedVariation = processedVariation,
        )
    }

    private fun syntheticFrame(green: Int, timestampNanos: Long): RgbFrame {
        return RgbFrame(
            width = WIDTH,
            height = HEIGHT,
            timestampNanos = timestampNanos,
            pixels = IntArray(WIDTH * HEIGHT) { index ->
                val x = index % WIDTH
                val y = index / WIDTH
                if (x in SIGNAL_LEFT until SIGNAL_RIGHT && y in SIGNAL_TOP until SIGNAL_BOTTOM) {
                    rgb(96, green, 96)
                } else {
                    rgb(96, 128, 96)
                }
            },
        )
    }

    private fun RgbFrame.centerGreen(): Double {
        var sum = 0.0
        var count = 0
        for (y in SIGNAL_TOP until SIGNAL_BOTTOM) {
            for (x in SIGNAL_LEFT until SIGNAL_RIGHT) {
                val pixel = pixels[y * width + x]
                sum += (pixel shr 8) and 0xFF
                count++
            }
        }
        return sum / count
    }

    private fun List<Double>.variation(): Double {
        if (isEmpty()) return 0.0
        return maxOrNull()!! - minOrNull()!!
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red shl 16) or (green shl 8) or blue
    }

    private data class SyntheticEvmMetrics(
        val rawVariation: Double,
        val processedVariation: Double,
    ) {
        val processedGain: Double get() = processedVariation - rawVariation
    }

    companion object {
        private const val WIDTH = 32
        private const val HEIGHT = 24
        private const val SIGNAL_LEFT = 8
        private const val SIGNAL_RIGHT = 24
        private const val SIGNAL_TOP = 6
        private const val SIGNAL_BOTTOM = 18
        private const val FPS = 30.0
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val WARMUP_FRAMES = 60
    }
}
