package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.RoiSource
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import com.dnrohr.eulerianmagnification.recording.RecordingOutputMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistedAppSettingsTest {
    @Test
    fun defaultsPreferMotionModeAndGlWhenAvailable() {
        val settings = PersistedAppSettings.defaultFor(MagnificationMode.entries)

        assertEquals(MagnificationMode.ObjectVibration, settings.analysisSettings.mode)
        assertEquals(ViewMode.Amplified, settings.analysisSettings.viewMode)
        assertEquals(12.0f, settings.analysisSettings.amplification)
        assertTrue(settings.requestedGlPreview)
        assertFalse(settings.cameraControlsLocked)
        assertFalse(settings.qualityCuesEnabled)
        assertEquals(RecordingOutputMode.Clean, settings.recordingOutputMode)
        assertEquals(RoiSource.FullFrame, settings.roiSource)
        assertFalse(settings.toMap().containsKey("manualRoi"))
    }

    @Test
    fun defaultsFallBackToAvailableNonMotionMode() {
        val settings = PersistedAppSettings.defaultFor(listOf(MagnificationMode.Breathing))

        assertEquals(MagnificationMode.Breathing, settings.analysisSettings.mode)
        assertFalse(settings.requestedGlPreview)
        assertEquals(RoiSource.FullFrame, settings.roiSource)
    }

    @Test
    fun pulseOnlyDefaultsToAutoRoi() {
        val settings = PersistedAppSettings.defaultFor(listOf(MagnificationMode.Pulse))

        assertEquals(RoiSource.Auto, settings.roiSource)
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
            qualityCuesEnabled = true,
            recordingOutputMode = RecordingOutputMode.Annotated,
            roiSource = RoiSource.Manual,
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
                PersistedAppSettings.KEY_QUALITY_CUES_ENABLED to "not-bool",
                PersistedAppSettings.KEY_RECORDING_OUTPUT_MODE to "nope",
                PersistedAppSettings.KEY_ROI_SOURCE to "nope",
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

        assertEquals(MagnificationMode.ObjectVibration, defaults.analysisSettings.mode)
        assertEquals(ViewMode.Amplified, defaults.analysisSettings.viewMode)
        assertEquals(12.0f, defaults.analysisSettings.amplification)
        assertTrue(defaults.requestedGlPreview)
        assertEquals(RecordingOutputMode.Clean, defaults.recordingOutputMode)
        assertEquals(RoiSource.FullFrame, defaults.roiSource)
    }

    @Test
    fun recordingOutputModeRestoresPreference() {
        val restored = PersistedAppSettings.fromMap(
            values = mapOf(PersistedAppSettings.KEY_RECORDING_OUTPUT_MODE to RecordingOutputMode.Annotated.name),
        )

        assertEquals(RecordingOutputMode.Annotated, restored.recordingOutputMode)
    }

    @Test
    fun roiSourceRestoresPreference() {
        val restored = PersistedAppSettings.fromMap(
            values = mapOf(PersistedAppSettings.KEY_ROI_SOURCE to RoiSource.Manual.name),
        )

        assertEquals(RoiSource.Manual, restored.roiSource)
    }

    @Test
    fun trueBooleansRestorePreferences() {
        val restored = PersistedAppSettings.fromMap(
            values = mapOf(
                PersistedAppSettings.KEY_REQUESTED_GL_PREVIEW to "true",
                PersistedAppSettings.KEY_CAMERA_CONTROLS_LOCKED to "true",
                PersistedAppSettings.KEY_QUALITY_CUES_ENABLED to "true",
            ),
        )

        assertTrue(restored.requestedGlPreview)
        assertTrue(restored.cameraControlsLocked)
        assertTrue(restored.qualityCuesEnabled)
    }
}
