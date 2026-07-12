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
                    recordingStability = "not exercised",
                )
            ),
        )

        val csv = report.toCsv()
        val json = report.toJson()

        assertTrue(csv.contains("Pulse color"))
        assertTrue(csv.contains("jankyPercent"))
        assertTrue(json.contains("\"deviceModel\": \"Pixel 8a\""))
        assertTrue(json.contains("\"frames\": 120"))
        assertTrue(json.contains("\"recordingStability\": \"not exercised\""))
    }
}
