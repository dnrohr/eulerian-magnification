package com.dnrohr.eulerianmagnification.profiling

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.gl.GlFrameStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceBenchmarkTest {
    @Test
    fun mapsAnalysisAndGlStats() {
        val benchmark = PerformanceBenchmark.from(
            sample = AnalysisSample(
                analysisFps = 18.5,
                latencyMillis = 42.0,
            ),
            glFrameStats = GlFrameStats(
                averageFrameMillis = 16.6,
                averageFps = 60.0,
                sampleCount = 30,
            ),
        )

        assertEquals(18.5, benchmark.analysisFps, 0.0)
        assertEquals(42.0, benchmark.analysisLatencyMillis, 0.0)
        assertEquals(60.0, benchmark.glFps, 0.0)
        assertEquals(41.5, benchmark.fpsDelta, 0.0)
        assertTrue(benchmark.glMeetsThirtyFps)
        assertTrue(benchmark.hasGlSamples)
    }

    @Test
    fun summaryWaitsForGlSamples() {
        val benchmark = PerformanceBenchmark(
            analysisFps = 20.0,
            analysisLatencyMillis = 30.0,
            glFps = 0.0,
            glFrameMillis = 0.0,
            glSampleCount = 0,
        )

        assertEquals("Benchmark: collecting GL samples", benchmark.summary())
    }

    @Test
    fun summaryFlagsBelowTargetGlFps() {
        val benchmark = PerformanceBenchmark(
            analysisFps = 28.0,
            analysisLatencyMillis = 20.0,
            glFps = 24.4,
            glFrameMillis = 41.0,
            glSampleCount = 60,
        )

        assertFalse(benchmark.glMeetsThirtyFps)
        assertEquals(
            "Benchmark: GL camera 24.4 fps (below target), CPU analysis 28.0 fps, delta -3.6 fps",
            benchmark.summary(),
        )
    }
}
