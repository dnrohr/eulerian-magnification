package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class BandpassFilterTest {
    @Test
    fun startsAtZeroForFirstSample() {
        val filter = BandpassFilter(lowCutHz = 0.7, highCutHz = 3.0)

        assertEquals(0.0, filter.update(128.0, 0L), 0.0)
    }

    @Test
    fun passesPulseBandMoreThanSlowDrift() {
        val pulseFilter = BandpassFilter(lowCutHz = 0.7, highCutHz = 3.0)
        val driftFilter = BandpassFilter(lowCutHz = 0.7, highCutHz = 3.0)
        var pulseEnergy = 0.0
        var driftEnergy = 0.0

        for (frame in 0 until 300) {
            val seconds = frame / 30.0
            val timestamp = (seconds * 1_000_000_000L).toLong()
            val pulse = 128.0 + 8.0 * sin(2.0 * PI * 1.2 * seconds)
            val drift = 128.0 + 8.0 * sin(2.0 * PI * 0.2 * seconds)
            pulseEnergy += kotlin.math.abs(pulseFilter.update(pulse, timestamp))
            driftEnergy += kotlin.math.abs(driftFilter.update(drift, timestamp))
        }

        assertTrue(pulseEnergy > driftEnergy * 1.8)
    }

    @Test
    fun breathingBandPassesSlowBreathingMoreThanPulse() {
        val breathingFilter = BandpassFilter(lowCutHz = 0.1, highCutHz = 0.6)
        val pulseFilter = BandpassFilter(lowCutHz = 0.1, highCutHz = 0.6)
        var breathingEnergy = 0.0
        var pulseEnergy = 0.0

        for (frame in 0 until 900) {
            val seconds = frame / 30.0
            val timestamp = (seconds * 1_000_000_000L).toLong()
            val breathing = 128.0 + 8.0 * sin(2.0 * PI * 0.25 * seconds)
            val pulse = 128.0 + 8.0 * sin(2.0 * PI * 1.2 * seconds)
            breathingEnergy += kotlin.math.abs(breathingFilter.update(breathing, timestamp))
            pulseEnergy += kotlin.math.abs(pulseFilter.update(pulse, timestamp))
        }

        assertTrue(breathingEnergy > pulseEnergy * 1.5)
    }

    @Test
    fun tremorBandPassesFastMotionMoreThanPulse() {
        val tremorFilter = BandpassFilter(lowCutHz = 4.0, highCutHz = 12.0)
        val pulseFilter = BandpassFilter(lowCutHz = 4.0, highCutHz = 12.0)
        var tremorEnergy = 0.0
        var pulseEnergy = 0.0

        for (frame in 0 until 300) {
            val seconds = frame / 30.0
            val timestamp = (seconds * 1_000_000_000L).toLong()
            val tremor = 128.0 + 8.0 * sin(2.0 * PI * 6.0 * seconds)
            val pulse = 128.0 + 8.0 * sin(2.0 * PI * 1.2 * seconds)
            tremorEnergy += kotlin.math.abs(tremorFilter.update(tremor, timestamp))
            pulseEnergy += kotlin.math.abs(pulseFilter.update(pulse, timestamp))
        }

        assertTrue(tremorEnergy > pulseEnergy * 1.4)
    }

    @Test
    fun objectVibrationBandPassesObjectMotionMoreThanBreathing() {
        val vibrationFilter = BandpassFilter(lowCutHz = 3.0, highCutHz = 12.0)
        val breathingFilter = BandpassFilter(lowCutHz = 3.0, highCutHz = 12.0)
        var vibrationEnergy = 0.0
        var breathingEnergy = 0.0

        for (frame in 0 until 300) {
            val seconds = frame / 30.0
            val timestamp = (seconds * 1_000_000_000L).toLong()
            val vibration = 128.0 + 8.0 * sin(2.0 * PI * 5.0 * seconds)
            val breathing = 128.0 + 8.0 * sin(2.0 * PI * 0.25 * seconds)
            vibrationEnergy += kotlin.math.abs(vibrationFilter.update(vibration, timestamp))
            breathingEnergy += kotlin.math.abs(breathingFilter.update(breathing, timestamp))
        }

        assertTrue(vibrationEnergy > breathingEnergy * 2.0)
    }
}
