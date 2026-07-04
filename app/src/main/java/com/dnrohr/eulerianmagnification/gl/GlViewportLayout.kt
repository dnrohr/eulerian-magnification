package com.dnrohr.eulerianmagnification.gl

data class GlViewport(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

object GlViewportLayout {
    fun full(size: GlTextureSize): GlViewport = GlViewport(
        x = 0,
        y = 0,
        width = size.width.coerceAtLeast(1),
        height = size.height.coerceAtLeast(1),
    )

    fun splitHorizontal(size: GlTextureSize): Pair<GlViewport, GlViewport> {
        val width = size.width.coerceAtLeast(2)
        val height = size.height.coerceAtLeast(1)
        val leftWidth = width / 2
        val rightWidth = width - leftWidth
        return GlViewport(0, 0, leftWidth, height) to GlViewport(leftWidth, 0, rightWidth, height)
    }
}
