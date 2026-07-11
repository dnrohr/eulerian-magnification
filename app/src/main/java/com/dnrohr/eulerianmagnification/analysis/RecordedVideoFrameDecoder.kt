package com.dnrohr.eulerianmagnification.analysis

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File

data class RecordedVideoDecodeOptions(
    val targetFps: Double = 30.0,
    val maxFrames: Int = 900,
    val frameOption: Int = MediaMetadataRetriever.OPTION_CLOSEST,
    val maxFrameWidth: Int? = null,
) {
    init {
        require(targetFps > 0.0) { "targetFps must be positive" }
        require(maxFrames > 0) { "maxFrames must be positive" }
        require(maxFrameWidth == null || maxFrameWidth > 0) { "maxFrameWidth must be positive" }
    }
}

object RecordedVideoDecodePlan {
    fun timestampsMicros(
        durationMillis: Long,
        targetFps: Double,
        maxFrames: Int,
    ): List<Long> {
        require(targetFps > 0.0) { "targetFps must be positive" }
        require(maxFrames > 0) { "maxFrames must be positive" }
        if (durationMillis <= 0L) return emptyList()

        val durationMicros = durationMillis * MICROS_PER_MILLISECOND
        val stepMicros = (MICROS_PER_SECOND / targetFps).toLong().coerceAtLeast(1L)
        val timestamps = mutableListOf<Long>()
        var timestampMicros = 0L
        while (timestampMicros < durationMicros && timestamps.size < maxFrames) {
            timestamps.add(timestampMicros)
            timestampMicros += stepMicros
        }
        return timestamps
    }

    private const val MICROS_PER_SECOND = 1_000_000.0
    private const val MICROS_PER_MILLISECOND = 1_000L
}

class RecordedVideoFrameDecoder {
    fun decode(
        file: File,
        options: RecordedVideoDecodeOptions = RecordedVideoDecodeOptions(),
    ): List<RgbFrame> {
        require(file.exists()) { "Video file does not exist: ${file.absolutePath}" }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMillis = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            val sourceSize = retriever.sourceSize()
            RecordedVideoDecodePlan
                .timestampsMicros(durationMillis, options.targetFps, options.maxFrames)
                .mapNotNull { timestampMicros ->
                    retriever.frameAt(timestampMicros, options, sourceSize)
                        ?.toRgbFrame(timestampMicros * NANOS_PER_MICROSECOND)
                }
        } finally {
            retriever.release()
        }
    }

    private fun MediaMetadataRetriever.frameAt(
        timestampMicros: Long,
        options: RecordedVideoDecodeOptions,
        sourceSize: VideoSize?,
    ): Bitmap? {
        val maxWidth = options.maxFrameWidth
        if (maxWidth != null && sourceSize != null && sourceSize.width > maxWidth) {
            val targetWidth = maxWidth
            val targetHeight = (sourceSize.height * (targetWidth / sourceSize.width.toDouble()))
                .toInt()
                .coerceAtLeast(1)
            return getScaledFrameAtTime(
                timestampMicros,
                options.frameOption,
                targetWidth,
                targetHeight,
            )
        }
        return getFrameAtTime(timestampMicros, options.frameOption)
    }

    private fun MediaMetadataRetriever.sourceSize(): VideoSize? {
        val width = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull()
            ?: return null
        val height = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull()
            ?: return null
        return VideoSize(width = width, height = height)
    }

    private fun Bitmap.toRgbFrame(timestampNanos: Long): RgbFrame {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return RgbFrame(
            width = width,
            height = height,
            timestampNanos = timestampNanos,
            pixels = pixels,
        )
    }

    companion object {
        private const val NANOS_PER_MICROSECOND = 1_000L
    }
}

private data class VideoSize(
    val width: Int,
    val height: Int,
)
