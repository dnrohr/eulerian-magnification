package com.dnrohr.eulerianmagnification.recording

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.VisualizationModel
import com.dnrohr.eulerianmagnification.gl.ProcessedGlFrame
import com.dnrohr.eulerianmagnification.quality.LightingDiagnostic
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProcessedRecordingSession(
    private val rootDirectory: File,
    private val startedAtMillis: Long = System.currentTimeMillis(),
    videoRecorderFactory: ((File) -> ProcessedVideoRecorder)? = null,
) {
    private val sessionDirectory = File(rootDirectory, sessionName()).apply { mkdirs() }
    private val videoRecorder = videoRecorderFactory?.invoke(File(sessionDirectory, "debug_processed.mp4"))
    private val frameTimeline = MonotonicFrameTimeline()
    private val samples = mutableListOf<RecordingSample>()
    private var lastTimestampNanos: Long? = null
    var droppedFrameEstimate: Int = 0
        private set

    val elapsedMillis: Long
        get() = (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0L)

    fun record(sample: AnalysisSample): RecordingSample {
        val previous = lastTimestampNanos
        if (previous != null && sample.frameTimestampNanos <= previous) {
            droppedFrameEstimate++
        }
        lastTimestampNanos = sample.frameTimestampNanos
        val recordingSample = RecordingSample.from(
            sample = sample,
            presentationTimestampNanos = frameTimeline.next(sample.frameTimestampNanos),
        )
        samples += recordingSample
        return recordingSample
    }

    fun record(
        sample: AnalysisSample,
        settings: AnalysisSettings,
    ): RecordingSample {
        val recordingSample = record(sample)
        videoRecorder?.record(sample, settings)
        return recordingSample
    }

    fun record(frame: ProcessedGlFrame) {
        videoRecorder?.record(frame)
    }

    fun stop(
        settings: AnalysisSettings,
        thermalStatus: String,
        lightingDiagnostic: LightingDiagnostic? = null,
        visualizationModel: VisualizationModel = VisualizationModel.live(
            settings = settings,
            fullFrameColorPreview = false,
        ),
    ): File {
        videoRecorder?.stop()
        val output = File(sessionDirectory, "metadata.json")
        output.writeText(toJson(settings, thermalStatus, lightingDiagnostic, visualizationModel))
        return output
    }

    private fun sessionName(): String {
        return "processed-${DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(startedAtMillis))}"
            .replace(":", "-")
            .replace(".", "-")
            .lowercase(Locale.US)
    }

    private fun toJson(
        settings: AnalysisSettings,
        thermalStatus: String,
        lightingDiagnostic: LightingDiagnostic?,
        visualizationModel: VisualizationModel,
    ): String {
        return buildString {
            appendLine("{")
            appendLine("  \"startedAtMillis\": $startedAtMillis,")
            appendLine("  \"durationMillis\": $elapsedMillis,")
            appendLine("  \"thermalStatus\": \"$thermalStatus\",")
            appendLine("  \"lightingStatus\": ${lightingDiagnostic?.metadataValue?.quoteJson() ?: "null"},")
            appendLine("  \"lightingStatusLabel\": ${lightingDiagnostic?.label?.quoteJson() ?: "null"},")
            appendLine("  \"lightingAction\": ${lightingDiagnostic?.action?.quoteJson() ?: "null"},")
            appendLine("  \"lightingAverageGreen\": ${lightingDiagnostic?.averageGreen?.format() ?: "null"},")
            appendLine("  \"lightingVariation\": ${lightingDiagnostic?.coefficientOfVariation?.format() ?: "null"},")
            appendLine("  \"mode\": \"${settings.mode.label}\",")
            appendLine("  \"viewMode\": \"${settings.viewMode.label}\",")
            appendLine("  \"signalSource\": \"${visualizationModel.signalSource.id}\",")
            appendLine("  \"signalSourceLabel\": \"${visualizationModel.signalSource.label}\",")
            appendLine("  \"renderer\": \"${visualizationModel.renderer.id}\",")
            appendLine("  \"rendererLabel\": \"${visualizationModel.renderer.label}\",")
            appendLine("  \"visualizationStyle\": \"${visualizationModel.visualizationStyle.id}\",")
            appendLine("  \"visualizationStyleLabel\": \"${visualizationModel.visualizationStyle.label}\",")
            appendLine("  \"amplification\": ${settings.amplification},")
            appendLine("  \"lowCutHz\": ${settings.lowCutHz},")
            appendLine("  \"highCutHz\": ${settings.highCutHz},")
            appendLine("  \"debugVideoPath\": ${videoRecorder?.outputFile?.absolutePath?.quoteJson() ?: "null"},")
            appendLine("  \"sampleCount\": ${samples.size},")
            appendLine("  \"droppedFrameEstimate\": $droppedFrameEstimate,")
            appendLine("  \"samples\": [")
            samples.forEachIndexed { index, sample ->
                append(sample.toJson(indent = "    "))
                if (index != samples.lastIndex) append(",")
                appendLine()
            }
            appendLine("  ]")
            appendLine("}")
        }
    }

    private fun String.quoteJson(): String {
        return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }

    private fun Number.format(): String {
        return String.format(Locale.US, "%.6f", toDouble())
    }
}

data class RecordingSample(
    val timestampNanos: Long,
    val presentationTimestampNanos: Long,
    val analysisFps: Double,
    val latencyMillis: Double,
    val averageGreen: Double,
    val bandpassedGreen: Double,
    val translationDx: Float,
    val translationDy: Float,
    val roiLeft: Float?,
    val roiTop: Float?,
    val roiRight: Float?,
    val roiBottom: Float?,
) {
    fun toJson(indent: String): String {
        return buildString {
            appendLine("$indent{")
            appendLine("$indent  \"timestampNanos\": $timestampNanos,")
            appendLine("$indent  \"presentationTimestampNanos\": $presentationTimestampNanos,")
            appendLine("$indent  \"analysisFps\": ${analysisFps.format()},")
            appendLine("$indent  \"latencyMillis\": ${latencyMillis.format()},")
            appendLine("$indent  \"averageGreen\": ${averageGreen.format()},")
            appendLine("$indent  \"bandpassedGreen\": ${bandpassedGreen.format()},")
            appendLine("$indent  \"translation\": {\"dx\": ${translationDx.format()}, \"dy\": ${translationDy.format()}},")
            appendLine("$indent  \"roi\": ${roiJson()}")
            append("$indent}")
        }
    }

    private fun roiJson(): String {
        return if (roiLeft == null || roiTop == null || roiRight == null || roiBottom == null) {
            "null"
        } else {
            "{\"left\": ${roiLeft.format()}, \"top\": ${roiTop.format()}, \"right\": ${roiRight.format()}, \"bottom\": ${roiBottom.format()}}"
        }
    }

    private fun Number.format(): String {
        return String.format(Locale.US, "%.6f", toDouble())
    }

    companion object {
        fun from(
            sample: AnalysisSample,
            presentationTimestampNanos: Long,
        ): RecordingSample {
            return RecordingSample(
                timestampNanos = sample.frameTimestampNanos,
                presentationTimestampNanos = presentationTimestampNanos,
                analysisFps = sample.analysisFps,
                latencyMillis = sample.latencyMillis,
                averageGreen = sample.averageGreen,
                bandpassedGreen = sample.bandpassedGreen,
                translationDx = sample.translation.dx,
                translationDy = sample.translation.dy,
                roiLeft = sample.roi?.left,
                roiTop = sample.roi?.top,
                roiRight = sample.roi?.right,
                roiBottom = sample.roi?.bottom,
            )
        }
    }
}
