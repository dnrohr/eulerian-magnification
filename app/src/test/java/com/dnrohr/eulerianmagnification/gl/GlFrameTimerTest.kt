package com.dnrohr.eulerianmagnification.gl

import org.junit.Assert.assertEquals
import org.junit.Test

class GlFrameTimerTest {
    @Test
    fun reportsEmptyStatsBeforeFrames() {
        val stats = GlFrameTimer().stats()

        assertEquals(0, stats.sampleCount)
        assertEquals(0.0, stats.averageFrameMillis, 0.0)
        assertEquals(0.0, stats.averageFps, 0.0)
        assertEquals(GlRenderPath.Unknown, stats.renderPath)
    }

    @Test
    fun averagesRecentFrameDurations() {
        val timer = GlFrameTimer(windowSize = 2)

        timer.markFrameAvailable(0L)
        timer.beginFrame(0L)
        timer.endFrame(16_000_000L)
        timer.markFrameAvailable(33_000_000L)
        timer.beginFrame(20_000_000L)
        timer.endFrame(52_000_000L)
        timer.markFrameAvailable(66_000_000L)
        val stats = timer.stats()

        assertEquals(2, stats.sampleCount)
        assertEquals(2, stats.renderSampleCount)
        assertEquals(24.0, stats.averageFrameMillis, 0.001)
        assertEquals(30.303, stats.averageFps, 0.01)
    }

    @Test
    fun reportsCameraCadenceSeparatelyFromRenderCost() {
        val timer = GlFrameTimer(windowSize = 2)

        timer.markFrameAvailable(0L)
        timer.beginFrame(0L)
        timer.endFrame(8_000_000L)
        timer.markFrameAvailable(200_000_000L)
        timer.beginFrame(200_000_000L)
        timer.endFrame(208_000_000L)

        val stats = timer.stats()

        assertEquals(8.0, stats.averageFrameMillis, 0.001)
        assertEquals(5.0, stats.averageFps, 0.001)
    }

    @Test
    fun carriesRenderPathInStats() {
        val timer = GlFrameTimer()

        timer.beginFrame(0L)
        val surfaceSize = GlTextureSize(1080, 2400)
        val diagnostics = GlReconstructionDiagnostics(
            activePyramidLevels = 3,
            internalSize = GlTextureSize(640, 360),
            temporalWarm = true,
        )
        val stats = timer.endFrame(
            timestampNanos = 16_000_000L,
            renderPath = GlRenderPath.LiveReconstruction,
            surfaceSize = surfaceSize,
            reconstructionDiagnostics = diagnostics,
        )

        assertEquals(GlRenderPath.LiveReconstruction, stats.renderPath)
        assertEquals(surfaceSize, stats.surfaceSize)
        assertEquals(diagnostics, stats.reconstructionDiagnostics)
    }

    @Test
    fun carriesLivePhaseDiagnosticsInStats() {
        val timer = GlFrameTimer()
        val phaseDiagnostics = LivePhaseDiagnostics(
            requested = true,
            warmupStatus = LivePhaseWarmupStatus.Ready,
            processingSize = GlTextureSize(160, 120),
        )

        timer.beginFrame(0L)
        val stats = timer.endFrame(
            timestampNanos = 16_000_000L,
            phaseDiagnostics = phaseDiagnostics,
        )

        assertEquals(phaseDiagnostics, stats.phaseDiagnostics)
        assertEquals("phase: 160x120 / phase ready / amplitude threshold unknown", stats.phaseDiagnostics.summary)
    }

    @Test
    fun ignoresInvalidFrameEndBeforeStart() {
        val timer = GlFrameTimer()

        timer.beginFrame(20L)
        val stats = timer.endFrame(10L)

        assertEquals(0, stats.sampleCount)
        assertEquals(0, stats.renderSampleCount)
    }

    @Test
    fun summarizesLivePyramidDiagnostics() {
        val ready = GlReconstructionDiagnostics(
            activePyramidLevels = 3,
            internalSize = GlTextureSize(320, 180),
            temporalWarm = true,
            temporalStateLevels = 3,
            lowCutHz = 0.7,
            highCutHz = 3.0,
            startLevel = 1,
            levelGains = listOf(0.35f, 0.75f, 1.0f),
            maxDelta = 0.18f,
        )
        val fallback = GlReconstructionDiagnostics(
            activePyramidLevels = 0,
            internalSize = null,
            fallbackReason = GlReconstructionFallbackReason.HalfFloatUnsupported,
        )

        assertEquals(
            "Pyramid: 3 levels / 320x180 / ready / temporal 3L / band 0.70-3.00Hz / start L1 / gains 0.35/0.75/1.00 / clamp +/-0.18",
            ready.summary(),
        )
        assertEquals(
            "Pyramid: 0 levels / n/a / fallback half-float unsupported",
            fallback.summary(),
        )
    }
}
