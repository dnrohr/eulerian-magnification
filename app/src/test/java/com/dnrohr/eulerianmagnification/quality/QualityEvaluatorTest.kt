package com.dnrohr.eulerianmagnification.quality

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import com.dnrohr.eulerianmagnification.analysis.RoiSource
import com.dnrohr.eulerianmagnification.analysis.TranslationEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityEvaluatorTest {
    @Test
    fun returnsGoodForStableBrightSignal() {
        val statuses = QualityEvaluator().evaluate(
            AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 30.0,
                timestampMonotonic = true,
            ),
        )

        assertEquals(listOf(QualityStatus.Good), statuses)
    }

    @Test
    fun detectsMissingFaceDarknessLowFpsAndTimingJump() {
        val statuses = QualityEvaluator().evaluate(
            AnalysisSample(
                roi = null,
                averageGreen = 30.0,
                bandpassedGreen = 0.0,
                analysisFps = 12.0,
                timestampMonotonic = false,
            ),
        )

        assertTrue(QualityStatus.FaceMissing in statuses)
        assertTrue(QualityStatus.TooDark in statuses)
        assertTrue(QualityStatus.LowFps in statuses)
        assertTrue(QualityStatus.TimingUnstable in statuses)
    }

    @Test
    fun doesNotWarnLowFpsForNormalTwentyFourFpsCameraCadence() {
        val statuses = QualityEvaluator().evaluate(
            AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 24.0,
                timestampMonotonic = true,
            ),
        )

        assertTrue(QualityStatus.LowFps !in statuses)
    }

    @Test
    fun warnsLowFpsBelowTwentyFourFpsCameraCadence() {
        val statuses = QualityEvaluator().evaluate(
            AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 23.0,
                timestampMonotonic = true,
            ),
        )

        assertTrue(QualityStatus.LowFps in statuses)
    }

    @Test
    fun warnsFullFrameSlowInsteadOfGenericLowFpsForFullFrameSource() {
        val statuses = QualityEvaluator().evaluate(
            sample = AnalysisSample(
                roi = NormalizedRect(0.0f, 0.0f, 1.0f, 1.0f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 12.0,
                timestampMonotonic = true,
            ),
            roiSource = RoiSource.FullFrame,
        )

        assertTrue(QualityStatus.FullFrameSlow in statuses)
        assertTrue(QualityStatus.LowFps !in statuses)
    }

    @Test
    fun warnsWhenThermalStatusIsModerateOrWorse() {
        val statuses = QualityEvaluator().evaluate(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 30.0,
                timestampMonotonic = true,
            ),
            thermalStatus = "severe",
        )

        assertTrue(QualityStatus.ThermalHigh in statuses)
    }

    @Test
    fun doesNotWarnForLightThermalStatus() {
        val statuses = QualityEvaluator().evaluate(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 30.0,
                timestampMonotonic = true,
            ),
            thermalStatus = "light",
        )

        assertTrue(QualityStatus.ThermalHigh !in statuses)
    }

    @Test
    fun warnsWhenSettledGlCameraCadenceFallsBelowTwentyFourFps() {
        val statuses = QualityEvaluator().evaluate(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 30.0,
                timestampMonotonic = true,
            ),
            cameraFrameFps = 12.0,
            cameraFrameSampleCount = 10,
        )

        assertTrue(QualityStatus.CameraFpsLow in statuses)
        assertTrue(QualityStatus.LowFps !in statuses)
    }

    @Test
    fun suppressesGlCameraCadenceWarningWhileStatsSettle() {
        val statuses = QualityEvaluator().evaluate(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 30.0,
                timestampMonotonic = true,
            ),
            cameraFrameFps = 12.0,
            cameraFrameSampleCount = 9,
        )

        assertTrue(QualityStatus.CameraFpsLow !in statuses)
    }

    @Test
    fun detectsWeakSignalWhenRoiAndFramesAreAvailable() {
        val statuses = QualityEvaluator().evaluate(
            AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.001,
                analysisFps = 30.0,
            ),
        )

        assertTrue(QualityStatus.SignalWeak in statuses)
    }

    @Test
    fun includesLightingFlickerWarning() {
        val statuses = QualityEvaluator().evaluate(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 30.0,
            ),
            lightingFlickerLikely = true,
        )

        assertTrue(QualityStatus.LightingFlicker in statuses)
    }

    @Test
    fun includesLightingUnstableWarning() {
        val statuses = QualityEvaluator().evaluate(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 30.0,
            ),
            lightingUnstable = true,
        )

        assertTrue(QualityStatus.LightingUnstable in statuses)
    }

    @Test
    fun detectsCameraMotionFromTranslationEstimate() {
        val statuses = QualityEvaluator().evaluate(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 30.0,
                translation = TranslationEstimate(dx = 0.03f, dy = 0.0f),
            ),
        )

        assertTrue(QualityStatus.CameraMotion in statuses)
        assertEquals("ROI motion", QualityStatus.CameraMotion.label)
    }

    @Test
    fun warnsWhenHighFrequencyModeHasSubtleCameraMotion() {
        val statuses = QualityEvaluator().evaluate(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 30.0,
                translation = TranslationEstimate(dx = 0.009f, dy = 0.0f),
            ),
            settings = AnalysisSettings(mode = MagnificationMode.Tremor),
        )

        assertTrue(QualityStatus.ModeMotionRisk in statuses)
    }

    @Test
    fun doesNotWarnPulseModeForHighFrequencyMotionThreshold() {
        val statuses = QualityEvaluator().evaluate(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 30.0,
                translation = TranslationEstimate(dx = 0.009f, dy = 0.0f),
            ),
            settings = AnalysisSettings(mode = MagnificationMode.Pulse),
        )

        assertTrue(QualityStatus.ModeMotionRisk !in statuses)
        assertTrue(QualityStatus.CameraMotion !in statuses)
    }

    @Test
    fun warnsWhenHighFrequencyModeUsesLargeAmplification() {
        val statuses = QualityEvaluator().evaluate(
            sample = AnalysisSample(
                roi = NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f),
                averageGreen = 120.0,
                bandpassedGreen = 0.2,
                analysisFps = 30.0,
            ),
            settings = AnalysisSettings(
                mode = MagnificationMode.ObjectVibration,
                amplification = 24.0f,
            ),
        )

        assertTrue(QualityStatus.AmplificationRisk in statuses)
    }

    @Test
    fun statusesExposeActionableGuidance() {
        assertEquals("Keep this setup.", QualityStatus.Good.action)
        assertEquals("Frame the face or select a manual ROI.", QualityStatus.FaceMissing.action)
        assertEquals("Use brighter, steady light.", QualityStatus.TooDark.action)
        assertEquals("Let the phone cool before validation.", QualityStatus.ThermalHigh.action)
        assertEquals("Close apps or reduce device load.", QualityStatus.LowFps.action)
        assertEquals("Switch to Auto ROI for live preview.", QualityStatus.FullFrameSlow.action)
        assertEquals("Hide controls or use Auto ROI.", QualityStatus.CameraFpsLow.action)
        assertEquals("Restart the preview if timing keeps jumping.", QualityStatus.TimingUnstable.action)
        assertEquals("Try daylight or a non-flickering lamp.", QualityStatus.LightingFlicker.action)
        assertEquals("Wait for exposure to settle, then lock AE/AWB.", QualityStatus.LightingUnstable.action)
        assertEquals("Mount the phone or redraw a stable ROI.", QualityStatus.CameraMotion.action)
        assertEquals("Use a tripod for high-frequency modes.", QualityStatus.ModeMotionRisk.action)
        assertEquals("Lower amplification below 18x.", QualityStatus.AmplificationRisk.action)
        assertEquals("Use steadier light or choose a stronger ROI.", QualityStatus.SignalWeak.action)
    }
}
