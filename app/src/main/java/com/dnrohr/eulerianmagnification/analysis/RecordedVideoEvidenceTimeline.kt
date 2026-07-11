package com.dnrohr.eulerianmagnification.analysis

import java.util.Locale

object RecordedVideoEvidenceTimeline {
    fun toCsv(frames: List<RecordedVideoProcessedFrame>): String {
        return buildString {
            appendLine("frameIndex,timestampMillis,analysisFps,averageGreen,bandpassedGreen,colorGate,colorGateGain,saturatedPixelFraction")
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
                append(',')
                append(frame.colorGate.reason.code)
                append(',')
                append(format(frame.colorGate.gain.toDouble()))
                append(',')
                append(format(frame.colorGate.saturatedPixelFraction))
                appendLine()
            }
        }
    }

    private fun format(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }

    private const val NANOS_PER_MILLISECOND = 1_000_000.0
}
