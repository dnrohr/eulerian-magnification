package com.dnrohr.eulerianmagnification.profiling

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.gl.GlFrameStats
import kotlin.math.abs
import kotlin.math.roundToInt

data class PerformanceBenchmark(
    val analysisFps: Double,
    val analysisLatencyMillis: Double,
    val glFps: Double,
    val glFrameMillis: Double,
    val glSampleCount: Int,
) {
    val fpsDelta: Double get() = glFps - analysisFps
    val glMeetsThirtyFps: Boolean get() = glFps >= TARGET_FPS
    val hasGlSamples: Boolean get() = glSampleCount > 0

    fun summary(): String {
        if (!hasGlSamples) return "Benchmark: collecting GL samples"
        val status = if (glMeetsThirtyFps) "OK" else "below target"
        val deltaPrefix = if (fpsDelta >= 0.0) "+" else ""
        return "Benchmark: GL ${glFps.oneDecimal()} fps ($status), CPU analysis ${analysisFps.oneDecimal()} fps, delta $deltaPrefix${fpsDelta.oneDecimal()} fps"
    }

    companion object {
        const val TARGET_FPS = 30.0

        fun from(
            sample: AnalysisSample,
            glFrameStats: GlFrameStats,
        ): PerformanceBenchmark = PerformanceBenchmark(
            analysisFps = sample.analysisFps,
            analysisLatencyMillis = sample.latencyMillis,
            glFps = glFrameStats.averageFps,
            glFrameMillis = glFrameStats.averageFrameMillis,
            glSampleCount = glFrameStats.sampleCount,
        )
    }
}

private fun Double.oneDecimal(): String = (this * 10.0).roundToInt().let { tenths ->
    "${tenths / 10}.${abs(tenths % 10)}"
}
