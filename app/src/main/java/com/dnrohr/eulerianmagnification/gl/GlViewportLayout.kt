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

    fun aspectFill(
        surfaceSize: GlTextureSize,
        contentSize: GlTextureSize,
    ): GlViewport {
        val surfaceWidth = surfaceSize.width.coerceAtLeast(1)
        val surfaceHeight = surfaceSize.height.coerceAtLeast(1)
        val contentWidth = contentSize.width.coerceAtLeast(1)
        val contentHeight = contentSize.height.coerceAtLeast(1)
        val surfaceAspect = surfaceWidth.toDouble() / surfaceHeight.toDouble()
        val contentAspect = contentWidth.toDouble() / contentHeight.toDouble()
        return if (contentAspect > surfaceAspect) {
            val width = (surfaceHeight * contentAspect).toInt()
            GlViewport(
                x = (surfaceWidth - width) / 2,
                y = 0,
                width = width.coerceAtLeast(1),
                height = surfaceHeight,
            )
        } else {
            val height = (surfaceWidth / contentAspect).toInt()
            GlViewport(
                x = 0,
                y = (surfaceHeight - height) / 2,
                width = surfaceWidth,
                height = height.coerceAtLeast(1),
            )
        }
    }

    fun orientContentToSurface(
        surfaceSize: GlTextureSize,
        contentSize: GlTextureSize,
    ): GlTextureSize {
        val surfacePortrait = surfaceSize.height >= surfaceSize.width
        val contentPortrait = contentSize.height >= contentSize.width
        return if (surfacePortrait == contentPortrait) {
            contentSize
        } else {
            GlTextureSize(contentSize.height, contentSize.width)
        }
    }
}
