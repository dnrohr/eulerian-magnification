package com.dnrohr.eulerianmagnification

import android.Manifest
import android.content.ContentValues
import android.os.Build
import android.os.PowerManager
import android.provider.MediaStore
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.RecordedVideoProcessor
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import com.dnrohr.eulerianmagnification.analysis.RoiSource
import com.dnrohr.eulerianmagnification.analysis.RgbFrame
import com.dnrohr.eulerianmagnification.recording.EncodedOutputValidator
import com.dnrohr.eulerianmagnification.recording.ProcessedRecordingSession
import com.dnrohr.eulerianmagnification.recording.RecordedVideoMp4Exporter
import com.dnrohr.eulerianmagnification.recording.RecordingOutputMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class ParityPresetBenchmarkInstrumentedTest {
    @Test
    fun writesPresetBenchmarkArtifacts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.CAMERA)
        val requestedOutputRoot = InstrumentationRegistry.getArguments().getString("outputDirPath")?.let(::File)
        val outputRoot = writableOutputRoot(
            requested = requestedOutputRoot,
            fallback = File(context.getExternalFilesDir(null), "preset-benchmark"),
        )

        val rows = ParityPreset.entries.map { preset ->
            savePreset(preset)
            shell("dumpsys gfxinfo ${context.packageName} reset")
            ActivityScenario.launch(MainActivity::class.java).use {
                Thread.sleep(BENCHMARK_WINDOW_MILLIS)
            }
            val gfxInfo = shell("dumpsys gfxinfo ${context.packageName}")
            val recordingProbe = runRecordingProbe(preset, outputRoot)
            val encodedProbe = runEncodedProbe(preset, outputRoot)
            ParityPresetBenchmarkRow(
                preset = preset,
                measuredFrames = gfxInfo.intAfter("Total frames rendered:") ?: 0,
                jankyFrames = gfxInfo.intAfter("Janky frames:") ?: 0,
                medianFrameMillis = gfxInfo.percentile("50th"),
                p90FrameMillis = gfxInfo.percentile("90th"),
                p95FrameMillis = gfxInfo.percentile("95th"),
                p99FrameMillis = gfxInfo.percentile("99th"),
                thermalStatus = currentThermalStatus(),
                recordingSampleCount = recordingProbe.sampleCount,
                recordingDroppedFrameEstimate = recordingProbe.droppedFrameEstimate,
                recordingStability = recordingProbe.status,
                encodedMp4Valid = encodedProbe.valid,
                encodedMp4Bytes = encodedProbe.bytes,
            )
        }

        val report = ParityPresetBenchmarkReport(
            deviceModel = Build.MODEL ?: "unknown",
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            rows = rows,
        )
        val csvFile = File(outputRoot, "preset_benchmark.csv")
        val jsonFile = File(outputRoot, "preset_benchmark.json")
        val csvText = report.toCsv()
        val jsonText = report.toJson()
        csvFile.writeText(csvText)
        jsonFile.writeText(jsonText)
        writeDownloadArtifact("preset_benchmark.csv", csvText)
        writeDownloadArtifact("preset_benchmark.json", jsonText)
        assertTrue(
            "public CSV mirror should include recording fields",
            shell("cat $PUBLIC_OUTPUT_DIR/preset_benchmark.csv").contains("recordingSampleCount"),
        )

        assertEquals(ParityPreset.entries.size, rows.size)
        assertTrue("CSV artifact should exist", csvFile.isFile)
        assertTrue("JSON artifact should exist", jsonFile.isFile)
        assertTrue("each preset should produce a row", rows.all { it.preset in ParityPreset.entries })
        assertTrue("recording metadata probe should be stable", rows.all { it.recordingDroppedFrameEstimate == 0 })
        assertTrue("encoded probe should produce valid MP4 output", rows.all { it.encodedMp4Valid && it.encodedMp4Bytes > 512L })
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

    private fun writableOutputRoot(
        requested: File?,
        fallback: File,
    ): File {
        val candidates = listOfNotNull(requested, fallback)
        for (candidate in candidates) {
            runCatching {
                candidate.mkdirs()
                val probe = File(candidate, ".write-probe")
                probe.writeText("ok")
                probe.delete()
                return candidate
            }
        }
        error("No writable benchmark output directory")
    }

    private fun shell(command: String): String {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        return automation.executeShellCommand(command).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
        }
    }

    private fun writeDownloadArtifact(
        fileName: String,
        contents: String,
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, if (fileName.endsWith(".json")) "application/json" else "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/eulerian-preset-benchmark")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) {
            "Could not create $fileName in Downloads"
        }
        resolver.openOutputStream(uri).use { output ->
            requireNotNull(output) { "Could not open $fileName in Downloads" }
            output.write(contents.toByteArray(Charsets.UTF_8))
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    private fun runRecordingProbe(
        preset: ParityPreset,
        outputRoot: File,
    ): RecordingProbe {
        val session = ProcessedRecordingSession(
            rootDirectory = File(outputRoot, "recording-probes/${preset.name.lowercase()}"),
            requestedOutputMode = RecordingOutputMode.Clean,
        )
        repeat(RECORDING_PROBE_SAMPLES) { index ->
            session.record(
                AnalysisSample(
                    analysisFps = 30.0,
                    latencyMillis = 16.0,
                    roi = NormalizedRect(0.25f, 0.25f, 0.75f, 0.75f),
                    averageGreen = 128.0,
                    bandpassedGreen = 0.01 * (index % 5),
                    frameTimestampNanos = index * FRAME_INTERVAL_NANOS,
                ),
                preset.settings,
            )
        }
        val metadata = session.stop(
            settings = preset.settings,
            thermalStatus = currentThermalStatus(),
        )
        return RecordingProbe(
            sampleCount = RECORDING_PROBE_SAMPLES,
            droppedFrameEstimate = session.droppedFrameEstimate,
            status = if (metadata.isFile && session.droppedFrameEstimate == 0) {
                "metadata ok"
            } else {
                "metadata issue"
            },
        )
    }

    private fun runEncodedProbe(
        preset: ParityPreset,
        outputRoot: File,
    ): EncodedProbe {
        val outputFile = File(outputRoot, "encoded-probes/${preset.name.lowercase()}.mp4")
        outputFile.parentFile?.mkdirs()
        outputFile.delete()
        val processed = RecordedVideoProcessor(preset.settings)
            .process(syntheticClip(preset))
            .processedFrames
        RecordedVideoMp4Exporter(
            bitrate = 700_000,
            frameRate = 30,
        ).export(processed, outputFile)
        val validation = EncodedOutputValidator().validate(outputFile)
        return EncodedProbe(
            valid = validation.isValid,
            bytes = outputFile.length(),
        )
    }

    private fun syntheticClip(preset: ParityPreset): List<RgbFrame> {
        val frequency = when (preset) {
            ParityPreset.PulseColor -> 1.2
            ParityPreset.BreathingSlowMotion -> 0.25
            ParityPreset.ObjectVibration,
            ParityPreset.FastTremor -> 6.0
        }
        return (0 until ENCODED_PROBE_FRAMES).map { frameIndex ->
            val seconds = frameIndex / ENCODED_PROBE_FPS
            val timestampNanos = (seconds * NANOS_PER_SECOND).toLong()
            val pixels = IntArray(ENCODED_PROBE_WIDTH * ENCODED_PROBE_HEIGHT) { rgb(80, 80, 80) }
            if (preset == ParityPreset.PulseColor || preset == ParityPreset.BreathingSlowMotion) {
                val green = 128 + (12.0 * sin(2.0 * PI * frequency * seconds)).roundToInt()
                for (y in 16 until 32) {
                    for (x in 22 until 42) {
                        pixels[y * ENCODED_PROBE_WIDTH + x] = rgb(96, green, 96)
                    }
                }
            } else {
                val edgeOffset = (2.0 * sin(2.0 * PI * frequency * seconds)).roundToInt()
                val edgeX = (ENCODED_PROBE_WIDTH / 2 + edgeOffset).coerceIn(1, ENCODED_PROBE_WIDTH - 2)
                for (y in 0 until ENCODED_PROBE_HEIGHT) {
                    for (x in edgeX until ENCODED_PROBE_WIDTH) {
                        pixels[y * ENCODED_PROBE_WIDTH + x] = rgb(180, 180, 180)
                    }
                }
            }
            RgbFrame(
                width = ENCODED_PROBE_WIDTH,
                height = ENCODED_PROBE_HEIGHT,
                timestampNanos = timestampNanos,
                pixels = pixels,
            )
        }
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (0xFF shl 24) or
            (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
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

    private data class RecordingProbe(
        val sampleCount: Int,
        val droppedFrameEstimate: Int,
        val status: String,
    )

    private data class EncodedProbe(
        val valid: Boolean,
        val bytes: Long,
    )

    private companion object {
        private const val BENCHMARK_WINDOW_MILLIS = 4_000L
        private const val RECORDING_PROBE_SAMPLES = 30
        private const val FRAME_INTERVAL_NANOS = 33_333_333L
        private const val PUBLIC_OUTPUT_DIR = "/sdcard/Download/eulerian-preset-benchmark"
        private const val ENCODED_PROBE_WIDTH = 64
        private const val ENCODED_PROBE_HEIGHT = 48
        private const val ENCODED_PROBE_FRAMES = 30
        private const val ENCODED_PROBE_FPS = 30.0
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
