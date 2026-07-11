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

        timer.beginFrame(0L)
        timer.endFrame(16_000_000L)
        timer.beginFrame(20_000_000L)
        timer.endFrame(52_000_000L)
        val stats = timer.stats()

        assertEquals(2, stats.sampleCount)
        assertEquals(24.0, stats.averageFrameMillis, 0.001)
        assertEquals(41.666, stats.averageFps, 0.01)
    }

    @Test
    fun carriesRenderPathInStats() {
        val timer = GlFrameTimer()

        timer.beginFrame(0L)
        val diagnostics = GlReconstructionDiagnostics(
            activePyramidLevels = 3,
            internalSize = GlTextureSize(640, 360),
            temporalWarm = true,
        )
        val stats = timer.endFrame(
            timestampNanos = 16_000_000L,
            renderPath = GlRenderPath.LiveReconstruction,
            reconstructionDiagnostics = diagnostics,
        )

        assertEquals(GlRenderPath.LiveReconstruction, stats.renderPath)
        assertEquals(diagnostics, stats.reconstructionDiagnostics)
    }

    @Test
    fun ignoresInvalidFrameEndBeforeStart() {
        val timer = GlFrameTimer()

        timer.beginFrame(20L)
        val stats = timer.endFrame(10L)

        assertEquals(0, stats.sampleCount)
    }

    @Test
    fun summarizesLivePyramidDiagnostics() {
        val ready = GlReconstructionDiagnostics(
            activePyramidLevels = 3,
            internalSize = GlTextureSize(320, 180),
            temporalWarm = true,
        )
        val fallback = GlReconstructionDiagnostics(
            activePyramidLevels = 0,
            internalSize = null,
            fallbackReason = GlReconstructionFallbackReason.HalfFloatUnsupported,
        )

        assertEquals("Pyramid: 3 levels / 320x180 / ready", ready.summary())
        assertEquals(
            "Pyramid: 0 levels / n/a / fallback half-float unsupported",
            fallback.summary(),
        )
    }
}
