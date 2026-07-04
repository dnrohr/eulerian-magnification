package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RoiTrackerTest {
    @Test
    fun predictsMotionBetweenDetections() {
        val tracker = RoiTracker(damping = 1.0f, maxStep = 0.10f)

        tracker.updateDetection(NormalizedRect(0.10f, 0.10f, 0.30f, 0.30f))
        tracker.updateDetection(NormalizedRect(0.15f, 0.12f, 0.35f, 0.32f))
        val predicted = tracker.predict()

        assertNotNull(predicted)
        assertEquals(0.20f, predicted!!.left, 0.001f)
        assertEquals(0.14f, predicted.top, 0.001f)
    }

    @Test
    fun dampsRepeatedPredictions() {
        val tracker = RoiTracker(damping = 0.50f, maxStep = 0.10f)

        tracker.updateDetection(NormalizedRect(0.10f, 0.10f, 0.30f, 0.30f))
        tracker.updateDetection(NormalizedRect(0.20f, 0.10f, 0.40f, 0.30f))
        val first = tracker.predict()
        val second = tracker.predict()

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(0.25f, first!!.left, 0.001f)
        assertEquals(0.275f, second!!.left, 0.001f)
    }

    @Test
    fun clampsPredictionInsideFrame() {
        val tracker = RoiTracker(damping = 1.0f, maxStep = 0.20f)

        tracker.updateDetection(NormalizedRect(0.70f, 0.70f, 0.95f, 0.95f))
        tracker.updateDetection(NormalizedRect(0.80f, 0.80f, 1.00f, 1.00f))
        val predicted = tracker.predict()

        assertNotNull(predicted)
        assertEquals(1.0f, predicted!!.right, 0.001f)
        assertEquals(1.0f, predicted.bottom, 0.001f)
    }
}
