package com.dnrohr.eulerianmagnification

import org.junit.Assert.assertTrue
import org.junit.Test

class ParityPresetBenchmarkReportTest {
    @Test
    fun writesCsvAndJsonRows() {
        val report = ParityPresetBenchmarkReport(
            deviceModel = "Pixel 8a",
            androidVersion = "16",
            rows = listOf(
                ParityPresetBenchmarkRow(
                    preset = ParityPreset.PulseColor,
                    measuredFrames = 120,
                    jankyFrames = 3,
                    medianFrameMillis = 16.7,
                    p90FrameMillis = 20.0,
                    p95FrameMillis = 24.0,
                    p99FrameMillis = 32.0,
                    thermalStatus = "none",
                    recordingSampleCount = 30,
                    recordingDroppedFrameEstimate = 0,
                    recordingStability = "metadata ok",
                    encodedMp4Valid = true,
                    encodedMp4Bytes = 2048L,
                )
            ),
        )

        val csv = report.toCsv()
        val json = report.toJson()

        assertTrue(csv.contains("Pulse color"))
        assertTrue(csv.contains("jankyPercent"))
        assertTrue(json.contains("\"deviceModel\": \"Pixel 8a\""))
        assertTrue(json.contains("\"frames\": 120"))
        assertTrue(json.contains("\"recordingSampleCount\": 30"))
        assertTrue(json.contains("\"recordingDroppedFrameEstimate\": 0"))
        assertTrue(json.contains("\"recordingStability\": \"metadata ok\""))
        assertTrue(json.contains("\"encodedMp4Valid\": true"))
        assertTrue(json.contains("\"encodedMp4Bytes\": 2048"))
    }
}
