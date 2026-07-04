package com.dnrohr.eulerianmagnification.gl

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMagnificationPassTest {
    @Test
    fun shaderLimitsAmplificationToRoi() {
        assertTrue(ColorMagnificationShaderSource.FRAGMENT.contains("insideRoi"))
        assertTrue(ColorMagnificationShaderSource.FRAGMENT.contains("uRoi"))
    }

    @Test
    fun shaderSupportsDifferenceMode() {
        assertTrue(ColorMagnificationShaderSource.FRAGMENT.contains("uDifferenceMode"))
        assertTrue(ColorMagnificationShaderSource.FRAGMENT.contains("abs(delta)"))
    }

    @Test
    fun mapsSampleAndSettingsToUniforms() {
        val uniforms = ColorMagnificationParameters().from(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.2f, 0.3f, 0.4f),
                bandpassedGreen = 0.5,
            ),
            settings = AnalysisSettings(amplification = 8.0f),
        )

        assertEquals(0.1f, uniforms.roi.left, 0.0f)
        assertEquals(0.0625f, uniforms.amplifiedSignal, 0.0001f)
        assertFalse(uniforms.differenceMode)
        assertFalse(uniforms.splitMode)
    }

    @Test
    fun mapsDifferenceModeFlag() {
        val uniforms = ColorMagnificationParameters().from(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.2f, 0.3f, 0.4f),
                bandpassedGreen = 0.5,
            ),
            settings = AnalysisSettings(viewMode = ViewMode.Difference),
        )

        assertTrue(uniforms.differenceMode)
    }

    @Test
    fun mapsSplitModeFlag() {
        val uniforms = ColorMagnificationParameters().from(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.2f, 0.3f, 0.4f),
                bandpassedGreen = 0.5,
            ),
            settings = AnalysisSettings(viewMode = ViewMode.Split),
        )

        assertTrue(uniforms.splitMode)
        assertFalse(uniforms.differenceMode)
    }

    @Test
    fun rawModeSuppressesAmplifiedSignal() {
        val uniforms = ColorMagnificationParameters().from(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.2f, 0.3f, 0.4f),
                bandpassedGreen = 0.5,
            ),
            settings = AnalysisSettings(
                amplification = 8.0f,
                viewMode = ViewMode.Raw,
            ),
        )

        assertEquals(0.0f, uniforms.amplifiedSignal, 0.0f)
    }
}
