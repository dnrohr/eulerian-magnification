package com.dnrohr.eulerianmagnification.gl

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMagnificationPassTest {
    @Test
    fun shadersStartWithVersionDeclaration() {
        assertTrue(ColorMagnificationShaderSource.VERTEX.startsWith("#version 300 es"))
        assertTrue(ColorMagnificationShaderSource.FRAGMENT.startsWith("#version 300 es"))
    }

    @Test
    fun shaderLimitsAmplificationToRoi() {
        assertTrue(ColorMagnificationShaderSource.FRAGMENT.contains("insideRoi"))
        assertTrue(ColorMagnificationShaderSource.FRAGMENT.contains("uRoi"))
        assertTrue(ColorMagnificationShaderSource.FRAGMENT.contains("uFullFrameMode"))
    }

    @Test
    fun shaderSupportsDifferenceMode() {
        assertTrue(ColorMagnificationShaderSource.FRAGMENT.contains("uDifferenceMode"))
        assertTrue(ColorMagnificationShaderSource.FRAGMENT.contains("signedColor"))
        assertTrue(ColorMagnificationShaderSource.FRAGMENT.contains("uAmplifiedSignal >= 0.0"))
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
        assertFalse(uniforms.fullFrameMode)
        assertEquals(8.0f, uniforms.amplification, 0.0f)
        assertEquals(0.7, uniforms.lowCutHz, 0.0)
        assertEquals(3.0, uniforms.highCutHz, 0.0)
        assertEquals(LivePyramidReconstructionProfile.PulseColor, uniforms.reconstructionProfile)
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
    fun mapsFullFrameModeFlag() {
        val uniforms = ColorMagnificationParameters().from(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.2f, 0.3f, 0.4f),
                bandpassedGreen = 0.5,
            ),
            settings = AnalysisSettings(viewMode = ViewMode.Split),
            fullFrameMode = true,
        )

        assertTrue(uniforms.fullFrameMode)
    }

    @Test
    fun mapsModeBandForLiveReconstruction() {
        val uniforms = ColorMagnificationParameters().from(
            sample = AnalysisSample(),
            settings = AnalysisSettings(mode = MagnificationMode.Breathing),
        )

        assertEquals(0.1, uniforms.lowCutHz, 0.0)
        assertEquals(0.6, uniforms.highCutHz, 0.0)
        assertEquals(LivePyramidReconstructionProfile.SlowMotion, uniforms.reconstructionProfile)
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

    @Test
    fun mapsPresentationTimestampForEncoderSurfaceExport() {
        val uniforms = ColorMagnificationParameters().from(
            sample = AnalysisSample(frameTimestampNanos = 50L),
            settings = AnalysisSettings(),
            presentationTimestampNanos = 33_333_333L,
        )

        assertEquals(33_333_333L, uniforms.presentationTimestampNanos)
    }

    @Test
    fun clampsPresentationTimestampToNonNegativeValue() {
        val uniforms = ColorMagnificationParameters().from(
            sample = AnalysisSample(frameTimestampNanos = -50L),
            settings = AnalysisSettings(),
            presentationTimestampNanos = -1L,
        )

        assertEquals(0L, uniforms.presentationTimestampNanos)
    }
}
