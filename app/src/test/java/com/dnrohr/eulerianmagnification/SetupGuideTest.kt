package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupGuideTest {
    @Test
    fun pulseGuideUsesSkinTargetAndColorExpectation() {
        val guide = SetupGuide.forMode(MagnificationMode.Pulse)

        assertEquals("Pulse setup", guide.title)
        assertTrue(guide.target.contains("forehead"))
        assertTrue(guide.expected.contains("color pulse"))
    }

    @Test
    fun breathingGuideDoesNotClaimPreviewWarping() {
        val guide = SetupGuide.forMode(MagnificationMode.Breathing)

        assertEquals("Breathing setup", guide.title)
        assertTrue(guide.target.contains("torso"))
        assertTrue(guide.expected.contains("not motion-warped yet"))
    }

    @Test
    fun fastMotionGuideUsesHighContrastTarget() {
        val guide = SetupGuide.forMode(MagnificationMode.Tremor)

        assertEquals("Fast motion setup", guide.title)
        assertTrue(guide.target.contains("high-contrast edge"))
        assertTrue(guide.expected.contains("camera shake"))
    }
}
