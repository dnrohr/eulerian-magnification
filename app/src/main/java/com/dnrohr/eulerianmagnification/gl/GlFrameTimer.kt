package com.dnrohr.eulerianmagnification.gl

class GlFrameTimer(private val windowSize: Int = 60) {
    private val frameDurationsNanos = ArrayDeque<Long>()
    private var frameStartNanos: Long? = null

    fun beginFrame(timestampNanos: Long) {
        frameStartNanos = timestampNanos
    }

    fun endFrame(
        timestampNanos: Long,
        renderPath: GlRenderPath = GlRenderPath.Unknown,
    ): GlFrameStats {
        val start = frameStartNanos
        if (start != null && timestampNanos >= start) {
            frameDurationsNanos.addLast(timestampNanos - start)
            while (frameDurationsNanos.size > windowSize) {
                frameDurationsNanos.removeFirst()
            }
        }
        frameStartNanos = null
        return stats(renderPath)
    }

    fun stats(renderPath: GlRenderPath = GlRenderPath.Unknown): GlFrameStats {
        if (frameDurationsNanos.isEmpty()) return GlFrameStats(renderPath = renderPath)
        val averageNanos = frameDurationsNanos.average()
        return GlFrameStats(
            averageFrameMillis = averageNanos / NANOS_PER_MILLISECOND,
            averageFps = if (averageNanos <= 0.0) 0.0 else NANOS_PER_SECOND / averageNanos,
            sampleCount = frameDurationsNanos.size,
            renderPath = renderPath,
        )
    }

    companion object {
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val NANOS_PER_MILLISECOND = 1_000_000.0
    }
}

data class GlFrameStats(
    val averageFrameMillis: Double = 0.0,
    val averageFps: Double = 0.0,
    val sampleCount: Int = 0,
    val renderPath: GlRenderPath = GlRenderPath.Unknown,
)

enum class GlRenderPath(val label: String) {
    Unknown("Unknown"),
    ColorBridge("GL color bridge"),
    LiveReconstruction("Live reconstruction"),
    LiveReconstructionFallback("Live reconstruction fallback"),
}
