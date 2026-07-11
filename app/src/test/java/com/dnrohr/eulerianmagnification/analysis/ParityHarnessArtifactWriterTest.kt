package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ParityHarnessArtifactWriterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun writesManifestReportTimelinesAndPreviewFrames() {
        val run = ParityHarness().run(
            sample = ParitySyntheticSamples.colorPulse(frameCount = 30),
            viewModes = listOf(ViewMode.Raw, ViewMode.Amplified, ViewMode.Split),
        )
        val bundle = ParityHarnessArtifactWriter(temporaryFolder.root).write(run)

        assertTrue(bundle.manifestFile.isFile)
        assertTrue(bundle.htmlReportFile.isFile)
        assertTrue(bundle.indexFile.isFile)
        assertEquals(3, bundle.viewArtifacts.size)
        bundle.viewArtifacts.forEach { artifacts ->
            assertTrue(artifacts.timelineFile.isFile)
            assertEquals(3, artifacts.previewFrameFiles.size)
            artifacts.previewFrameFiles.forEach { frameFile ->
                assertTrue(frameFile.isFile)
                assertTrue(frameFile.readText().startsWith("P3"))
            }
        }
        assertTrue(bundle.indexFile.readText().contains("\"previewFramePaths\""))
        assertTrue(bundle.manifestFile.readText().contains("\"sampleId\": \"synthetic-color-pulse\""))
    }

    @Test
    fun canWriteDurableSyntheticHarnessArtifactsWhenConfigured() {
        val configuredOutput = System.getProperty("parityHarnessOutputDir")
            ?.takeIf { it.isNotBlank() }
            ?.let { File(it) }
            ?: temporaryFolder.newFolder("parity-output")
        val harness = ParityHarness()
        val writer = ParityHarnessArtifactWriter(configuredOutput)

        val pulseBundle = writer.write(
            harness.run(
                sample = ParitySyntheticSamples.colorPulse(frameCount = 60),
                viewModes = ViewMode.entries,
            ),
        )
        val motionBundle = writer.write(
            harness.run(
                sample = ParitySyntheticSamples.movingEdge(frameCount = 60),
                viewModes = ViewMode.entries,
            ),
        )

        assertTrue(pulseBundle.indexFile.isFile)
        assertTrue(motionBundle.indexFile.isFile)
        assertTrue(pulseBundle.htmlReportFile.readText().contains("Synthetic Color Pulse"))
        assertTrue(motionBundle.manifestFile.readText().contains("\"renderer\": \"recorded_riesz_phase_motion\""))
    }
}
