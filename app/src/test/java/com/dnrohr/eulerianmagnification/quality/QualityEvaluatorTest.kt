package com.dnrohr.eulerianmagnification.quality

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
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
}
