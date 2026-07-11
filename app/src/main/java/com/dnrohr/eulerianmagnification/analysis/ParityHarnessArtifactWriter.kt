package com.dnrohr.eulerianmagnification.analysis

import java.io.File
import java.util.Locale

data class ParityHarnessArtifactBundle(
    val rootDirectory: File,
    val manifestFile: File,
    val htmlReportFile: File,
    val indexFile: File,
    val viewArtifacts: List<ParityHarnessViewArtifacts>,
) {
    fun indexJson(): String {
        return buildString {
            appendLine("{")
            appendLine("  \"rootDirectory\": ${rootDirectory.invariantSeparatorsPath.quoteJson()},")
            appendLine("  \"manifestPath\": ${manifestFile.invariantSeparatorsPath.quoteJson()},")
            appendLine("  \"htmlReportPath\": ${htmlReportFile.invariantSeparatorsPath.quoteJson()},")
            appendLine("  \"views\": [")
            viewArtifacts.forEachIndexed { index, artifacts ->
                appendLine("    {")
                appendLine("      \"viewMode\": ${artifacts.viewMode.label.quoteJson()},")
                appendLine("      \"timelinePath\": ${artifacts.timelineFile.invariantSeparatorsPath.quoteJson()},")
                appendLine("      \"previewFramePaths\": [")
                artifacts.previewFrameFiles.forEachIndexed { frameIndex, frameFile ->
                    append("        ${frameFile.invariantSeparatorsPath.quoteJson()}")
                    if (frameIndex != artifacts.previewFrameFiles.lastIndex) append(',')
                    appendLine()
                }
                appendLine("      ]")
                append("    }")
                if (index != viewArtifacts.lastIndex) append(',')
                appendLine()
            }
            appendLine("  ]")
            appendLine("}")
        }
    }
}

data class ParityHarnessViewArtifacts(
    val viewMode: ViewMode,
    val timelineFile: File,
    val previewFrameFiles: List<File>,
)

class ParityHarnessArtifactWriter(
    private val outputRoot: File,
) {
    fun write(run: ParityHarnessRun): ParityHarnessArtifactBundle {
        val sampleDirectory = outputRoot.resolve(run.sample.id.sanitizeFileName())
        sampleDirectory.mkdirs()
        require(sampleDirectory.isDirectory) { "Could not create ${sampleDirectory.absolutePath}" }

        val manifestFile = sampleDirectory.resolve("manifest.json")
        val htmlReportFile = sampleDirectory.resolve("evidence_report.html")
        manifestFile.writeText(run.manifestJson())
        htmlReportFile.writeText(run.htmlReport())

        val viewArtifacts = run.viewResults.map { result ->
            val viewDirectory = sampleDirectory.resolve(result.viewMode.name.lowercase(Locale.US))
            viewDirectory.mkdirs()
            require(viewDirectory.isDirectory) { "Could not create ${viewDirectory.absolutePath}" }

            val timelineFile = viewDirectory.resolve("signal_timeline.csv")
            timelineFile.writeText(result.timelineCsv)
            val previewFrames = previewFrameIndices(result.processedFrames.size).map { frameIndex ->
                val frameFile = viewDirectory.resolve("frame_${frameIndex.toString().padStart(4, '0')}.ppm")
                frameFile.writeText(result.processedFrames[frameIndex].frame.toPpm())
                frameFile
            }

            ParityHarnessViewArtifacts(
                viewMode = result.viewMode,
                timelineFile = timelineFile,
                previewFrameFiles = previewFrames,
            )
        }

        val bundle = ParityHarnessArtifactBundle(
            rootDirectory = sampleDirectory,
            manifestFile = manifestFile,
            htmlReportFile = htmlReportFile,
            indexFile = sampleDirectory.resolve("artifact_index.json"),
            viewArtifacts = viewArtifacts,
        )
        bundle.indexFile.writeText(bundle.indexJson())
        return bundle
    }

    private fun previewFrameIndices(frameCount: Int): List<Int> {
        require(frameCount > 0) { "frameCount must be positive" }
        return listOf(0, frameCount / 2, frameCount - 1).distinct()
    }

    private fun RgbFrame.toPpm(): String {
        return buildString {
            appendLine("P3")
            appendLine("$width $height")
            appendLine("255")
            pixels.forEachIndexed { index, pixel ->
                append((pixel shr 16) and 0xFF)
                append(' ')
                append((pixel shr 8) and 0xFF)
                append(' ')
                append(pixel and 0xFF)
                if ((index + 1) % width == 0) {
                    appendLine()
                } else {
                    append(' ')
                }
            }
        }
    }

    private fun String.sanitizeFileName(): String {
        return replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifBlank { "sample" }
    }
}

private fun String.quoteJson(): String = "\"${escapeJson()}\""

private fun String.escapeJson(): String {
    return buildString {
        this@escapeJson.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}
