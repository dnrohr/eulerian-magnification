package com.dnrohr.eulerianmagnification.analysis

import java.io.File
import kotlin.math.roundToInt

data class RecordedVideoValidationResult(
    val sourceName: String,
    val settings: AnalysisSettings,
    val report: RecordedVideoAnalysisReport,
) {
    fun summary(): String {
        if (!report.hasFrames) {
            return "Video processing: $sourceName produced no frames"
        }
        val timing = if (report.timestampsMonotonic) "timing OK" else "timing issue"
        return "Video processing: $sourceName ${settings.mode.label} ${settings.lowCutHz.oneDecimal()}-${settings.highCutHz.oneDecimal()} Hz, " +
            "${report.frameCount} frames, ${report.averageFps.oneDecimal()} fps, energy ${report.bandpassedEnergy.oneDecimal()}, " +
            "peak ${report.maxBandpassedMagnitude.oneDecimal()}, $timing"
    }
}

class RecordedVideoValidator(
    private val decode: (File, RecordedVideoDecodeOptions) -> List<RgbFrame> = { file, options ->
        RecordedVideoFrameDecoder().decode(file, options)
    },
) {
    fun validate(
        file: File,
        settings: AnalysisSettings,
        roi: NormalizedRect = RecordedVideoAnalyzer.DEFAULT_ROI,
        decodeOptions: RecordedVideoDecodeOptions = RecordedVideoDecodeOptions(),
    ): RecordedVideoValidationResult {
        val frames = decode(file, decodeOptions)
        val report = RecordedVideoAnalysisRunner(settings, roi).analyze(frames)
        return RecordedVideoValidationResult(
            sourceName = file.name,
            settings = settings,
            report = report,
        )
    }
}

private fun Double.oneDecimal(): String = (this * 10.0).roundToInt().let { tenths ->
    "${tenths / 10}.${kotlin.math.abs(tenths % 10)}"
}
