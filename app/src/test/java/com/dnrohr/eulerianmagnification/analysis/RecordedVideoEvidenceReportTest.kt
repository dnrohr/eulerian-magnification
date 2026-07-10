package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordedVideoEvidenceReportTest {
    @Test
    fun writesHtmlReportWithMetadataAndPlot() {
        val html = RecordedVideoEvidenceReport.toHtml(
            sourceName = "clip<1>.mp4",
            settings = AnalysisSettings(mode = MagnificationMode.Pulse, viewMode = ViewMode.Difference),
            frames = listOf(
                processedFrame(timestampNanos = 0L, bandpassedGreen = -0.5),
                processedFrame(timestampNanos = 33_000_000L, bandpassedGreen = 0.5),
            ),
            qualitySummary = "signal present",
        )

        assertTrue(html.contains("<!doctype html>"))
        assertTrue(html.contains("clip&lt;1&gt;.mp4"))
        assertTrue(html.contains("Pulse / Difference"))
        assertTrue(html.contains("Signal: Recorded ROI green bandpass"))
        assertTrue(html.contains("Renderer: ROI signal diagnostic"))
        assertTrue(html.contains("Quality: signal present"))
        assertTrue(html.contains("<polyline"))
        assertFalse(html.contains("clip<1>.mp4"))
    }

    private fun processedFrame(
        timestampNanos: Long,
        bandpassedGreen: Double,
    ): RecordedVideoProcessedFrame {
        return RecordedVideoProcessedFrame(
            frame = RgbFrame(
                width = 1,
                height = 1,
                timestampNanos = timestampNanos,
                pixels = intArrayOf(0),
            ),
            sample = AnalysisSample(
                analysisFps = 30.0,
                bandpassedGreen = bandpassedGreen,
                frameTimestampNanos = timestampNanos,
            ),
        )
    }
}
