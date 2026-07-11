package com.dnrohr.eulerianmagnification.recording

import org.json.JSONObject
import java.io.File
import java.util.Locale

data class RecordingGalleryItem(
    val metadataFile: File,
    val sessionName: String,
    val startedAtMillis: Long,
    val durationMillis: Long,
    val mode: String,
    val viewMode: String,
    val sampleCount: Int,
    val debugVideoPath: String?,
    val timelinePath: String?,
    val evidenceReportPath: String?,
    val qualitySummary: String?,
) {
    val summary: String
        get() {
            val seconds = durationMillis / 1000.0
            return "$mode/$viewMode, $sampleCount samples, ${String.format(Locale.US, "%.1f", seconds)}s"
        }

    val detail: String
        get() {
            val quality = qualitySummary?.takeIf { it.isNotBlank() } ?: "quality unknown"
            return "$sessionName - $quality - ${artifacts.joinToString { it.label }}"
        }

    val artifacts: List<RecordingArtifact>
        get() = buildList {
            add(RecordingArtifact.Metadata(metadataFile.absolutePath))
            debugVideoPath?.let { add(RecordingArtifact.Video(it)) }
            timelinePath?.let { add(RecordingArtifact.SignalTimeline(it)) }
            evidenceReportPath?.let { add(RecordingArtifact.Report(it)) }
        }
}

sealed class RecordingArtifact(
    val label: String,
    val path: String,
) {
    class Metadata(path: String) : RecordingArtifact("metadata", path)
    class Video(path: String) : RecordingArtifact("video", path)
    class SignalTimeline(path: String) : RecordingArtifact("signal CSV", path)
    class Report(path: String) : RecordingArtifact("report", path)
}

object RecordingGallery {
    fun listRecent(rootDirectory: File, limit: Int = 5): List<RecordingGalleryItem> {
        return rootDirectory
            .listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isDirectory && it.name.startsWith("processed-") }
            .mapNotNull { sessionDirectory -> readItem(sessionDirectory) }
            .sortedWith(
                compareByDescending<RecordingGalleryItem> { it.startedAtMillis }
                    .thenByDescending { it.metadataFile.lastModified() },
            )
            .take(limit)
            .toList()
    }

    fun deleteItem(rootDirectory: File, item: RecordingGalleryItem): Boolean {
        val root = rootDirectory.canonicalFile
        val sessionDirectory = item.metadataFile.parentFile?.canonicalFile ?: return false
        if (sessionDirectory.parentFile != root) return false
        if (!sessionDirectory.name.startsWith("processed-")) return false
        return sessionDirectory.deleteRecursively()
    }

    private fun readItem(sessionDirectory: File): RecordingGalleryItem? {
        val metadataFile = File(sessionDirectory, "metadata.json")
        if (!metadataFile.isFile) return null

        return runCatching {
            val json = JSONObject(metadataFile.readText())
            RecordingGalleryItem(
                metadataFile = metadataFile,
                sessionName = sessionDirectory.name,
                startedAtMillis = json.optLong("startedAtMillis", metadataFile.lastModified()),
                durationMillis = json.optLong("durationMillis", 0L),
                mode = json.optString("mode", "Unknown"),
                viewMode = json.optString("viewMode", "Unknown"),
                sampleCount = json.optInt("sampleCount", 0),
                debugVideoPath = json.optString("debugVideoPath").takeUnless { it.isBlank() || it == "null" },
                timelinePath = json.optString("timelinePath").takeUnless { it.isBlank() || it == "null" },
                evidenceReportPath = json.optString("evidenceReportPath").takeUnless { it.isBlank() || it == "null" },
                qualitySummary = json.optString("qualitySummary").takeUnless { it.isBlank() || it == "null" },
            )
        }.getOrNull()
    }
}
