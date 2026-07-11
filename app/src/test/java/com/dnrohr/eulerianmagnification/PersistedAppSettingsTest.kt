package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistedAppSettingsTest {
    @Test
    fun defaultsUseFirstAvailableModeAndDoNotPersistTransientState() {
        val settings = PersistedAppSettings.defaultFor(listOf(MagnificationMode.Breathing))

        assertEquals(MagnificationMode.Breathing, settings.analysisSettings.mode)
        assertEquals(ViewMode.Amplified, settings.analysisSettings.viewMode)
        assertEquals(12.0f, settings.analysisSettings.amplification)
        assertFalse(settings.requestedGlPreview)
        assertFalse(settings.cameraControlsLocked)
        assertFalse(settings.toMap().containsKey("manualRoi"))
    }

    @Test
    fun roundTripsDurableSettings() {
        val original = PersistedAppSettings(
            analysisSettings = AnalysisSettings(
                mode = MagnificationMode.Tremor,
                viewMode = ViewMode.Split,
                amplification = 22.0f,
            ),
            requestedGlPreview = true,
            cameraControlsLocked = true,
        )

        val restored = PersistedAppSettings.fromMap(original.toMap())

        assertEquals(original, restored)
    }

    @Test
    fun invalidStoredValuesFallBackToDefaults() {
        val restored = PersistedAppSettings.fromMap(
            values = mapOf(
                PersistedAppSettings.KEY_MODE to "Nope",
                PersistedAppSettings.KEY_VIEW_MODE to "AlsoNope",
                PersistedAppSettings.KEY_AMPLIFICATION to "bad",
                PersistedAppSettings.KEY_REQUESTED_GL_PREVIEW to "not-bool",
                PersistedAppSettings.KEY_CAMERA_CONTROLS_LOCKED to "not-bool",
            ),
            availableModes = listOf(MagnificationMode.Pulse),
        )

        assertEquals(PersistedAppSettings.defaultFor(listOf(MagnificationMode.Pulse)), restored)
    }

    @Test
    fun unavailableModeFallsBackToAvailableDefault() {
        val restored = PersistedAppSettings.fromMap(
            values = mapOf(PersistedAppSettings.KEY_MODE to MagnificationMode.ObjectVibration.name),
            availableModes = listOf(MagnificationMode.Pulse),
        )

        assertEquals(MagnificationMode.Pulse, restored.analysisSettings.mode)
    }

    @Test
    fun amplificationIsClampedToUiRange() {
        val restored = PersistedAppSettings.fromMap(
            values = mapOf(PersistedAppSettings.KEY_AMPLIFICATION to "99.0"),
        )

        assertEquals(30.0f, restored.analysisSettings.amplification)
    }

    @Test
    fun resetDefaultsAreStable() {
        val defaults = PersistedAppSettings.defaultFor()

        assertEquals(MagnificationMode.Pulse, defaults.analysisSettings.mode)
        assertEquals(ViewMode.Amplified, defaults.analysisSettings.viewMode)
        assertEquals(12.0f, defaults.analysisSettings.amplification)
    }

    @Test
    fun trueBooleansRestorePreferences() {
        val restored = PersistedAppSettings.fromMap(
            values = mapOf(
                PersistedAppSettings.KEY_REQUESTED_GL_PREVIEW to "true",
                PersistedAppSettings.KEY_CAMERA_CONTROLS_LOCKED to "true",
            ),
        )

        assertTrue(restored.requestedGlPreview)
        assertTrue(restored.cameraControlsLocked)
    }
}
