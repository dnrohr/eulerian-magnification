package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationEstimatorTest {
    @Test
    fun returnsZeroUntilTwoRoisExist() {
        val estimator = TranslationEstimator()

        val first = estimator.update(NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f))

        assertEquals(0.0f, first.magnitude, 0.0f)
    }

    @Test
    fun estimatesCenterTranslation() {
        val estimator = TranslationEstimator(smoothing = 0.0f)

        estimator.update(NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f))
        val estimate = estimator.update(NormalizedRect(0.2f, 0.15f, 0.4f, 0.35f))

        assertEquals(0.1f, estimate.dx, 0.001f)
        assertEquals(0.05f, estimate.dy, 0.001f)
    }

    @Test
    fun smoothsTranslation() {
        val estimator = TranslationEstimator(smoothing = 0.5f)

        estimator.update(NormalizedRect(0.1f, 0.1f, 0.3f, 0.3f))
        val estimate = estimator.update(NormalizedRect(0.3f, 0.1f, 0.5f, 0.3f))

        assertEquals(0.1f, estimate.dx, 0.001f)
        assertEquals(0.0f, estimate.dy, 0.001f)
    }
}
