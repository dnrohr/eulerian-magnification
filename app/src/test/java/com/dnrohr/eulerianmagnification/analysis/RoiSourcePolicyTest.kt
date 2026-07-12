package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoiSourcePolicyTest {
    @Test
    fun defaultsAllModesToAutoRoi() {
        assertEquals(RoiSource.Auto, RoiSourcePolicy.defaultFor(MagnificationMode.ObjectVibration))
        assertEquals(RoiSource.Auto, RoiSourcePolicy.defaultFor(MagnificationMode.Tremor))
        assertEquals(RoiSource.Auto, RoiSourcePolicy.defaultFor(MagnificationMode.Breathing))
        assertEquals(RoiSource.Auto, RoiSourcePolicy.defaultFor(MagnificationMode.Pulse))
    }

    @Test
    fun activeRoiUsesSelectedSource() {
        val auto = NormalizedRect(0.1f, 0.2f, 0.3f, 0.4f)
        val manual = NormalizedRect(0.2f, 0.3f, 0.4f, 0.5f)

        assertEquals(auto, RoiSourcePolicy.activeRoi(RoiSource.Auto, auto, manual))
        assertEquals(RoiSourcePolicy.FULL_FRAME_ROI, RoiSourcePolicy.activeRoi(RoiSource.FullFrame, auto, manual))
        assertEquals(manual, RoiSourcePolicy.activeRoi(RoiSource.Manual, auto, manual))
        assertNull(RoiSourcePolicy.activeRoi(RoiSource.Manual, auto, null))
    }

    @Test
    fun labelsSelectedSourcePlainly() {
        assertEquals("Tracking", RoiSourcePolicy.labelFor(RoiSource.Auto, RoiState.Tracking))
        assertEquals("Full frame", RoiSourcePolicy.labelFor(RoiSource.FullFrame, RoiState.Tracking))
        assertEquals("Manual ROI", RoiSourcePolicy.labelFor(RoiSource.Manual, RoiState.Tracking))
    }
}
