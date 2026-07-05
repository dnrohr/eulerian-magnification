package com.dnrohr.eulerianmagnification.capabilities

import android.content.Context
import android.app.ActivityManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.util.Range
import android.util.Size
import org.json.JSONArray
import org.json.JSONObject

class CapabilityReporter(private val context: Context) {
    fun logSummary() {
        Log.i(TAG, buildReport().toString())
    }

    fun buildReport(): JSONObject {
        return JSONObject()
            .put("device", deviceInfo())
            .put("graphics", graphicsInfo())
            .put("cameras", cameraInfo())
            .put("encoders", encoderInfo())
            .put("power", powerInfo())
    }

    private fun deviceInfo(): JSONObject {
        return JSONObject()
            .put("manufacturer", Build.MANUFACTURER)
            .put("model", Build.MODEL)
            .put("device", Build.DEVICE)
            .put("sdk", Build.VERSION.SDK_INT)
            .put("release", Build.VERSION.RELEASE)
    }

    private fun graphicsInfo(): JSONObject {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val version = activityManager.deviceConfigurationInfo.reqGlEsVersion
        val major = version shr 16
        val minor = version and 0xffff
        return JSONObject()
            .put("glesVersion", "$major.$minor")
            .put("supportsGles3", major >= 3)
    }

    private fun cameraInfo(): JSONArray {
        val manager = context.getSystemService(CameraManager::class.java)
        val cameras = JSONArray()

        manager.cameraIdList.forEach { id ->
            val characteristics = manager.getCameraCharacteristics(id)
            val configMap = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
            )
            cameras.put(
                JSONObject()
                    .put("id", id)
                    .put("lensFacing", lensFacingName(characteristics))
                    .put("previewSizes", sizesToJson(configMap?.getOutputSizes(SurfaceTexture::class.java)))
                    .put("fpsRanges", rangesToJson(characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)))
                    .put("highSpeedVideoSizes", sizesToJson(configMap?.highSpeedVideoSizes))
                    .put("stabilizationModes", intArrayToJson(characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES))),
            )
        }

        return cameras
    }

    private fun encoderInfo(): JSONArray {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val encoders = JSONArray()

        codecList.codecInfos
            .filter(MediaCodecInfo::isEncoder)
            .forEach { codec ->
                encoders.put(
                    JSONObject()
                        .put("name", codec.name)
                        .put("types", JSONArray(codec.supportedTypes.toList())),
                )
            }

        return encoders
    }

    private fun powerInfo(): JSONObject {
        val battery = context.getSystemService(BatteryManager::class.java)
        val power = context.getSystemService(PowerManager::class.java)
        return JSONObject()
            .put("batteryPercent", battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
            .put("powerSaveMode", power.isPowerSaveMode)
            .put("thermalStatus", currentThermalStatus(power))
    }

    private fun currentThermalStatus(power: PowerManager): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (power.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "none"
                PowerManager.THERMAL_STATUS_LIGHT -> "light"
                PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
                else -> "unknown"
            }
        } else {
            "unavailable"
        }
    }

    private fun lensFacingName(characteristics: CameraCharacteristics): String {
        return when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_FRONT -> "front"
            CameraCharacteristics.LENS_FACING_BACK -> "back"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "external"
            else -> "unknown"
        }
    }

    private fun sizesToJson(sizes: Array<Size>?): JSONArray {
        val json = JSONArray()
        sizes.orEmpty()
            .sortedWith(compareBy<Size> { it.width }.thenBy { it.height })
            .forEach { json.put("${it.width}x${it.height}") }
        return json
    }

    private fun rangesToJson(ranges: Array<Range<Int>>?): JSONArray {
        val json = JSONArray()
        ranges.orEmpty().forEach { json.put("${it.lower}-${it.upper}") }
        return json
    }

    private fun intArrayToJson(values: IntArray?): JSONArray {
        val json = JSONArray()
        (values ?: intArrayOf()).forEach { json.put(it) }
        return json
    }

    companion object {
        private const val TAG = "CapabilityReporter"
    }
}
