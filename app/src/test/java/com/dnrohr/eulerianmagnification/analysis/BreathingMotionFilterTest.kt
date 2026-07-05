package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class BreathingMotionFilterTest {
    @Test
    fun startsAtZeroForFirstMotionSample() {
        val filter = BreathingMotionFilter(amplification = 10.0f)

        val sample = filter.update(
            translation = TranslationEstimate(dx = 0.0f, dy = 0.01f),
            timestampNanos = 0L,
        )

        assertEquals(0.0, sample.bandpassedDy, 0.0)
        assertEquals(0.0, sample.amplifiedDy, 0.0)
    }

    @Test
    fun amplificationScalesBandpassedMotion() {
        val filter = BreathingMotionFilter(amplification = 6.0f)
        filter.update(TranslationEstimate(dy = 0.0f), 0L)

        val sample = filter.update(
            translation = TranslationEstimate(dx = 0.0f, dy = 0.02f),
            timestampNanos = 33_333_333L,
        )

        assertEquals(sample.bandpassedDy * 6.0, sample.amplifiedDy, 0.000001)
    }

    @Test
    fun breathingBandPassesSlowVerticalMotionMoreThanPulseMotion() {
        val breathingFilter = BreathingMotionFilter(amplification = 1.0f)
        val pulseFilter = BreathingMotionFilter(amplification = 1.0f)
        var breathingEnergy = 0.0
        var pulseEnergy = 0.0

        for (frame in 0 until 900) {
            val seconds = frame / FPS
            val timestamp = (seconds * NANOS_PER_SECOND).toLong()
            val breathingDy = (0.01 * sin(2.0 * PI * 0.25 * seconds)).toFloat()
            val pulseDy = (0.01 * sin(2.0 * PI * 1.2 * seconds)).toFloat()

            breathingEnergy += abs(
                breathingFilter.update(TranslationEstimate(dy = breathingDy), timestamp).bandpassedDy,
            )
            pulseEnergy += abs(
                pulseFilter.update(TranslationEstimate(dy = pulseDy), timestamp).bandpassedDy,
            )
        }

        assertTrue(breathingEnergy > pulseEnergy * 1.5)
    }

    @Test
    fun ignoresHorizontalMotionForBreathingSignal() {
        val filter = BreathingMotionFilter(amplification = 10.0f)
        var energy = 0.0

        for (frame in 0 until 120) {
            val seconds = frame / FPS
            val timestamp = (seconds * NANOS_PER_SECOND).toLong()
            val horizontalDx = (0.02 * sin(2.0 * PI * 0.25 * seconds)).toFloat()
            energy += abs(filter.update(TranslationEstimate(dx = horizontalDx, dy = 0.0f), timestamp).amplifiedDy)
        }

        assertEquals(0.0, energy, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeAmplification() {
        BreathingMotionFilter(amplification = -1.0f)
    }

    companion object {
        private const val FPS = 30.0
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
