package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.roundToInt

data class PreviewSize(
    val width: Int,
    val height: Int,
)

object PreviewRoiMapper {
    fun mapAnalysisToPreview(
        roi: NormalizedRect,
        frameSize: PreviewSize,
        previewSize: PreviewSize,
        rotationDegrees: Int,
        mirrorHorizontally: Boolean,
    ): NormalizedRect {
        if (frameSize.width <= 0 || frameSize.height <= 0 || previewSize.width <= 0 || previewSize.height <= 0) {
            return roi
        }
        val oriented = roi.rotated(rotationDegrees)
            .let { if (mirrorHorizontally) it.mirroredHorizontally() else it }
        val contentSize = orientedContentSize(frameSize, rotationDegrees)
        val viewport = aspectFillViewport(previewSize, contentSize)
        return NormalizedRect(
            left = ((oriented.left * viewport.width) + viewport.x) / previewSize.width.toFloat(),
            top = ((oriented.top * viewport.height) + viewport.y) / previewSize.height.toFloat(),
            right = ((oriented.right * viewport.width) + viewport.x) / previewSize.width.toFloat(),
            bottom = ((oriented.bottom * viewport.height) + viewport.y) / previewSize.height.toFloat(),
        )
    }

    fun mapPreviewToAnalysis(
        roi: NormalizedRect,
        frameSize: PreviewSize,
        previewSize: PreviewSize,
        rotationDegrees: Int,
        mirrorHorizontally: Boolean,
    ): NormalizedRect {
        if (frameSize.width <= 0 || frameSize.height <= 0 || previewSize.width <= 0 || previewSize.height <= 0) {
            return roi
        }
        val contentSize = orientedContentSize(frameSize, rotationDegrees)
        val viewport = aspectFillViewport(previewSize, contentSize)
        val oriented = NormalizedRect(
            left = ((roi.left * previewSize.width) - viewport.x) / viewport.width,
            top = ((roi.top * previewSize.height) - viewport.y) / viewport.height,
            right = ((roi.right * previewSize.width) - viewport.x) / viewport.width,
            bottom = ((roi.bottom * previewSize.height) - viewport.y) / viewport.height,
        ).normalized()
        val unmirrored = if (mirrorHorizontally) oriented.mirroredHorizontally() else oriented
        return unmirrored.rotated(-rotationDegrees).normalized()
    }

    private fun NormalizedRect.rotated(rotationDegrees: Int): NormalizedRect {
        return when (rotationDegrees.floorToRightAngle()) {
            90 -> NormalizedRect(
                left = top,
                top = 1.0f - right,
                right = bottom,
                bottom = 1.0f - left,
            )
            180 -> NormalizedRect(
                left = 1.0f - right,
                top = 1.0f - bottom,
                right = 1.0f - left,
                bottom = 1.0f - top,
            )
            270 -> NormalizedRect(
                left = 1.0f - bottom,
                top = left,
                right = 1.0f - top,
                bottom = right,
            )
            else -> this
        }
    }

    private fun NormalizedRect.mirroredHorizontally(): NormalizedRect {
        return NormalizedRect(
            left = 1.0f - right,
            top = top,
            right = 1.0f - left,
            bottom = bottom,
        )
    }

    private fun NormalizedRect.normalized(): NormalizedRect {
        val normalizedLeft = minOf(left, right).coerceIn(0.0f, 1.0f)
        val normalizedRight = maxOf(left, right).coerceIn(0.0f, 1.0f)
        val normalizedTop = minOf(top, bottom).coerceIn(0.0f, 1.0f)
        val normalizedBottom = maxOf(top, bottom).coerceIn(0.0f, 1.0f)
        return NormalizedRect(normalizedLeft, normalizedTop, normalizedRight, normalizedBottom)
    }

    private fun orientedContentSize(size: PreviewSize, rotationDegrees: Int): PreviewSize {
        return when (rotationDegrees.floorToRightAngle()) {
            90, 270 -> PreviewSize(size.height, size.width)
            else -> size
        }
    }

    private fun aspectFillViewport(
        previewSize: PreviewSize,
        contentSize: PreviewSize,
    ): Viewport {
        val previewWidth = previewSize.width.coerceAtLeast(1)
        val previewHeight = previewSize.height.coerceAtLeast(1)
        val contentWidth = contentSize.width.coerceAtLeast(1)
        val contentHeight = contentSize.height.coerceAtLeast(1)
        val previewAspect = previewWidth.toDouble() / previewHeight.toDouble()
        val contentAspect = contentWidth.toDouble() / contentHeight.toDouble()
        return if (contentAspect > previewAspect) {
            val width = (previewHeight * contentAspect).roundToInt()
            Viewport(
                x = (previewWidth - width) / 2.0f,
                y = 0.0f,
                width = width.toFloat(),
                height = previewHeight.toFloat(),
            )
        } else {
            val height = (previewWidth / contentAspect).roundToInt()
            Viewport(
                x = 0.0f,
                y = (previewHeight - height) / 2.0f,
                width = previewWidth.toFloat(),
                height = height.toFloat(),
            )
        }
    }

    private fun Int.floorToRightAngle(): Int = ((this % 360) + 360) % 360 / 90 * 90

    private data class Viewport(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
    )
}
