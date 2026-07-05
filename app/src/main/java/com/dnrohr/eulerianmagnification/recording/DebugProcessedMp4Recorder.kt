package com.dnrohr.eulerianmagnification.recording

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.view.Surface
import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import com.dnrohr.eulerianmagnification.quality.ArtifactSuppressor
import java.io.File

class DebugProcessedMp4Recorder(
    override val outputFile: File,
    private val width: Int = DEFAULT_WIDTH,
    private val height: Int = DEFAULT_HEIGHT,
) : ProcessedVideoRecorder {
    private val codec = MediaCodec.createEncoderByType(MIME_TYPE)
    private val bufferInfo = MediaCodec.BufferInfo()
    private val muxer: MediaMuxer
    private val inputSurface: Surface
    private var trackIndex = -1
    private var muxerStarted = false
    private var stopped = false

    init {
        outputFile.parentFile?.mkdirs()
        val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_SECONDS)
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = codec.createInputSurface()
        codec.start()
        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    override fun record(
        sample: AnalysisSample,
        settings: AnalysisSettings,
    ) {
        if (stopped) return
        drainEncoder(endOfStream = false)
        drawFrame(sample, settings)
    }

    override fun stop() {
        if (stopped) return
        stopped = true
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

    private fun drawFrame(
        sample: AnalysisSample,
        settings: AnalysisSettings,
    ) {
        val canvas = inputSurface.lockCanvas(null)
        try {
            DebugFrameRenderer(width, height).draw(canvas, sample, settings)
        } finally {
            inputSurface.unlockCanvasAndPost(canvas)
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

    private class DebugFrameRenderer(
        private val width: Int,
        private val height: Int,
    ) {
        private val artifactSuppressor = ArtifactSuppressor()
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34.0f
        }
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 200, 87)
            textSize = 28.0f
        }
        private val roiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6.0f
            color = Color.rgb(0, 191, 165)
        }
        private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        fun draw(
            canvas: Canvas,
            sample: AnalysisSample,
            settings: AnalysisSettings,
        ) {
            canvas.drawColor(Color.rgb(16, 20, 24))
            drawTint(canvas, sample, settings)
            drawLabels(canvas, sample, settings)
            drawSignalBar(canvas, sample, settings)
        }

        private fun drawTint(
            canvas: Canvas,
            sample: AnalysisSample,
            settings: AnalysisSettings,
        ) {
            val roi = sample.roi ?: return
            val left = roi.left * width
            val top = roi.top * height
            val right = roi.right * width
            val bottom = roi.bottom * height
            if (settings.viewMode != ViewMode.Raw) {
                val signal = artifactSuppressor.amplify(sample.bandpassedGreen, settings.amplification)
                val alpha = (signal.normalizedMagnitude.coerceIn(0.12, 0.70) * 255.0).toInt()
                tintPaint.color = if (signal.value >= 0.0) {
                    Color.argb(alpha, 255, 107, 107)
                } else {
                    Color.argb(alpha, 58, 134, 255)
                }
                canvas.drawRect(left, top, right, bottom, tintPaint)
            }
            canvas.drawRect(left, top, right, bottom, roiPaint)
        }

        private fun drawLabels(
            canvas: Canvas,
            sample: AnalysisSample,
            settings: AnalysisSettings,
        ) {
            canvas.drawText("Eulerian Magnification Debug Output", 32.0f, 64.0f, textPaint)
            canvas.drawText("Mode: ${settings.mode.label} / ${settings.viewMode.label}", 32.0f, 112.0f, labelPaint)
            canvas.drawText("Band: ${settings.lowCutHz}-${settings.highCutHz} Hz  Amp: ${settings.amplification}x", 32.0f, 152.0f, labelPaint)
            canvas.drawText("FPS: ${"%.1f".format(sample.analysisFps)}  Latency: ${"%.0f".format(sample.latencyMillis)} ms", 32.0f, 192.0f, labelPaint)
            canvas.drawText("Green: ${"%.1f".format(sample.averageGreen)}  Signal: ${"%+.3f".format(sample.bandpassedGreen)}", 32.0f, 232.0f, labelPaint)
            canvas.drawText("Motion: dx ${"%+.4f".format(sample.translation.dx)} dy ${"%+.4f".format(sample.translation.dy)}", 32.0f, 272.0f, labelPaint)
        }

        private fun drawSignalBar(
            canvas: Canvas,
            sample: AnalysisSample,
            settings: AnalysisSettings,
        ) {
            val centerX = width / 2.0f
            val centerY = height - 96.0f
            val maxWidth = width * 0.42f
            val signal = artifactSuppressor.amplify(sample.bandpassedGreen, settings.amplification)
                .value
                .div(ArtifactSuppressor.DEFAULT_MAX_AMPLIFIED_MAGNITUDE)
                .coerceIn(-1.0, 1.0)
                .toFloat()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 200, 87)
                strokeWidth = 12.0f
            }
            canvas.drawLine(centerX - maxWidth, centerY, centerX + maxWidth, centerY, paint)
            paint.color = if (signal >= 0.0f) Color.rgb(255, 107, 107) else Color.rgb(58, 134, 255)
            canvas.drawLine(centerX, centerY, centerX + signal * maxWidth, centerY, paint)
        }
    }

    companion object {
        private const val MIME_TYPE = "video/avc"
        private const val DEFAULT_WIDTH = 720
        private const val DEFAULT_HEIGHT = 1280
        private const val BITRATE = 2_500_000
        private const val FRAME_RATE = 30
        private const val I_FRAME_INTERVAL_SECONDS = 1
        private const val TIMEOUT_US = 10_000L
    }
}
