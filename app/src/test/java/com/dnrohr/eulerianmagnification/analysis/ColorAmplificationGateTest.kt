package com.dnrohr.eulerianmagnification.analysis

import com.dnrohr.eulerianmagnification.quality.LightingDiagnostic
import com.dnrohr.eulerianmagnification.quality.LightingDiagnosticStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorAmplificationGateTest {
    @Test
    fun stablePulseInputKeepsFullAmplification() {
        val result = ColorAmplificationGate().evaluate(
            mode = MagnificationMode.Pulse,
            lighting = diagnostic(LightingDiagnosticStatus.Stable),
            saturatedPixelFraction = 0.0,
        )

        assertEquals(ColorAmplificationGateReason.Stable, result.reason)
        assertEquals(1.0f, result.gain)
        assertFalse(result.attenuated)
    }

    @Test
    fun lightingFlickerDampensPulseColorAmplification() {
        val result = ColorAmplificationGate().evaluate(
            mode = MagnificationMode.Pulse,
            lighting = diagnostic(LightingDiagnosticStatus.FlickerLikely),
            saturatedPixelFraction = 0.0,
        )

        assertEquals(ColorAmplificationGateReason.LightingFlicker, result.reason)
        assertEquals(0.25f, result.gain)
        assertTrue(result.attenuated)
    }

    @Test
    fun exposurePumpingDampensPulseColorAmplification() {
        val result = ColorAmplificationGate().evaluate(
            mode = MagnificationMode.Pulse,
            lighting = diagnostic(LightingDiagnosticStatus.ExposurePumping),
            saturatedPixelFraction = 0.0,
        )

        assertEquals(ColorAmplificationGateReason.ExposurePumping, result.reason)
        assertEquals(0.25f, result.gain)
    }

    @Test
    fun tooDarkDisablesPulseColorAmplification() {
        val result = ColorAmplificationGate().evaluate(
            mode = MagnificationMode.Pulse,
            lighting = diagnostic(LightingDiagnosticStatus.TooDark),
            saturatedPixelFraction = 0.0,
        )

        assertEquals(ColorAmplificationGateReason.TooDark, result.reason)
        assertEquals(0.0f, result.gain)
    }

    @Test
    fun saturatedRoiDampensOtherwiseStablePulseColorAmplification() {
        val result = ColorAmplificationGate().evaluate(
            mode = MagnificationMode.Pulse,
            lighting = diagnostic(LightingDiagnosticStatus.Stable),
            saturatedPixelFraction = 0.2,
        )

        assertEquals(ColorAmplificationGateReason.Saturated, result.reason)
        assertEquals(0.4f, result.gain)
        assertEquals(0.2, result.saturatedPixelFraction, 0.0001)
    }

    @Test
    fun nonPulseModesAreNotColorGated() {
        val result = ColorAmplificationGate().evaluate(
            mode = MagnificationMode.Tremor,
            lighting = diagnostic(LightingDiagnosticStatus.FlickerLikely),
            saturatedPixelFraction = 0.2,
        )

        assertEquals(ColorAmplificationGateReason.Stable, result.reason)
        assertEquals(1.0f, result.gain)
    }

    private fun diagnostic(status: LightingDiagnosticStatus): LightingDiagnostic {
        return LightingDiagnostic(
            status = status,
            averageGreen = 120.0,
            coefficientOfVariation = 0.0,
            flickerLikely = status == LightingDiagnosticStatus.FlickerLikely,
            motionMagnitude = 0.0f,
        )
    }
}
