package com.dnrohr.eulerianmagnification.gl

enum class LivePhaseFallbackReason(
    val label: String,
) {
    NotRequested("not requested"),
    MissingManualRoi("manual ROI required"),
    UnsupportedGl("GL phase resources unavailable"),
    MemoryBudgetExceeded("phase ROI exceeds memory budget"),
    TimingUnhealthy("preview timing unhealthy"),
    LowAmplitude("phase amplitude too low"),
    RendererError("phase renderer disabled after GL error"),
}

data class LivePhaseDiagnostics(
    val requested: Boolean,
    val warmupStatus: LivePhaseWarmupStatus = LivePhaseWarmupStatus.Uninitialized,
    val processingSize: GlTextureSize? = null,
    val amplitudeGate: LivePhaseAmplitudeGate = LivePhaseAmplitudeGate.Unknown,
    val fallbackReason: LivePhaseFallbackReason? = null,
) {
    val active: Boolean
        get() = requested && fallbackReason == null && warmupStatus == LivePhaseWarmupStatus.Ready

    val summary: String
        get() {
            if (!requested) return "phase: ${LivePhaseFallbackReason.NotRequested.label}"
            fallbackReason?.let { return "phase fallback: ${it.label}" }
            val size = processingSize?.let { "${it.width}x${it.height}" } ?: "size unknown"
            return "phase: $size / ${warmupStatus.label} / ${amplitudeGate.label}"
        }
}

data class LivePhaseAmplitudeGate(
    val status: LivePhaseAmplitudeGateStatus,
    val meanAmplitude: Float? = null,
    val threshold: Float? = null,
) {
    val label: String
        get() = when (status) {
            LivePhaseAmplitudeGateStatus.Unknown -> "amplitude unknown"
            LivePhaseAmplitudeGateStatus.Pass -> "amplitude ok ${formatMean()}"
            LivePhaseAmplitudeGateStatus.Low -> "amplitude low ${formatMean()}"
        }.trim()

    private fun formatMean(): String {
        val mean = meanAmplitude ?: return ""
        val threshold = threshold ?: return String.format("%.3f", mean)
        return String.format("%.3f/%.3f", mean, threshold)
    }

    companion object {
        val Unknown = LivePhaseAmplitudeGate(LivePhaseAmplitudeGateStatus.Unknown)
    }
}

enum class LivePhaseAmplitudeGateStatus {
    Unknown,
    Pass,
    Low,
}
