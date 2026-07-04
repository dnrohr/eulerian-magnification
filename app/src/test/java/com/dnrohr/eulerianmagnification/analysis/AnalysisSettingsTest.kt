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
}
