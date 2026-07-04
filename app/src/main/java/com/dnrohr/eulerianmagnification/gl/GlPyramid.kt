package com.dnrohr.eulerianmagnification.gl

class GlPyramid(
    baseSize: GlTextureSize,
    levelCount: Int,
) {
    val levels: List<GlRenderTarget> = pyramidSizes(baseSize, levelCount).map(::GlRenderTarget)

    fun release() {
        levels.forEach(GlRenderTarget::release)
    }

    companion object {
        fun pyramidSizes(
            baseSize: GlTextureSize,
            levelCount: Int,
        ): List<GlTextureSize> {
            require(levelCount > 0) { "levelCount must be positive" }
            val sizes = mutableListOf<GlTextureSize>()
            var width = baseSize.width
            var height = baseSize.height
            repeat(levelCount) {
                sizes += GlTextureSize(width, height)
                width = (width / 2).coerceAtLeast(1)
                height = (height / 2).coerceAtLeast(1)
            }
            return sizes
        }
    }
}
