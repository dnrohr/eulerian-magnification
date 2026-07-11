package com.dnrohr.eulerianmagnification.gl

import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import kotlin.math.ceil
import kotlin.math.roundToInt

data class LivePhaseRoiPlan(
    val surfaceSize: GlTextureSize,
    val roi: NormalizedRect,
    val maxTextureDimension: Int = DEFAULT_MAX_TEXTURE_DIMENSION,
    val memoryBudgetBytes: Int = DEFAULT_MEMORY_BUDGET_BYTES,
) {
    init {
        require(roi.left >= 0.0f && roi.top >= 0.0f) { "roi must start inside the frame" }
        require(roi.right <= 1.0f && roi.bottom <= 1.0f) { "roi must end inside the frame" }
        require(roi.width > 0.0f && roi.height > 0.0f) { "roi must have positive size" }
        require(maxTextureDimension > 0) { "maxTextureDimension must be positive" }
        require(memoryBudgetBytes > 0) { "memoryBudgetBytes must be positive" }
    }

    val sourcePixelSize: GlTextureSize = GlTextureSize(
        width = ceil(surfaceSize.width * roi.width).toInt().coerceAtLeast(1),
        height = ceil(surfaceSize.height * roi.height).toInt().coerceAtLeast(1),
    )

    val processingSize: GlTextureSize = sourcePixelSize.fitWithin(maxTextureDimension)
    val phaseStatePlan: LivePhaseRoiStatePlan = LivePhaseRoiStatePlan(processingSize)
    val renderTargetCount: Int = phaseStatePlan.renderTargetCount + WORKING_TARGET_COUNT
    val estimatedBytes: Int = processingSize.width * processingSize.height *
        renderTargetCount * BYTES_PER_RGBA16F_PIXEL
    val fitsMemoryBudget: Boolean = estimatedBytes <= memoryBudgetBytes

    companion object {
        const val DEFAULT_MAX_TEXTURE_DIMENSION = 320
        const val DEFAULT_MEMORY_BUDGET_BYTES = 16 * 1024 * 1024
        const val WORKING_TARGET_COUNT = 3
        const val BYTES_PER_RGBA16F_PIXEL = 8
    }
}

data class LivePhaseRoiStatePlan(
    val size: GlTextureSize,
) {
    val renderTargetCount: Int get() = TARGETS_PER_PHASE_STATE

    companion object {
        const val TARGETS_PER_PHASE_STATE = 5
    }
}

enum class LivePhaseWarmupStatus(
    val label: String,
) {
    Uninitialized("phase warmup: waiting for reference frame"),
    Warming("phase warmup: filling temporal state"),
    Ready("phase ready"),
}

private fun GlTextureSize.fitWithin(maxDimension: Int): GlTextureSize {
    val largest = maxOf(width, height)
    if (largest <= maxDimension) return this
    val scale = maxDimension / largest.toDouble()
    return GlTextureSize(
        width = (width * scale).roundToInt().coerceAtLeast(1),
        height = (height * scale).roundToInt().coerceAtLeast(1),
    )
}
