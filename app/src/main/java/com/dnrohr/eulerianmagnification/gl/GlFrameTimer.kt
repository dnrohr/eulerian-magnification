package com.dnrohr.eulerianmagnification.gl

import java.util.Locale

class GlFrameTimer(private val windowSize: Int = 60) {
    private val frameDurationsNanos = ArrayDeque<Long>()
    private var frameStartNanos: Long? = null

    fun beginFrame(timestampNanos: Long) {
        frameStartNanos = timestampNanos
    }

    fun endFrame(
        timestampNanos: Long,
        renderPath: GlRenderPath = GlRenderPath.Unknown,
        reconstructionDiagnostics: GlReconstructionDiagnostics = GlReconstructionDiagnostics(),
        phaseDiagnostics: LivePhaseDiagnostics = LivePhaseDiagnostics(requested = false),
    ): GlFrameStats {
        val start = frameStartNanos
        if (start != null && timestampNanos >= start) {
            frameDurationsNanos.addLast(timestampNanos - start)
            while (frameDurationsNanos.size > windowSize) {
                frameDurationsNanos.removeFirst()
            }
        }
        frameStartNanos = null
        return stats(renderPath, reconstructionDiagnostics, phaseDiagnostics)
    }

    fun stats(
        renderPath: GlRenderPath = GlRenderPath.Unknown,
        reconstructionDiagnostics: GlReconstructionDiagnostics = GlReconstructionDiagnostics(),
        phaseDiagnostics: LivePhaseDiagnostics = LivePhaseDiagnostics(requested = false),
    ): GlFrameStats {
        if (frameDurationsNanos.isEmpty()) {
            return GlFrameStats(
                renderPath = renderPath,
                reconstructionDiagnostics = reconstructionDiagnostics,
                phaseDiagnostics = phaseDiagnostics,
            )
        }
        val averageNanos = frameDurationsNanos.average()
        return GlFrameStats(
            averageFrameMillis = averageNanos / NANOS_PER_MILLISECOND,
            averageFps = if (averageNanos <= 0.0) 0.0 else NANOS_PER_SECOND / averageNanos,
            sampleCount = frameDurationsNanos.size,
            renderPath = renderPath,
            reconstructionDiagnostics = reconstructionDiagnostics,
            phaseDiagnostics = phaseDiagnostics,
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
    val reconstructionDiagnostics: GlReconstructionDiagnostics = GlReconstructionDiagnostics(),
    val phaseDiagnostics: LivePhaseDiagnostics = LivePhaseDiagnostics(requested = false),
)

enum class GlRenderPath(val label: String) {
    Unknown("Unknown"),
    ColorBridge("GL color bridge"),
    LiveReconstruction("Live reconstruction"),
    LiveReconstructionFallback("Live reconstruction fallback"),
}

data class GlReconstructionDiagnostics(
    val activePyramidLevels: Int = 0,
    val internalSize: GlTextureSize? = null,
    val temporalWarm: Boolean = false,
    val temporalStateLevels: Int = 0,
    val lowCutHz: Double? = null,
    val highCutHz: Double? = null,
    val startLevel: Int = 0,
    val levelGains: List<Float> = emptyList(),
    val maxDelta: Float? = null,
    val fallbackReason: GlReconstructionFallbackReason = GlReconstructionFallbackReason.None,
) {
    val hasLivePyramid: Boolean get() = activePyramidLevels > 0 && internalSize != null

    fun summary(): String {
        val sizeLabel = internalSize?.let { "${it.width}x${it.height}" } ?: "n/a"
        val warmupLabel = if (temporalWarm) "ready" else "warming"
        return if (fallbackReason == GlReconstructionFallbackReason.None) {
            val policyLabel = policySummary()
            "Pyramid: ${activePyramidLevels} levels / $sizeLabel / $warmupLabel$policyLabel"
        } else {
            "Pyramid: ${activePyramidLevels} levels / $sizeLabel / fallback ${fallbackReason.label}"
        }
    }

    private fun policySummary(): String {
        val labels = mutableListOf<String>()
        if (temporalStateLevels > 0) {
            labels += "temporal ${temporalStateLevels}L"
        }
        if (lowCutHz != null && highCutHz != null) {
            labels += "band ${lowCutHz.policyFormat()}-${highCutHz.policyFormat()}Hz"
        }
        labels += "start L$startLevel"
        if (levelGains.isNotEmpty()) {
            labels += "gains ${levelGains.joinToString(separator = "/") { it.policyFormat() }}"
        }
        maxDelta?.let {
            labels += "clamp +/-${it.policyFormat()}"
        }
        return if (labels.isEmpty()) "" else " / ${labels.joinToString(separator = " / ")}"
    }
}

private fun Float.policyFormat(): String = String.format(Locale.US, "%.2f", this)
private fun Double.policyFormat(): String = String.format(Locale.US, "%.2f", this)

enum class GlReconstructionFallbackReason(val label: String) {
    None("none"),
    NotRequested("not requested"),
    HalfFloatUnsupported("half-float unsupported"),
    MissingRenderTargets("missing targets"),
    IncompletePyramid("incomplete pyramid"),
    GlError("GL error"),
}
