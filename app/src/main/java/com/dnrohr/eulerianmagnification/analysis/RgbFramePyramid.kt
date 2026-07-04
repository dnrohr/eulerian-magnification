package com.dnrohr.eulerianmagnification.analysis

data class RgbFramePyramid(
    val levels: List<RgbFrame>,
) {
    init {
        require(levels.isNotEmpty()) { "levels must not be empty" }
    }

    val levelCount: Int get() = levels.size
}

class RgbFramePyramidBuilder(
    private val levelCount: Int,
) {
    init {
        require(levelCount > 0) { "levelCount must be positive" }
    }

    fun build(base: RgbFrame): RgbFramePyramid {
        val levels = mutableListOf(base)
        while (levels.size < levelCount) {
            val previous = levels.last()
            if (previous.width == 1 && previous.height == 1) break
            levels.add(previous.downsample())
        }
        return RgbFramePyramid(levels)
    }

    private fun RgbFrame.downsample(): RgbFrame {
        val nextWidth = (width / 2).coerceAtLeast(1)
        val nextHeight = (height / 2).coerceAtLeast(1)
        val nextPixels = IntArray(nextWidth * nextHeight)
        for (y in 0 until nextHeight) {
            for (x in 0 until nextWidth) {
                nextPixels[y * nextWidth + x] = averageBlock(
                    sourceX = x * 2,
                    sourceY = y * 2,
                )
            }
        }
        return RgbFrame(
            width = nextWidth,
            height = nextHeight,
            timestampNanos = timestampNanos,
            pixels = nextPixels,
        )
    }

    private fun RgbFrame.averageBlock(sourceX: Int, sourceY: Int): Int {
        var red = 0
        var green = 0
        var blue = 0
        var count = 0
        for (dy in 0..1) {
            for (dx in 0..1) {
                val x = sourceX + dx
                val y = sourceY + dy
                if (x < width && y < height) {
                    val pixel = pixels[y * width + x]
                    red += (pixel shr 16) and 0xFF
                    green += (pixel shr 8) and 0xFF
                    blue += pixel and 0xFF
                    count++
                }
            }
        }
        return rgb(
            red = red / count,
            green = green / count,
            blue = blue / count,
        )
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }
}
