package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisSettingsTest {
    @Test
    fun exposesPulseBand() {
        val settings = AnalysisSettings(mode = MagnificationMode.Pulse)

        assertEquals(0.7, settings.lowCutHz, 0.0)
        assertEquals(3.0, settings.highCutHz, 0.0)
    }

    @Test
    fun exposesBreathingBand() {
        val settings = AnalysisSettings(mode = MagnificationMode.Breathing)

        assertEquals(0.1, settings.lowCutHz, 0.0)
        assertEquals(0.6, settings.highCutHz, 0.0)
    }

    @Test
    fun exposesTremorBand() {
        val settings = AnalysisSettings(mode = MagnificationMode.Tremor)

        assertEquals(4.0, settings.lowCutHz, 0.0)
        assertEquals(12.0, settings.highCutHz, 0.0)
    }

    @Test
    fun exposesObjectVibrationBand() {
        val settings = AnalysisSettings(mode = MagnificationMode.ObjectVibration)

        assertEquals(3.0, settings.lowCutHz, 0.0)
        assertEquals(12.0, settings.highCutHz, 0.0)
    }

    @Test
    fun exposesTruthfulOutputLabels() {
        assertEquals("Color amplification", MagnificationMode.Pulse.outputLabel)
        assertEquals("Color amp", MagnificationMode.Pulse.compactOutputLabel)
        assertEquals("Breathing signal", MagnificationMode.Breathing.outputLabel)
        assertEquals("Breath sig", MagnificationMode.Breathing.compactOutputLabel)
        assertEquals("Experimental motion analysis", MagnificationMode.Tremor.outputLabel)
        assertEquals("Motion exp", MagnificationMode.Tremor.compactOutputLabel)
        assertEquals("Experimental object vibration", MagnificationMode.ObjectVibration.outputLabel)
        assertEquals("Object exp", MagnificationMode.ObjectVibration.compactOutputLabel)
    }
}
