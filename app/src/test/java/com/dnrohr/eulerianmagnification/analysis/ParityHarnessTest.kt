package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParityHarnessTest {
    @Test
    fun syntheticPulseProducesAllReviewViewsAndManifestMetrics() {
        val run = ParityHarness().run(ParitySyntheticSamples.colorPulse())

        assertEquals(ViewMode.entries.toSet(), run.viewResults.map { it.viewMode }.toSet())
        assertTrue(run.viewResults.first { it.viewMode == ViewMode.Raw }.metrics.meanAbsDelta == 0.0)

        val amplified = run.viewResults.first { it.viewMode == ViewMode.Amplified }
        assertEquals(RendererKind.RecordedLinearEvm, amplified.rendererKind)
        assertTrue(amplified.metrics.meanAbsDelta > 0.5)
        assertTrue(amplified.metrics.inBandSignalEnergy > 0.0)
        assertTrue(amplified.timelineCsv.startsWith("frameIndex,timestampMillis"))

        val split = run.viewResults.first { it.viewMode == ViewMode.Split }
        assertEquals(32, split.outputWidth)
        assertEquals(12, split.outputHeight)
        assertTrue(split.metrics.meanAbsDelta > 0.5)

        val manifest = run.manifestJson()
        assertTrue(manifest.contains("\"sampleId\": \"synthetic-color-pulse\""))
        assertTrue(manifest.contains("\"viewMode\": \"Amplified\""))
        assertTrue(manifest.contains("\"renderer\": \"recorded_linear_evm\""))
        assertTrue(manifest.contains("\"backgroundPumpingScore\""))
    }

    @Test
    fun movingEdgeUsesPhaseRendererForMotionViews() {
        val run = ParityHarness().run(
            sample = ParitySyntheticSamples.movingEdge(),
            viewModes = listOf(ViewMode.Amplified, ViewMode.Split),
        )

        run.viewResults.forEach { result ->
            assertEquals(RendererKind.RecordedRieszPhaseMotion, result.rendererKind)
            assertTrue(result.metrics.meanAbsDelta > 0.0)
            assertTrue(result.metrics.changedPixelFraction > 0.0)
        }
    }

    @Test
    fun htmlReportEscapesSampleFieldsAndListsMetrics() {
        val run = ParityHarness().run(
            sample = ParitySyntheticSamples.colorPulse().copy(
                id = "synthetic-<pulse>",
                displayName = "Synthetic <Pulse>",
            ),
            viewModes = listOf(ViewMode.Amplified),
        )

        val html = run.htmlReport()

        assertTrue(html.contains("<!doctype html>"))
        assertTrue(html.contains("Synthetic &lt;Pulse&gt;"))
        assertTrue(html.contains("Recorded full-frame linear EVM"))
        assertTrue(html.contains("Background pumping"))
    }
}
