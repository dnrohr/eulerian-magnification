package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class SampleVideoCatalogTest {
    @Test
    fun catalogIncludesMitBabyDownloadSpec() {
        val sample = SampleVideoCatalog.byId("mit-baby")

        assertNotNull(sample)
        requireNotNull(sample)
        assertEquals("sample-videos/mit-evm-baby.mp4", sample.relativePath)
        assertEquals(MagnificationMode.Breathing, sample.recommendedMode)
        assertEquals(ViewMode.Split, sample.recommendedViewMode)
        assertTrue(sample.sourceUrl!!.contains("baby.mp4"))
        assertEquals(64, sample.sha256.length)
    }

    @Test
    fun catalogIncludesLocalEulerFallbackSpec() {
        val sample = SampleVideoCatalog.byId("local-euler")

        assertNotNull(sample)
        requireNotNull(sample)
        assertEquals("sample-videos/euler.mp4", sample.relativePath)
        assertEquals(MagnificationMode.Pulse, sample.recommendedMode)
        assertEquals(ViewMode.Split, sample.recommendedViewMode)
        assertEquals(null, sample.sourceUrl)
        assertTrue(sample.redistribution.contains("local", ignoreCase = true))
    }

    @Test
    fun reportsAvailableSamplesFromWorkspaceRoot() {
        val root = Files.createTempDirectory("sample-catalog").toFile()
        val sample = SampleVideoCatalog.byId("mit-baby")!!
        val file = sample.localFile(root)
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(1, 2, 3))

        assertEquals(listOf(sample), SampleVideoCatalog.availableSamples(root))
    }

    @Test
    fun retrievalInstructionsNamePathAndChecksum() {
        val mit = SampleVideoCatalog.retrievalInstructions(SampleVideoCatalog.byId("mit-baby")!!)
        val local = SampleVideoCatalog.retrievalInstructions(SampleVideoCatalog.byId("local-euler")!!)

        assertTrue(mit.contains("https://"))
        assertTrue(mit.contains("sample-videos/mit-evm-baby.mp4"))
        assertTrue(mit.contains("2C5E744384AB88FCCD3AA4883959B33EB4CDB7384C3E46E788CEDE821B2478EE"))
        assertFalse(local.contains("https://"))
        assertTrue(local.contains("sample-videos/euler.mp4"))
    }
}
