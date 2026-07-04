package com.dnrohr.eulerianmagnification.quality

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
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
}
