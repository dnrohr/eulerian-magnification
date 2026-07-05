package com.dnrohr.eulerianmagnification.recording

import androidx.test.platform.app.InstrumentationRegistry
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.RecordedVideoProcessor
import com.dnrohr.eulerianmagnification.analysis.RgbFrame
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class RecordedVideoMp4ExporterInstrumentedTest {
    @Test
    fun exportsProcessedRecordedFramesToValidMp4() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputFile = File(context.cacheDir, "recorded-video-export.mp4")
        outputFile.delete()
        val processed = RecordedVideoProcessor(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 16.0f,
                viewMode = ViewMode.Split,
            )
        ).process(syntheticClip()).processedFrames

        RecordedVideoMp4Exporter(
            bitrate = 700_000,
            frameRate = 30,
        ).export(processed, outputFile)

        val validation = EncodedOutputValidator().validate(outputFile)
        assertTrue(validation.errors.joinToString(), validation.isValid)
    }

    private fun syntheticClip(): List<RgbFrame> {
        return (0 until 30).map { frameIndex ->
            val seconds = frameIndex / FPS
            val timestampNanos = (seconds * NANOS_PER_SECOND).toLong()
            val green = 128 + (14.0 * sin(2.0 * PI * 1.2 * seconds)).roundToInt()
            val pixels = IntArray(WIDTH * HEIGHT) { rgb(80, 80, 80) }
            for (y in 16 until 32) {
                for (x in 22 until 42) {
                    pixels[y * WIDTH + x] = rgb(96, green, 96)
                }
            }
            RgbFrame(
                width = WIDTH,
                height = HEIGHT,
                timestampNanos = timestampNanos,
                pixels = pixels,
            )
        }
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (0xFF shl 24) or
            (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }

    companion object {
        private const val WIDTH = 64
        private const val HEIGHT = 48
        private const val FPS = 30.0
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
