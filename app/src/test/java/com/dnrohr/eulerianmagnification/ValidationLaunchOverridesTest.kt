package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.RoiSource
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationLaunchOverridesTest {
    @Test
    fun parsesValidationLaunchExtras() {
        val overrides = ValidationLaunchOverrides.fromMap(
            mapOf(
                ValidationLaunchOverrides.EXTRA_MODE to "ObjectVibration",
                ValidationLaunchOverrides.EXTRA_VIEW to "Split",
                ValidationLaunchOverrides.EXTRA_AMPLIFICATION to "18.5",
                ValidationLaunchOverrides.EXTRA_GL_PREVIEW to "yes",
                ValidationLaunchOverrides.EXTRA_ROI_SOURCE to "FullFrame",
                ValidationLaunchOverrides.EXTRA_MANUAL_ROI to "0.2,0.3,0.7,0.8",
                ValidationLaunchOverrides.EXTRA_CAMERA_LOCK to "on",
                ValidationLaunchOverrides.EXTRA_CONTROLS to "true",
                ValidationLaunchOverrides.EXTRA_CLEAN to "0",
                ValidationLaunchOverrides.EXTRA_PERSIST to "1",
            )
        )

        assertEquals(MagnificationMode.ObjectVibration, overrides.mode)
        assertEquals(ViewMode.Split, overrides.viewMode)
        assertEquals(18.5f, overrides.amplification)
        assertEquals(true, overrides.requestedGlPreview)
        assertEquals(RoiSource.FullFrame, overrides.roiSource)
        assertEquals(0.2f, overrides.manualRoi?.left)
        assertEquals(0.3f, overrides.manualRoi?.top)
        assertEquals(0.7f, overrides.manualRoi?.right)
        assertEquals(0.8f, overrides.manualRoi?.bottom)
        assertEquals(true, overrides.cameraControlsLocked)
        assertEquals(true, overrides.controlsExpanded)
        assertEquals(false, overrides.cleanPreview)
        assertTrue(overrides.persistSettings)
        assertTrue(overrides.hasAnyOverride)
    }

    @Test
    fun clampsAmplificationAndIgnoresInvalidValues() {
        val high = ValidationLaunchOverrides.fromMap(
            mapOf(ValidationLaunchOverrides.EXTRA_AMPLIFICATION to "99")
        )
        val invalid = ValidationLaunchOverrides.fromMap(
            mapOf(
                ValidationLaunchOverrides.EXTRA_MODE to "missing",
                ValidationLaunchOverrides.EXTRA_VIEW to "missing",
                ValidationLaunchOverrides.EXTRA_AMPLIFICATION to "nope",
                ValidationLaunchOverrides.EXTRA_GL_PREVIEW to "maybe",
                ValidationLaunchOverrides.EXTRA_ROI_SOURCE to "missing",
                ValidationLaunchOverrides.EXTRA_MANUAL_ROI to "0.8,0.2,0.1,0.5",
            )
        )

        assertEquals(30.0f, high.amplification)
        assertNull(invalid.mode)
        assertNull(invalid.viewMode)
        assertNull(invalid.amplification)
        assertNull(invalid.requestedGlPreview)
        assertNull(invalid.roiSource)
        assertNull(invalid.manualRoi)
        assertFalse(invalid.hasAnyOverride)
    }

    @Test
    fun defaultsToNonPersistentWithoutOverrides() {
        val overrides = ValidationLaunchOverrides.fromMap(emptyMap())

        assertFalse(overrides.hasAnyOverride)
        assertFalse(overrides.persistSettings)
    }
}
