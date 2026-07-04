package com.dnrohr.eulerianmagnification.gl

class GlTemporalState(
    levelSizes: List<GlTextureSize>,
) {
    val levels: List<GlTemporalLevel> = levelSizes.map(::GlTemporalLevel)

    fun swap() {
        levels.forEach(GlTemporalLevel::swap)
    }

    fun release() {
        levels.forEach(GlTemporalLevel::release)
    }
}

class GlTemporalLevel(size: GlTextureSize) {
    private val targets = listOf(
        GlRenderTarget(size),
        GlRenderTarget(size),
    )
    private var currentIndex = 0

    val current: GlRenderTarget get() = targets[currentIndex]
    val previous: GlRenderTarget get() = targets[1 - currentIndex]
    val size: GlTextureSize get() = current.size

    fun swap() {
        currentIndex = 1 - currentIndex
    }

    fun release() {
        targets.forEach(GlRenderTarget::release)
    }
}

object GlTemporalStateLayout {
    fun levelSizesFor(pyramid: GlPyramid): List<GlTextureSize> {
        return pyramid.levels.map { it.size }
    }
}
