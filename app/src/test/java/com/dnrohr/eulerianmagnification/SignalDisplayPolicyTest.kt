package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SignalDisplayPolicyTest {
    @Test
    fun pulseUsesPulseHistory() {
        val pulse = listOf(1.0, 2.0)
        val breathing = listOf(9.0)

        assertEquals(
            pulse,
            SignalDisplayPolicy.compactSignalHistory(MagnificationMode.Pulse, pulse, breathing),
        )
    }

    @Test
    fun breathingUsesBreathingHistory() {
        val pulse = listOf(1.0, 2.0)
        val breathing = listOf(9.0)

        assertEquals(
            breathing,
            SignalDisplayPolicy.compactSignalHistory(MagnificationMode.Breathing, pulse, breathing),
        )
    }

    @Test
    fun fastMotionUsesPulseBandHistory() {
        val pulse = listOf(1.0, 2.0)
        val breathing = listOf(9.0)

        assertEquals(
            pulse,
            SignalDisplayPolicy.compactSignalHistory(MagnificationMode.Tremor, pulse, breathing),
        )
    }
}
