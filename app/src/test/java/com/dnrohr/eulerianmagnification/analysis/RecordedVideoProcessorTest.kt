package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class RecordedVideoProcessorTest {
    @Test
    fun rawViewPreservesInputFrames() {
        val frames = syntheticClip(frequencyHz = 1.2)
        val result = RecordedVideoProcessor(
            settings = AnalysisSettings(viewMode = ViewMode.Raw),
        ).process(frames)

        assertEquals(frames.size, result.sourceFrameCount)
        assertEquals(frames.first().width, result.processedFrames.first().frame.width)
        assertEquals(frames.first().pixels.toList(), result.processedFrames.first().frame.pixels.toList())
    }

    @Test
    fun amplifiedViewChangesFullFrameForInBandSignal() {
        val frames = syntheticGlobalClip(frequencyHz = 1.2)
        val result = RecordedVideoProcessor(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 20.0f,
                viewMode = ViewMode.Amplified,
            ),
        ).process(frames)

        val changedFrame = result.processedFrames.first { processed ->
            processed.frame.pixels[0] != frames[result.processedFrames.indexOf(processed)].pixels[0]
        }
        val sourceFrame = frames[result.processedFrames.indexOf(changedFrame)]
        assertEquals(sourceFrame.width, changedFrame.frame.width)
        assertEquals(sourceFrame.height, changedFrame.frame.height)
        assertTrue(changedFrame.frame.pixels[0] != sourceFrame.pixels[0])
    }

    @Test
    fun differenceViewRendersSignedRoiSignalWithDimContext() {
        val frames = syntheticClip(frequencyHz = 1.2)
        val result = RecordedVideoProcessor(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 20.0f,
                viewMode = ViewMode.Difference,
            ),
        ).process(frames)

        val positiveFrame = result.processedFrames.first { processed ->
            processed.sample.bandpassedGreen > 0.1
        }.frame
        val negativeFrame = result.processedFrames.first { processed ->
            processed.sample.bandpassedGreen < -0.1
        }.frame

        assertEquals(rgb(17, 17, 17), positiveFrame.pixels[0])
        assertTrue(red(positiveFrame.pixels[roiIndex()]) > blue(positiveFrame.pixels[roiIndex()]))
        assertTrue(blue(negativeFrame.pixels[roiIndex()]) > red(negativeFrame.pixels[roiIndex()]))
    }

    @Test
    fun splitViewPlacesRawAndProcessedFramesSideBySide() {
        val frames = syntheticGlobalClip(frequencyHz = 1.2)
        val result = RecordedVideoProcessor(
            settings = AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 20.0f,
                viewMode = ViewMode.Split,
            ),
        ).process(frames)

        val splitFrame = result.processedFrames.first().frame
        assertEquals(WIDTH * 2, splitFrame.width)
        assertEquals(HEIGHT, splitFrame.height)
        assertEquals(frames.first().pixels[0], splitFrame.pixels[0])
        assertEquals(rgb24(frames.first().pixels[0]), rgb24(splitFrame.pixels[WIDTH]))

        val changedSplitFrame = result.processedFrames.first { processed ->
            rgb24(processed.frame.pixels[WIDTH]) != rgb24(frames[result.processedFrames.indexOf(processed)].pixels[0])
        }.frame
        assertTrue(rgb24(changedSplitFrame.pixels[WIDTH]) != rgb24(changedSplitFrame.pixels[0]))
    }

    @Test
    fun fastMotionAmplifiedViewUsesPhaseMotionOutput() {
        val frames = translatingEdgeClip(frequencyHz = 6.0)
        val result = RecordedVideoProcessor(
            settings = AnalysisSettings(
                mode = MagnificationMode.Tremor,
                amplification = 8.0f,
                viewMode = ViewMode.Amplified,
            ),
        ).process(frames)

        val changedFrame = result.processedFrames.first { processed ->
            processed.frame.pixels.toList() != frames[result.processedFrames.indexOf(processed)].pixels.toList()
        }
        val sourceFrame = frames[result.processedFrames.indexOf(changedFrame)]
        assertEquals(sourceFrame.width, changedFrame.frame.width)
        assertEquals(sourceFrame.height, changedFrame.frame.height)
    }

    private fun syntheticClip(frequencyHz: Double): List<RgbFrame> {
        return (0 until FRAME_COUNT).map { frameIndex ->
            val seconds = frameIndex / FPS
            val timestampNanos = (seconds * NANOS_PER_SECOND).toLong()
            val green = 128 + (10.0 * sin(2.0 * PI * frequencyHz * seconds)).roundToInt()
            syntheticFrame(timestampNanos, green)
        }
    }

    private fun syntheticFrame(timestampNanos: Long, green: Int): RgbFrame {
        val pixels = IntArray(WIDTH * HEIGHT) { rgb(96, 96, 96) }
        val roi = RecordedVideoAnalyzer.DEFAULT_ROI
        val left = (roi.left * WIDTH).toInt()
        val top = (roi.top * HEIGHT).toInt()
        val right = (roi.right * WIDTH).toInt()
        val bottom = (roi.bottom * HEIGHT).toInt()
        for (y in top until bottom) {
            for (x in left until right) {
                pixels[y * WIDTH + x] = rgb(96, green, 96)
            }
        }
        return RgbFrame(
            width = WIDTH,
            height = HEIGHT,
            timestampNanos = timestampNanos,
            pixels = pixels,
        )
    }

    private fun syntheticGlobalClip(frequencyHz: Double): List<RgbFrame> {
        return (0 until FRAME_COUNT).map { frameIndex ->
            val seconds = frameIndex / FPS
            val timestampNanos = (seconds * NANOS_PER_SECOND).toLong()
            val green = 128 + (10.0 * sin(2.0 * PI * frequencyHz * seconds)).roundToInt()
            RgbFrame(
                width = WIDTH,
                height = HEIGHT,
                timestampNanos = timestampNanos,
                pixels = IntArray(WIDTH * HEIGHT) { rgb(96, green, 96) },
            )
        }
    }

    private fun translatingEdgeClip(frequencyHz: Double): List<RgbFrame> {
        return (0 until FRAME_COUNT).map { frameIndex ->
            val seconds = frameIndex / FPS
            val timestampNanos = (seconds * NANOS_PER_SECOND).toLong()
            val edgeOffset = (2.0 * sin(2.0 * PI * frequencyHz * seconds)).roundToInt()
            val edgeX = WIDTH / 2 + edgeOffset
            RgbFrame(
                width = WIDTH,
                height = HEIGHT,
                timestampNanos = timestampNanos,
                pixels = IntArray(WIDTH * HEIGHT) { index ->
                    val x = index % WIDTH
                    if (x < edgeX) {
                        rgb(48, 48, 48)
                    } else {
                        rgb(208, 208, 208)
                    }
                },
            )
        }
    }

    private fun roiIndex(): Int {
        val roi = RecordedVideoAnalyzer.DEFAULT_ROI
        val x = (((roi.left + roi.right) / 2.0f) * WIDTH).toInt()
        val y = (((roi.top + roi.bottom) / 2.0f) * HEIGHT).toInt()
        return y * WIDTH + x
    }

    private fun rgb(red: Int, green: Int, blue: Int): Int {
        return (0xFF shl 24) or
            (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)
    }

    private fun red(pixel: Int): Int = (pixel shr 16) and 0xFF
    private fun blue(pixel: Int): Int = pixel and 0xFF
    private fun rgb24(pixel: Int): Int = pixel and 0x00FF_FFFF

    companion object {
        private const val WIDTH = 64
        private const val HEIGHT = 48
        private const val FPS = 30.0
        private const val FRAME_COUNT = 120
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
