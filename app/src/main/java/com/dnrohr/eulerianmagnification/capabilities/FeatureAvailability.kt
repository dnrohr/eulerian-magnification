package com.dnrohr.eulerianmagnification.capabilities

import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import org.json.JSONObject

data class FeatureAvailability(
    val liveCameraAvailable: Boolean,
    val glPreviewAvailable: Boolean,
    val processedRecordingAvailable: Boolean,
    val recordedVideoValidationAvailable: Boolean,
    val availableModes: List<MagnificationMode>,
) {
    companion object {
        val unavailable = FeatureAvailability(
            liveCameraAvailable = false,
            glPreviewAvailable = false,
            processedRecordingAvailable = false,
            recordedVideoValidationAvailable = true,
            availableModes = emptyList(),
        )
    }
}

object CapabilityAvailability {
    fun fromReport(report: JSONObject): FeatureAvailability {
        val cameras = report.optJSONArray("cameras")
        val hasFrontCamera = cameras.anyObject { camera ->
            camera.optString("lensFacing") == "front"
        }
        val hasThirtyFpsFrontCamera = cameras.anyObject { camera ->
            camera.optString("lensFacing") == "front" &&
                camera.optJSONArray("fpsRanges").anyString { range ->
                    range.substringAfter('-', range).toIntOrNull()?.let { it >= 30 } == true
                }
        }
        val supportsGles3 = report.optJSONObject("graphics")?.optBoolean("supportsGles3") == true
        val hasH264Encoder = report.optJSONArray("encoders").anyObject { encoder ->
            encoder.optJSONArray("types").anyString { it.equals("video/avc", ignoreCase = true) }
        }

        return FeatureAvailability(
            liveCameraAvailable = hasFrontCamera,
            glPreviewAvailable = hasFrontCamera && supportsGles3,
            processedRecordingAvailable = hasFrontCamera && hasH264Encoder,
            recordedVideoValidationAvailable = true,
            availableModes = buildList {
                if (hasFrontCamera) {
                    add(MagnificationMode.Pulse)
                    add(MagnificationMode.Breathing)
                }
                if (hasThirtyFpsFrontCamera) {
                    add(MagnificationMode.Tremor)
                }
            },
        )
    }
}

private inline fun org.json.JSONArray?.anyObject(predicate: (JSONObject) -> Boolean): Boolean {
    if (this == null) return false
    for (index in 0 until length()) {
        val item = optJSONObject(index) ?: continue
        if (predicate(item)) return true
    }
    return false
}

private inline fun org.json.JSONArray?.anyString(predicate: (String) -> Boolean): Boolean {
    if (this == null) return false
    for (index in 0 until length()) {
        if (predicate(optString(index))) return true
    }
    return false
}
