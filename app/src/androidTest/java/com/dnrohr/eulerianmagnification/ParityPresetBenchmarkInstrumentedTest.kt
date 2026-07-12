package com.dnrohr.eulerianmagnification

import android.Manifest
import android.os.Build
import android.os.PowerManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.dnrohr.eulerianmagnification.analysis.RoiSource
import com.dnrohr.eulerianmagnification.recording.RecordingOutputMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class ParityPresetBenchmarkInstrumentedTest {
    @Test
    fun writesPresetBenchmarkArtifacts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.CAMERA)
        val outputRoot = InstrumentationRegistry.getArguments().getString("outputDirPath")?.let(::File)
            ?: File(context.getExternalFilesDir(null), "preset-benchmark")
        outputRoot.mkdirs()

        val rows = ParityPreset.entries.map { preset ->
            savePreset(preset)
            shell("dumpsys gfxinfo ${context.packageName} reset")
            ActivityScenario.launch(MainActivity::class.java).use {
                Thread.sleep(BENCHMARK_WINDOW_MILLIS)
            }
            val gfxInfo = shell("dumpsys gfxinfo ${context.packageName}")
            ParityPresetBenchmarkRow(
                preset = preset,
                measuredFrames = gfxInfo.intAfter("Total frames rendered:") ?: 0,
                jankyFrames = gfxInfo.intAfter("Janky frames:") ?: 0,
                medianFrameMillis = gfxInfo.percentile("50th"),
                p90FrameMillis = gfxInfo.percentile("90th"),
                p95FrameMillis = gfxInfo.percentile("95th"),
                p99FrameMillis = gfxInfo.percentile("99th"),
                thermalStatus = currentThermalStatus(),
                recordingStability = "preview benchmark only; recording not exercised",
            )
        }

        val report = ParityPresetBenchmarkReport(
            deviceModel = Build.MODEL ?: "unknown",
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            rows = rows,
        )
        val csvFile = File(outputRoot, "preset_benchmark.csv")
        val jsonFile = File(outputRoot, "preset_benchmark.json")
        csvFile.writeText(report.toCsv())
        jsonFile.writeText(report.toJson())

        assertEquals(ParityPreset.entries.size, rows.size)
        assertTrue("CSV artifact should exist", csvFile.isFile)
        assertTrue("JSON artifact should exist", jsonFile.isFile)
        assertTrue("each preset should produce a row", rows.all { it.preset in ParityPreset.entries })
    }

    private fun savePreset(preset: ParityPreset) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val settings = PersistedAppSettings(
            analysisSettings = preset.settings,
            requestedGlPreview = true,
            cameraControlsLocked = true,
            qualityCuesEnabled = false,
            recordingOutputMode = RecordingOutputMode.Clean,
            roiSource = RoiSource.Auto,
        )
        AppSettingsStore(context).save(settings)
    }

    private fun shell(command: String): String {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        return automation.executeShellCommand(command).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
        }
    }

    private fun currentThermalStatus(): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val power = context.getSystemService(PowerManager::class.java)
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

    private fun String.intAfter(label: String): Int? {
        val line = lineSequence().firstOrNull { it.trimStart().startsWith(label) } ?: return null
        return line.substringAfter(label).trim().substringBefore(' ').toIntOrNull()
    }

    private fun String.percentile(label: String): Double? {
        val regex = Regex("${Regex.escape(label)} percentile: ([0-9.]+)ms")
        return regex.find(this)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
    }

    private companion object {
        private const val BENCHMARK_WINDOW_MILLIS = 4_000L
    }
}
