package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class GatedRateEstimatorTest {
    @Test
    fun estimatesPulseRateFromCleanSyntheticSignal() {
        val result = GatedRateEstimator.estimate(
            mode = MagnificationMode.Pulse,
            samples = sineSignal(frequencyHz = 1.2),
            gate = passingGate(),
        )

        assertTrue(result.visible)
        assertNotNull(result.estimate)
        val estimate = requireNotNull(result.estimate)
        assertEquals(RateEstimateKind.Pulse, estimate.kind)
        assertEquals(72.0, estimate.perMinute, 1.0)
        assertFalse(estimate.diagnostic)
        assertTrue(estimate.experimental)
    }

    @Test
    fun estimatesBreathingRateFromCleanSyntheticSignal() {
        val result = GatedRateEstimator.estimate(
            mode = MagnificationMode.Breathing,
            samples = sineSignal(frequencyHz = 0.25, seconds = 14),
            gate = passingGate(frameCount = 420, bandpassedEnergy = 80.0),
        )

        assertTrue(result.visible)
        val estimate = requireNotNull(result.estimate)
        assertEquals(RateEstimateKind.Breathing, estimate.kind)
        assertEquals(15.0, estimate.perMinute, 0.6)
    }

    @Test
    fun hidesUnsupportedFastMotionMode() {
        val result = GatedRateEstimator.estimate(
            mode = MagnificationMode.Tremor,
            samples = sineSignal(frequencyHz = 5.0),
            gate = passingGate(),
        )

        assertFalse(result.visible)
        assertEquals(RateEstimateHiddenReason.UnsupportedMode, result.hiddenReason)
    }

    @Test
    fun hidesWhenPrerequisitesFail() {
        val cases = listOf(
            passingGate(frameCount = 60) to RateEstimateHiddenReason.TooFewFrames,
            passingGate(averageFps = 12.0) to RateEstimateHiddenReason.LowFps,
            passingGate(timestampsMonotonic = false) to RateEstimateHiddenReason.TimingUnstable,
            passingGate(hasRoi = false) to RateEstimateHiddenReason.MissingRoi,
            passingGate(lightingStable = false) to RateEstimateHiddenReason.LightingUnstable,
            passingGate(motionMagnitude = 0.02f) to RateEstimateHiddenReason.MotionUnstable,
            passingGate(bandpassedEnergy = 1.0) to RateEstimateHiddenReason.WeakSignal,
            passingGate(maxBandpassedMagnitude = 0.01) to RateEstimateHiddenReason.WeakSignal,
        )

        cases.forEach { (gate, reason) ->
            val result = GatedRateEstimator.estimate(
                mode = MagnificationMode.Pulse,
                samples = sineSignal(frequencyHz = 1.2),
                gate = gate,
            )

            assertFalse(result.visible)
            assertEquals(reason, result.hiddenReason)
        }
    }

    @Test
    fun hidesOutOfBandPeriodicSignal() {
        val result = GatedRateEstimator.estimate(
            mode = MagnificationMode.Pulse,
            samples = sineSignal(frequencyHz = 0.25, seconds = 14),
            gate = passingGate(frameCount = 420, bandpassedEnergy = 80.0),
        )

        assertFalse(result.visible)
        assertEquals(RateEstimateHiddenReason.NoPeriodicSignal, result.hiddenReason)
    }

    private fun sineSignal(
        frequencyHz: Double,
        seconds: Int = 10,
        fps: Double = 30.0,
    ): List<RateSignalSample> {
        val frames = (seconds * fps).toInt()
        return (0 until frames).map { frame ->
            val timeSeconds = frame / fps
            RateSignalSample(
                timestampNanos = (timeSeconds * NANOS_PER_SECOND).toLong(),
                value = sin(2.0 * PI * frequencyHz * timeSeconds),
            )
        }
    }

    private fun passingGate(
        frameCount: Int = 300,
        averageFps: Double = 30.0,
        timestampsMonotonic: Boolean = true,
        hasRoi: Boolean = true,
        lightingStable: Boolean = true,
        motionMagnitude: Float = 0.0f,
        bandpassedEnergy: Double = 80.0,
        maxBandpassedMagnitude: Double = 1.0,
    ): RateEstimateGate {
        return RateEstimateGate(
            frameCount = frameCount,
            averageFps = averageFps,
            timestampsMonotonic = timestampsMonotonic,
            hasRoi = hasRoi,
            lightingStable = lightingStable,
            motionMagnitude = motionMagnitude,
            bandpassedEnergy = bandpassedEnergy,
            maxBandpassedMagnitude = maxBandpassedMagnitude,
        )
    }

    private companion object {
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
