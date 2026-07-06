package com.dnrohr.eulerianmagnification.analysis

import java.util.Locale

object RecordedVideoEvidenceTimeline {
    fun toCsv(frames: List<RecordedVideoProcessedFrame>): String {
        return buildString {
            appendLine("frameIndex,timestampMillis,analysisFps,averageGreen,bandpassedGreen")
            frames.forEachIndexed { index, frame ->
                val sample = frame.sample
                append(index)
                append(',')
                append(format(sample.frameTimestampNanos / NANOS_PER_MILLISECOND))
                append(',')
                append(format(sample.analysisFps))
                append(',')
                append(format(sample.averageGreen))
                append(',')
                append(format(sample.bandpassedGreen))
                appendLine()
            }
        }
    }

    private fun format(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }

    private const val NANOS_PER_MILLISECOND = 1_000_000.0
}
