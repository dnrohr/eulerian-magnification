package com.dnrohr.eulerianmagnification.recording

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import com.dnrohr.eulerianmagnification.analysis.TranslationEstimate
import com.dnrohr.eulerianmagnification.analysis.VisualizationModel
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import com.dnrohr.eulerianmagnification.gl.GlTextureSize
import com.dnrohr.eulerianmagnification.gl.ProcessedGlFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProcessedRecordingSessionTest {
    @Test
    fun writesMetadataJsonWithSettingsAndSamples() {
        val directory = Files.createTempDirectory("recording-session").toFile()
        val session = ProcessedRecordingSession(directory, startedAtMillis = 1_700_000_000_000L)

        session.record(
            AnalysisSample(
                analysisFps = 30.0,
                roi = NormalizedRect(0.1f, 0.2f, 0.3f, 0.4f),
                averageGreen = 120.0,
                bandpassedGreen = 0.25,
                latencyMillis = 12.0,
                translation = TranslationEstimate(dx = 0.01f, dy = -0.02f),
                frameTimestampNanos = 100L,
            ),
        )
        val output = session.stop(
            settings = AnalysisSettings(mode = MagnificationMode.Breathing),
            thermalStatus = "none",
        )
        val json = output.readText()

        assertTrue(output.exists())
        assertTrue(json.contains("\"mode\": \"Breathing\""))
        assertTrue(json.contains("\"signalSource\": \"roi_vertical_translation\""))
        assertTrue(json.contains("\"renderer\": \"live_roi_signal_tint\""))
        assertTrue(json.contains("\"visualizationStyle\": \"roi_signal_overlay\""))
        assertTrue(json.contains("\"thermalStatus\": \"none\""))
        assertTrue(json.contains("\"sampleCount\": 1"))
        assertTrue(json.contains("\"bandpassedGreen\": 0.250000"))
        assertTrue(json.contains("\"presentationTimestampNanos\": 0"))
        assertTrue(json.contains("\"translation\": {\"dx\": 0.010000, \"dy\": -0.020000}"))
    }

    @Test
    fun estimatesDroppedFramesFromNonMonotonicTimestamps() {
        val directory = Files.createTempDirectory("recording-session").toFile()
        val session = ProcessedRecordingSession(directory)

        session.record(AnalysisSample(frameTimestampNanos = 200L))
        session.record(AnalysisSample(frameTimestampNanos = 100L))

        assertTrue(session.droppedFrameEstimate == 1)
    }

    @Test
    fun writesResolvedLiveVisualizationModelWhenProvided() {
        val directory = Files.createTempDirectory("recording-session").toFile()
        val session = ProcessedRecordingSession(directory, startedAtMillis = 1_700_000_000_000L)
        val settings = AnalysisSettings(
            mode = MagnificationMode.Pulse,
            viewMode = ViewMode.Split,
        )

        val output = session.stop(
            settings = settings,
            thermalStatus = "none",
            visualizationModel = VisualizationModel.live(
                settings = settings,
                fullFrameColorPreview = true,
            ),
        )
        val json = output.readText()

        assertTrue(json.contains("\"signalSource\": \"roi_green_bandpass\""))
        assertTrue(json.contains("\"renderer\": \"live_gl_full_frame_color_bridge\""))
        assertTrue(json.contains("\"visualizationStyle\": \"split_comparison\""))
    }

    @Test
    fun writesMonotonicPresentationTimestampsForNonMonotonicSourceFrames() {
        val directory = Files.createTempDirectory("recording-session").toFile()
        val session = ProcessedRecordingSession(directory)

        val first = session.record(AnalysisSample(frameTimestampNanos = 200L))
        val second = session.record(AnalysisSample(frameTimestampNanos = 100L))
        val third = session.record(AnalysisSample(frameTimestampNanos = 300L))

        val output = session.stop(
            settings = AnalysisSettings(),
            thermalStatus = "none",
        )
        val json = output.readText()

        assertEquals(0L, first.presentationTimestampNanos)
        assertEquals(33_333_333L, second.presentationTimestampNanos)
        assertEquals(66_666_666L, third.presentationTimestampNanos)
        assertTrue(json.contains("\"timestampNanos\": 200"))
        assertTrue(json.contains("\"timestampNanos\": 100"))
        assertTrue(json.contains("\"presentationTimestampNanos\": 0"))
        assertTrue(json.contains("\"presentationTimestampNanos\": 33333333"))
        assertTrue(json.contains("\"presentationTimestampNanos\": 66666666"))
    }

    @Test
    fun forwardsSamplesToVideoRecorderAndStopsIt() {
        val directory = Files.createTempDirectory("recording-session").toFile()
        val fakeRecorder = FakeVideoRecorder(File(directory, "debug_processed.mp4"))
        val session = ProcessedRecordingSession(
            rootDirectory = directory,
            videoRecorderFactory = { fakeRecorder },
        )

        val recordingSample = session.record(
            sample = AnalysisSample(frameTimestampNanos = 300L),
            settings = AnalysisSettings(),
        )
        session.stop(
            settings = AnalysisSettings(),
            thermalStatus = "none",
        )

        assertEquals(0L, recordingSample.presentationTimestampNanos)
        assertEquals(1, fakeRecorder.recordedSamples)
        assertTrue(fakeRecorder.stopped)
    }

    @Test
    fun forwardsProcessedGlFramesToVideoRecorder() {
        val directory = Files.createTempDirectory("recording-session").toFile()
        val fakeRecorder = FakeVideoRecorder(File(directory, "debug_processed.mp4"))
        val session = ProcessedRecordingSession(
            rootDirectory = directory,
            videoRecorderFactory = { fakeRecorder },
        )

        session.record(
            ProcessedGlFrame(
                textureId = 4,
                size = GlTextureSize(640, 480),
                presentationTimestampNanos = 33_333_333L,
                splitMode = false,
            )
        )

        assertEquals(1, fakeRecorder.recordedFrames)
        assertEquals(33_333_333L, fakeRecorder.lastFrameTimestampNanos)
    }

    private class FakeVideoRecorder(
        override val outputFile: File,
    ) : ProcessedVideoRecorder {
        var recordedSamples = 0
            private set
        var recordedFrames = 0
            private set
        var lastFrameTimestampNanos = -1L
            private set
        var stopped = false
            private set

        override fun record(
            sample: AnalysisSample,
            settings: AnalysisSettings,
        ) {
            recordedSamples++
        }

        override fun record(frame: ProcessedGlFrame) {
            recordedFrames++
            lastFrameTimestampNanos = frame.presentationTimestampNanos
        }

        override fun stop() {
            stopped = true
        }
    }
}
