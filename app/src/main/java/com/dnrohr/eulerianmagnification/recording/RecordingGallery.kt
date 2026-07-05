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
) {
    val summary: String
        get() {
            val seconds = durationMillis / 1000.0
            return "$mode/$viewMode, $sampleCount samples, ${String.format(Locale.US, "%.1f", seconds)}s"
        }
}

object RecordingGallery {
    fun listRecent(rootDirectory: File, limit: Int = 3): List<RecordingGalleryItem> {
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
            )
        }.getOrNull()
    }
}
