package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import org.junit.Assert.assertEquals
import org.junit.Test

class DemoPresetTest {
    @Test
    fun pulsePresetUsesAmplifiedPulseColorView() {
        val settings = DemoPreset.Pulse.settings

        assertEquals(MagnificationMode.Pulse, settings.mode)
        assertEquals(ViewMode.Amplified, settings.viewMode)
        assertEquals(12.0f, settings.amplification)
    }

    @Test
    fun breathingPresetUsesDifferenceViewForSignalInspection() {
        val settings = DemoPreset.Breathing.settings

        assertEquals(MagnificationMode.Breathing, settings.mode)
        assertEquals(ViewMode.Difference, settings.viewMode)
        assertEquals(16.0f, settings.amplification)
    }

    @Test
    fun fastMotionPresetUsesSplitComparison() {
        val settings = DemoPreset.FastMotion.settings

        assertEquals(MagnificationMode.Tremor, settings.mode)
        assertEquals(ViewMode.Split, settings.viewMode)
        assertEquals(20.0f, settings.amplification)
    }
}
