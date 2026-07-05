package com.dnrohr.eulerianmagnification.capabilities

import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityAvailabilityTest {
    @Test
    fun pixelClassReportEnablesLiveGlRecordingAndAllModes() {
        val availability = CapabilityAvailability.fromReport(
            report(
                cameras = JSONArray()
                    .put(frontCamera("15-30"))
                    .put(JSONObject().put("lensFacing", "back").put("fpsRanges", JSONArray().put("15-60"))),
                encoders = JSONArray().put(encoder("video/avc", "video/hevc")),
                supportsGles3 = true,
            ),
        )

        assertTrue(availability.liveCameraAvailable)
        assertTrue(availability.glPreviewAvailable)
        assertTrue(availability.processedRecordingAvailable)
        assertEquals(
            listOf(MagnificationMode.Pulse, MagnificationMode.Breathing, MagnificationMode.Tremor),
            availability.availableModes,
        )
    }

    @Test
    fun missingFrontCameraDisablesLiveFeaturesAndModes() {
        val availability = CapabilityAvailability.fromReport(
            report(
                cameras = JSONArray().put(JSONObject().put("lensFacing", "back").put("fpsRanges", JSONArray().put("15-30"))),
                encoders = JSONArray().put(encoder("video/avc")),
                supportsGles3 = true,
            ),
        )

        assertFalse(availability.liveCameraAvailable)
        assertFalse(availability.glPreviewAvailable)
        assertFalse(availability.processedRecordingAvailable)
        assertEquals(emptyList<MagnificationMode>(), availability.availableModes)
    }

    @Test
    fun lowFpsFrontCameraKeepsPulseAndBreathingOnly() {
        val availability = CapabilityAvailability.fromReport(
            report(
                cameras = JSONArray().put(frontCamera("15-24")),
                encoders = JSONArray().put(encoder("video/avc")),
                supportsGles3 = true,
            ),
        )

        assertEquals(
            listOf(MagnificationMode.Pulse, MagnificationMode.Breathing),
            availability.availableModes,
        )
    }

    @Test
    fun missingGles3OrH264HidesOnlyRelatedControls() {
        val availability = CapabilityAvailability.fromReport(
            report(
                cameras = JSONArray().put(frontCamera("15-30")),
                encoders = JSONArray().put(encoder("video/hevc")),
                supportsGles3 = false,
            ),
        )

        assertTrue(availability.liveCameraAvailable)
        assertFalse(availability.glPreviewAvailable)
        assertFalse(availability.processedRecordingAvailable)
        assertTrue(availability.recordedVideoValidationAvailable)
    }

    private fun report(
        cameras: JSONArray,
        encoders: JSONArray,
        supportsGles3: Boolean,
    ): JSONObject {
        return JSONObject()
            .put("graphics", JSONObject().put("supportsGles3", supportsGles3))
            .put("cameras", cameras)
            .put("encoders", encoders)
    }

    private fun frontCamera(vararg ranges: String): JSONObject {
        val fpsRanges = JSONArray()
        ranges.forEach { fpsRanges.put(it) }
        return JSONObject()
            .put("lensFacing", "front")
            .put("fpsRanges", fpsRanges)
    }

    private fun encoder(vararg types: String): JSONObject {
        val supportedTypes = JSONArray()
        types.forEach { supportedTypes.put(it) }
        return JSONObject().put("types", supportedTypes)
    }
}
