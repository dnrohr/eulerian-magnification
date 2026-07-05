package com.dnrohr.eulerianmagnification.recording

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import com.dnrohr.eulerianmagnification.analysis.RecordedVideoProcessedFrame
import java.io.File

class RecordedVideoMp4Exporter(
    private val bitrate: Int = DEFAULT_BITRATE,
    private val frameRate: Int = DEFAULT_FRAME_RATE,
) {
    fun export(
        frames: List<RecordedVideoProcessedFrame>,
        outputFile: File,
    ): File {
        require(frames.isNotEmpty()) { "frames must not be empty" }
        outputFile.parentFile?.mkdirs()
        val firstFrame = frames.first().frame
        val outputWidth = firstFrame.width.evenDimension()
        val outputHeight = firstFrame.height.evenDimension()
        SurfaceMp4Writer(outputFile, outputWidth, outputHeight, bitrate, frameRate).use { writer ->
            frames.forEach { processed ->
                writer.write(processed)
            }
        }
        return outputFile
    }

    private fun Int.evenDimension(): Int = if (this % 2 == 0) this.coerceAtLeast(2) else this + 1

    private class SurfaceMp4Writer(
        private val outputFile: File,
        private val width: Int,
        private val height: Int,
        bitrate: Int,
        frameRate: Int,
    ) : AutoCloseable {
        private val codec = MediaCodec.createEncoderByType(MIME_TYPE)
        private val bufferInfo = MediaCodec.BufferInfo()
        private val muxer: MediaMuxer
        private val inputSurface: Surface
        private val destination = Rect(0, 0, width, height)
        private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        private var trackIndex = -1
        private var muxerStarted = false
        private var closed = false

        init {
            val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_SECONDS)
            }
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        }

        fun write(processed: RecordedVideoProcessedFrame) {
            if (closed) return
            drainEncoder(endOfStream = false)
            drawFrame(processed)
        }

        private fun drawFrame(processed: RecordedVideoProcessedFrame) {
            val frame = processed.frame
            val bitmap = Bitmap.createBitmap(frame.pixels, frame.width, frame.height, Bitmap.Config.ARGB_8888)
            val canvas = inputSurface.lockCanvas(null)
            try {
                canvas.drawColor(Color.BLACK)
                canvas.drawBitmap(bitmap, null, destination, bitmapPaint)
            } finally {
                inputSurface.unlockCanvasAndPost(canvas)
                bitmap.recycle()
            }
        }

        private fun drainEncoder(endOfStream: Boolean) {
            while (true) {
                val encoderStatus = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (!endOfStream) return
                    }
                    encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        check(!muxerStarted) { "Output format changed after muxer started." }
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    encoderStatus >= 0 -> {
                        val encodedData = codec.getOutputBuffer(encoderStatus)
                        if (encodedData != null && bufferInfo.size > 0) {
                            check(muxerStarted) { "Muxer has not started." }
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                        codec.releaseOutputBuffer(encoderStatus, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            return
                        }
                    }
                }
            }
        }

        override fun close() {
            if (closed) return
            closed = true
            drainEncoder(endOfStream = false)
            codec.signalEndOfInputStream()
            drainEncoder(endOfStream = true)
            inputSurface.release()
            codec.stop()
            codec.release()
            if (muxerStarted) {
                muxer.stop()
            }
            muxer.release()
        }
    }

    companion object {
        private const val MIME_TYPE = "video/avc"
        private const val DEFAULT_BITRATE = 2_500_000
        private const val DEFAULT_FRAME_RATE = 30
        private const val I_FRAME_INTERVAL_SECONDS = 1
        private const val TIMEOUT_US = 10_000L
    }
}
