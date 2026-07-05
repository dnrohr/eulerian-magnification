package com.dnrohr.eulerianmagnification.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class RecordingGalleryTest {
    @Test
    fun listsRecentRecordingMetadataNewestFirst() {
        val root = Files.createTempDirectory("recording-gallery").toFile()
        writeMetadata(root, "processed-old", startedAtMillis = 100L, mode = "Pulse", sampleCount = 3)
        writeMetadata(root, "processed-new", startedAtMillis = 200L, mode = "Breathing", sampleCount = 7)

        val recordings = RecordingGallery.listRecent(root)

        assertEquals(2, recordings.size)
        assertEquals("processed-new", recordings[0].sessionName)
        assertEquals("Breathing", recordings[0].mode)
        assertEquals(7, recordings[0].sampleCount)
        assertTrue(recordings[0].summary.contains("Breathing"))
    }

    @Test
    fun skipsInvalidOrIncompleteRecordingDirectories() {
        val root = Files.createTempDirectory("recording-gallery").toFile()
        root.resolve("processed-empty").mkdirs()
        writeMetadata(root, "processed-valid", startedAtMillis = 100L, mode = "Pulse", sampleCount = 1)
        root.resolve("processed-bad").mkdirs()
        root.resolve("processed-bad").resolve("metadata.json").writeText("{")

        val recordings = RecordingGallery.listRecent(root)

        assertEquals(1, recordings.size)
        assertEquals("processed-valid", recordings.single().sessionName)
    }

    @Test
    fun appliesLimit() {
        val root = Files.createTempDirectory("recording-gallery").toFile()
        writeMetadata(root, "processed-1", startedAtMillis = 1L)
        writeMetadata(root, "processed-2", startedAtMillis = 2L)
        writeMetadata(root, "processed-3", startedAtMillis = 3L)

        val recordings = RecordingGallery.listRecent(root, limit = 2)

        assertEquals(listOf("processed-3", "processed-2"), recordings.map { it.sessionName })
    }

    private fun writeMetadata(
        root: File,
        sessionName: String,
        startedAtMillis: Long,
        mode: String = "Pulse",
        sampleCount: Int = 0,
    ) {
        val directory = root.resolve(sessionName).apply { mkdirs() }
        directory.resolve("metadata.json").writeText(
            """
            {
              "startedAtMillis": $startedAtMillis,
              "durationMillis": 1500,
              "mode": "$mode",
              "viewMode": "Amplified",
              "debugVideoPath": null,
              "sampleCount": $sampleCount
            }
            """.trimIndent(),
        )
    }
}
