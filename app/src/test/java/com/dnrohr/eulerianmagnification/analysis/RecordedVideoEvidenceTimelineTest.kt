package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordedVideoEvidenceTimelineTest {
    @Test
    fun writesHeaderAndSignalRows() {
        val csv = RecordedVideoEvidenceTimeline.toCsv(
            listOf(
                RecordedVideoProcessedFrame(
                    frame = RgbFrame(
                        width = 1,
                        height = 1,
                        timestampNanos = 16_000_000L,
                        pixels = intArrayOf(0),
                    ),
                    sample = AnalysisSample(
                        analysisFps = 30.0,
                        averageGreen = 128.25,
                        bandpassedGreen = -0.5,
                        frameTimestampNanos = 16_000_000L,
                    ),
                ),
            ),
        )

        assertEquals(
            "frameIndex,timestampMillis,analysisFps,averageGreen,bandpassedGreen,colorGate,colorGateGain,saturatedPixelFraction\n" +
                "0,16.000000,30.000000,128.250000,-0.500000,stable,1.000000,0.000000\n",
            csv.replace("\r\n", "\n"),
        )
    }
}
