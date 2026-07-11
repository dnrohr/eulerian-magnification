package com.dnrohr.eulerianmagnification.quality

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.TranslationEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LightingStabilityAnalyzerTest {
    @Test
    fun reportsSettlingUntilEnoughSamplesArrive() {
        val analyzer = LightingStabilityAnalyzer()

        val diagnostic = analyzer.update(sample(100.0))

        assertEquals(LightingDiagnosticStatus.Settling, diagnostic.status)
        assertEquals("Wait a few seconds before locking AE/AWB.", diagnostic.action)
    }

    @Test
    fun reportsStableForConsistentBrightness() {
        val analyzer = LightingStabilityAnalyzer()
        repeat(8) { analyzer.update(sample(120.0 + it * 0.1)) }

        val diagnostic = analyzer.update(sample(120.2))

        assertEquals(LightingDiagnosticStatus.Stable, diagnostic.status)
        assertEquals("stable", diagnostic.metadataValue)
    }

    @Test
    fun reportsTooDarkBeforeOtherWarnings() {
        val analyzer = LightingStabilityAnalyzer()
        repeat(8) { analyzer.update(sample(30.0 + it * 0.1)) }

        val diagnostic = analyzer.update(sample(30.5))

        assertEquals(LightingDiagnosticStatus.TooDark, diagnostic.status)
    }

    @Test
    fun reportsAlternatingBrightnessAsFlicker() {
        val analyzer = LightingStabilityAnalyzer()
        listOf(100.0, 108.0, 99.0, 109.0, 98.0, 108.0, 99.0, 109.0, 98.0)
            .forEach { analyzer.update(sample(it)) }

        val diagnostic = analyzer.update(sample(108.0))

        assertEquals(LightingDiagnosticStatus.FlickerLikely, diagnostic.status)
        assertTrue(diagnostic.flickerLikely)
    }

    @Test
    fun separatesExposurePumpingFromRoiMotionContamination() {
        val exposureAnalyzer = LightingStabilityAnalyzer()
        listOf(95.0, 96.0, 97.0, 98.0, 116.0, 117.0, 118.0, 119.0, 120.0)
            .forEach { exposureAnalyzer.update(sample(it)) }

        assertEquals(
            LightingDiagnosticStatus.ExposurePumping,
            exposureAnalyzer.update(sample(120.0)).status,
        )

        val motionAnalyzer = LightingStabilityAnalyzer()
        listOf(95.0, 96.0, 97.0, 98.0, 116.0, 117.0, 118.0, 119.0, 120.0)
            .forEach { motionAnalyzer.update(sample(it, motion = 0.02f)) }

        assertEquals(
            LightingDiagnosticStatus.MotionContaminated,
            motionAnalyzer.update(sample(120.0, motion = 0.02f)).status,
        )
    }

    private fun sample(
        averageGreen: Double,
        motion: Float = 0.0f,
    ): AnalysisSample {
        return AnalysisSample(
            averageGreen = averageGreen,
            translation = TranslationEstimate(dx = motion, dy = 0.0f),
        )
    }
}
