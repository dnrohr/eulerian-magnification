package com.dnrohr.eulerianmagnification.recording

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import com.dnrohr.eulerianmagnification.analysis.TranslationEstimate
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
        assertTrue(json.contains("\"thermalStatus\": \"none\""))
        assertTrue(json.contains("\"sampleCount\": 1"))
        assertTrue(json.contains("\"bandpassedGreen\": 0.250000"))
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
    fun forwardsSamplesToVideoRecorderAndStopsIt() {
        val directory = Files.createTempDirectory("recording-session").toFile()
        val fakeRecorder = FakeVideoRecorder(File(directory, "debug_processed.mp4"))
        val session = ProcessedRecordingSession(
            rootDirectory = directory,
            videoRecorderFactory = { fakeRecorder },
        )

        session.record(
            sample = AnalysisSample(frameTimestampNanos = 300L),
            settings = AnalysisSettings(),
        )
        session.stop(
            settings = AnalysisSettings(),
            thermalStatus = "none",
        )

        assertEquals(1, fakeRecorder.recordedSamples)
        assertTrue(fakeRecorder.stopped)
    }

    private class FakeVideoRecorder(
        override val outputFile: File,
    ) : ProcessedVideoRecorder {
        var recordedSamples = 0
            private set
        var stopped = false
            private set

        override fun record(
            sample: AnalysisSample,
            settings: AnalysisSettings,
        ) {
            recordedSamples++
        }

        override fun stop() {
            stopped = true
        }
    }
}
