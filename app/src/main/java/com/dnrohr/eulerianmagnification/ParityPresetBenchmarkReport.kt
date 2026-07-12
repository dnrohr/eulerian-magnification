package com.dnrohr.eulerianmagnification

import java.util.Locale

data class ParityPresetBenchmarkRow(
    val preset: ParityPreset,
    val measuredFrames: Int,
    val jankyFrames: Int,
    val medianFrameMillis: Double?,
    val p90FrameMillis: Double?,
    val p95FrameMillis: Double?,
    val p99FrameMillis: Double?,
    val thermalStatus: String,
    val recordingSampleCount: Int,
    val recordingDroppedFrameEstimate: Int,
    val recordingStability: String,
    val encodedMp4Valid: Boolean,
    val encodedMp4Bytes: Long,
) {
    val jankyPercent: Double
        get() = if (measuredFrames <= 0) 0.0 else jankyFrames * 100.0 / measuredFrames.toDouble()

    val hasFrameEvidence: Boolean
        get() = measuredFrames > 0
}

data class ParityPresetBenchmarkReport(
    val deviceModel: String,
    val androidVersion: String,
    val rows: List<ParityPresetBenchmarkRow>,
) {
    fun toCsv(): String {
        return buildString {
            appendLine("preset,mode,band,view,amplification,frames,jankyFrames,jankyPercent,medianMs,p90Ms,p95Ms,p99Ms,thermalStatus,recordingSampleCount,recordingDroppedFrameEstimate,recordingStability,encodedMp4Valid,encodedMp4Bytes")
            rows.forEach { row ->
                appendLine(
                    listOf(
                        row.preset.label,
                        row.preset.settings.mode.label,
                        row.preset.bandLabel,
                        row.preset.settings.viewMode.label,
                        row.preset.settings.amplification.formatNumber(),
                        row.measuredFrames.toString(),
                        row.jankyFrames.toString(),
                        row.jankyPercent.formatNumber(),
                        row.medianFrameMillis.formatNullableNumber(),
                        row.p90FrameMillis.formatNullableNumber(),
                        row.p95FrameMillis.formatNullableNumber(),
                        row.p99FrameMillis.formatNullableNumber(),
                        row.thermalStatus,
                        row.recordingSampleCount.toString(),
                        row.recordingDroppedFrameEstimate.toString(),
                        row.recordingStability,
                        row.encodedMp4Valid.toString(),
                        row.encodedMp4Bytes.toString(),
                    ).joinToString(",") { it.csvEscape() },
                )
            }
        }
    }

    fun toJson(): String {
        return buildString {
            appendLine("{")
            appendLine("  \"deviceModel\": ${deviceModel.quoteJson()},")
            appendLine("  \"androidVersion\": ${androidVersion.quoteJson()},")
            appendLine("  \"rows\": [")
            rows.forEachIndexed { index, row ->
                appendLine("    {")
                appendLine("      \"preset\": ${row.preset.label.quoteJson()},")
                appendLine("      \"mode\": ${row.preset.settings.mode.label.quoteJson()},")
                appendLine("      \"band\": ${row.preset.bandLabel.quoteJson()},")
                appendLine("      \"view\": ${row.preset.settings.viewMode.label.quoteJson()},")
                appendLine("      \"amplification\": ${row.preset.settings.amplification.formatNumber()},")
                appendLine("      \"frames\": ${row.measuredFrames},")
                appendLine("      \"jankyFrames\": ${row.jankyFrames},")
                appendLine("      \"jankyPercent\": ${row.jankyPercent.formatNumber()},")
                appendLine("      \"medianMs\": ${row.medianFrameMillis.formatNullableNumber()},")
                appendLine("      \"p90Ms\": ${row.p90FrameMillis.formatNullableNumber()},")
                appendLine("      \"p95Ms\": ${row.p95FrameMillis.formatNullableNumber()},")
                appendLine("      \"p99Ms\": ${row.p99FrameMillis.formatNullableNumber()},")
                appendLine("      \"thermalStatus\": ${row.thermalStatus.quoteJson()},")
                appendLine("      \"recordingSampleCount\": ${row.recordingSampleCount},")
                appendLine("      \"recordingDroppedFrameEstimate\": ${row.recordingDroppedFrameEstimate},")
                appendLine("      \"recordingStability\": ${row.recordingStability.quoteJson()},")
                appendLine("      \"encodedMp4Valid\": ${row.encodedMp4Valid},")
                appendLine("      \"encodedMp4Bytes\": ${row.encodedMp4Bytes}")
                append("    }")
                if (index != rows.lastIndex) append(',')
                appendLine()
            }
            appendLine("  ]")
            appendLine("}")
        }
    }
}

private fun Double?.formatNullableNumber(): String {
    return this?.formatNumber() ?: "null"
}

private fun Number.formatNumber(): String {
    return String.format(Locale.US, "%.3f", toDouble())
}

private fun String.csvEscape(): String {
    val escaped = replace("\"", "\"\"")
    return "\"$escaped\""
}

private fun String.quoteJson(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
