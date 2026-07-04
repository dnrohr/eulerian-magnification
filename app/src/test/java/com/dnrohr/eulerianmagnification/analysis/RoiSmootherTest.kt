package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class RoiSmootherTest {
    @Test
    fun smoothsSmallRoiMovement() {
        val smoother = RoiSmoother(smoothing = 0.75f)

        smoother.update(NormalizedRect(0.10f, 0.10f, 0.40f, 0.40f))
        val smoothed = smoother.update(NormalizedRect(0.14f, 0.14f, 0.44f, 0.44f))

        assertEquals(0.11f, smoothed.left, 0.001f)
        assertEquals(0.41f, smoothed.right, 0.001f)
    }

    @Test
    fun resetsAfterLargeJump() {
        val smoother = RoiSmoother(jumpResetThreshold = 0.20f)

        smoother.update(NormalizedRect(0.10f, 0.10f, 0.40f, 0.40f))
        val smoothed = smoother.update(NormalizedRect(0.55f, 0.55f, 0.85f, 0.85f))

        assertEquals(0.55f, smoothed.left, 0.0f)
        assertEquals(0.85f, smoothed.right, 0.0f)
    }
}
