package com.dnrohr.eulerianmagnification.recording

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.gl.GlEncoderSurfaceRenderer
import com.dnrohr.eulerianmagnification.gl.ProcessedGlFrame
import java.io.File

class GlProcessedMp4Recorder(
    override val outputFile: File,
    private val bitrate: Int = BITRATE,
    private val frameRate: Int = FRAME_RATE,
) : ProcessedVideoRecorder {
    private var codec: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var renderer: GlEncoderSurfaceRenderer? = null
    private val bufferInfo = MediaCodec.BufferInfo()
    private var trackIndex = -1
    private var muxerStarted = false
    private var stopped = false
    private var frameSize: Pair<Int, Int>? = null

    override fun record(
        sample: AnalysisSample,
        settings: AnalysisSettings,
    ) = Unit

    @Synchronized
    override fun record(frame: ProcessedGlFrame) {
        if (stopped) return
        ensureStarted(frame)
        drainEncoder(endOfStream = false)
        renderer?.render(frame)
        drainEncoder(endOfStream = false)
    }

    @Synchronized
    override fun stop() {
        if (stopped) return
        stopped = true
        val activeCodec = codec
        if (activeCodec != null) {
            drainEncoder(endOfStream = false)
            activeCodec.signalEndOfInputStream()
            drainEncoder(endOfStream = true)
        }
        renderer?.release()
        renderer = null
        activeCodec?.stop()
        activeCodec?.release()
        codec = null
        if (muxerStarted) {
            muxer?.stop()
        }
        muxer?.release()
        muxer = null
    }

    private fun ensureStarted(frame: ProcessedGlFrame) {
        val size = frame.size.width to frame.size.height
        val existingSize = frameSize
        if (existingSize != null) {
            check(existingSize == size) { "Processed GL frame size changed during recording." }
            return
        }

        outputFile.parentFile?.mkdirs()
        frameSize = size
        val activeCodec = MediaCodec.createEncoderByType(MIME_TYPE)
        val format = MediaFormat.createVideoFormat(MIME_TYPE, frame.size.width, frame.size.height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_SECONDS)
        }
        activeCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = activeCodec.createInputSurface()
        activeCodec.start()
        codec = activeCodec
        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        renderer = GlEncoderSurfaceRenderer(inputSurface).also { it.initialize() }
    }

    private fun drainEncoder(endOfStream: Boolean) {
        val activeCodec = codec ?: return
        while (true) {
            when (val encoderStatus = activeCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> if (!endOfStream) return
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    check(!muxerStarted) { "Output format changed after muxer started." }
                    trackIndex = requireNotNull(muxer).addTrack(activeCodec.outputFormat)
                    requireNotNull(muxer).start()
                    muxerStarted = true
                }
                else -> if (encoderStatus >= 0) {
                    val encodedData = activeCodec.getOutputBuffer(encoderStatus)
                    if (encodedData != null && bufferInfo.size > 0) {
                        check(muxerStarted) { "Muxer has not started." }
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        requireNotNull(muxer).writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    activeCodec.releaseOutputBuffer(encoderStatus, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                }
            }
        }
    }

    companion object {
        private const val MIME_TYPE = "video/avc"
        private const val BITRATE = 4_000_000
        private const val FRAME_RATE = 30
        private const val I_FRAME_INTERVAL_SECONDS = 1
        private const val TIMEOUT_US = 10_000L
    }
}
