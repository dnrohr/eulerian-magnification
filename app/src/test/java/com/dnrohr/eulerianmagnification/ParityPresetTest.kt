package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParityPresetTest {
    @Test
    fun definesFourLockedPresets() {
        assertEquals(4, ParityPreset.entries.size)
        assertEquals(MagnificationMode.Pulse, ParityPreset.PulseColor.settings.mode)
        assertEquals(MagnificationMode.Breathing, ParityPreset.BreathingSlowMotion.settings.mode)
        assertEquals(MagnificationMode.ObjectVibration, ParityPreset.ObjectVibration.settings.mode)
        assertEquals(MagnificationMode.Tremor, ParityPreset.FastTremor.settings.mode)
    }

    @Test
    fun lockedPresetsUseExpectedViewsAndBands() {
        assertEquals(ViewMode.Amplified, ParityPreset.PulseColor.settings.viewMode)
        assertEquals("0.7-3 Hz", ParityPreset.PulseColor.bandLabel)
        assertEquals(ViewMode.Difference, ParityPreset.BreathingSlowMotion.settings.viewMode)
        assertEquals("0.1-0.6 Hz", ParityPreset.BreathingSlowMotion.bandLabel)
        assertEquals(ViewMode.Split, ParityPreset.ObjectVibration.settings.viewMode)
        assertEquals("3-12 Hz", ParityPreset.ObjectVibration.bandLabel)
        assertEquals(ViewMode.Split, ParityPreset.FastTremor.settings.viewMode)
        assertEquals("4-12 Hz", ParityPreset.FastTremor.bandLabel)
    }

    @Test
    fun mapsModeToPreset() {
        assertEquals(ParityPreset.PulseColor, ParityPreset.forMode(MagnificationMode.Pulse))
        assertEquals(ParityPreset.BreathingSlowMotion, ParityPreset.forMode(MagnificationMode.Breathing))
        assertEquals(ParityPreset.ObjectVibration, ParityPreset.forMode(MagnificationMode.ObjectVibration))
        assertEquals(ParityPreset.FastTremor, ParityPreset.forMode(MagnificationMode.Tremor))
    }

    @Test
    fun warnsWhenFpsIsTooCloseToBand() {
        val warnings = ParityPresetWarnings.forPreset(
            preset = ParityPreset.FastTremor,
            measuredFps = 24.0,
        )

        assertTrue(warnings.any { it.contains("Measured FPS") })
    }

    @Test
    fun explainsObjectAndTremorBandOverlap() {
        val warnings = ParityPresetWarnings.forPreset(
            preset = ParityPreset.ObjectVibration,
            measuredFps = 30.0,
        )

        assertTrue(warnings.any { it.contains("intentionally overlap") })
    }
}
