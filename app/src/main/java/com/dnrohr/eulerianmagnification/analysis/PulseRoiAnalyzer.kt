package com.dnrohr.eulerianmagnification.analysis

import android.graphics.Rect
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.dnrohr.eulerianmagnification.profiling.FpsMeter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class PulseRoiAnalyzer(
    private val settingsProvider: () -> AnalysisSettings,
    private val roiSourceProvider: () -> RoiSource,
    private val manualRoiProvider: () -> NormalizedRect?,
    private val onSample: (AnalysisSample) -> Unit,
) : ImageAnalysis.Analyzer {
    constructor(
        settings: AnalysisSettings,
        roiSource: RoiSource = RoiSource.Auto,
        manualRoi: NormalizedRect? = null,
        onSample: (AnalysisSample) -> Unit,
    ) : this(
        settingsProvider = { settings },
        roiSourceProvider = { roiSource },
        manualRoiProvider = { manualRoi },
        onSample = onSample,
    )

    private val fpsMeter = FpsMeter()
    private var bandpassFilter = newBandpassFilter(settingsProvider())
    private var activeLowCutHz = settingsProvider().lowCutHz
    private var activeHighCutHz = settingsProvider().highCutHz
    private val roiSmoother = RoiSmoother()
    private val roiTracker = RoiTracker()
    private val timestampTracker = TimestampTracker()
    private val translationEstimator = TranslationEstimator()
    private val detectorBusy = AtomicBoolean(false)
    private var latestFaceBounds: NormalizedRect? = null
    private var missedDetections = 0
    private var frameIndex = 0

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.12f)
            .enableTracking()
            .build(),
    )

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val settings = settingsProvider()
        val roiSource = roiSourceProvider()
        val manualRoi = manualRoiProvider()
        val bandpassFilter = bandpassFilterFor(settings)
        val timestamp = imageProxy.imageInfo.timestamp
        val timestampStatus = timestampTracker.record(timestamp)
        fpsMeter.recordFrame(timestamp)
        latestFaceBounds = roiTracker.holdLastDetection() ?: latestFaceBounds
        val activeAutoRoi = latestFaceBounds
        val activeManualRoi = manualRoi.takeIf { roiSource == RoiSource.Manual }
        val roiState = when {
            roiSource == RoiSource.FullFrame -> RoiState.FullFrame
            activeManualRoi != null -> RoiState.Manual
            activeAutoRoi != null && missedDetections > 0 -> RoiState.Frozen
            activeAutoRoi != null -> RoiState.Tracking
            else -> RoiState.Center
        }
        val roi = when (roiSource) {
            RoiSource.FullFrame -> RoiSourcePolicy.FULL_FRAME_ROI.toRect(imageProxy.width, imageProxy.height)
            RoiSource.Manual -> activeManualRoi
            ?.toRect(imageProxy.width, imageProxy.height)
                ?: centeredRoi(imageProxy.width, imageProxy.height)
            RoiSource.Auto -> activeAutoRoi
                ?.toRect(imageProxy.width, imageProxy.height)
                ?.let { skinSubregion(it, imageProxy.width, imageProxy.height) }
                ?: centeredRoi(imageProxy.width, imageProxy.height)
        }
        val colorStats = colorStats(imageProxy, roi)
        val bandpassed = bandpassFilter.update(colorStats.averageGreen, timestamp)
        val normalizedRoi = roi.toNormalized(imageProxy.width, imageProxy.height)
        val translation = translationEstimator.update(normalizedRoi)

        onSample(
            AnalysisSample(
                analysisFps = fpsMeter.framesPerSecond(),
                roi = normalizedRoi,
                averageGreen = colorStats.averageGreen,
                saturatedPixelFraction = colorStats.saturatedPixelFraction,
                bandpassedGreen = bandpassed,
                latencyMillis = (System.nanoTime() - timestamp).coerceAtLeast(0L) / NANOS_PER_MILLISECOND,
                timestampMonotonic = timestampStatus.isMonotonic,
                translation = translation,
                frameTimestampNanos = timestamp,
                frameWidth = imageProxy.width,
                frameHeight = imageProxy.height,
                rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                roiState = roiState,
            ),
        )

        if (roiSource != RoiSource.Auto) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        val shouldDetect = frameIndex++ % DETECTION_INTERVAL_FRAMES == 0
        if (mediaImage != null && shouldDetect && detectorBusy.compareAndSet(false, true)) {
            val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(input)
                .addOnSuccessListener { faces ->
                    val detected = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                        ?.boundingBox
                        ?.clamped(imageProxy.width, imageProxy.height)
                        ?.toNormalized(imageProxy.width, imageProxy.height)
                        ?.let(roiSmoother::update)
                        ?.let(roiTracker::updateDetection)
                    if (detected != null) {
                        missedDetections = 0
                        latestFaceBounds = detected
                    } else {
                        missedDetections += 1
                        if (missedDetections >= MAX_MISSED_DETECTIONS) {
                            latestFaceBounds = null
                            roiTracker.reset()
                            roiSmoother.reset()
                        }
                    }
                }
                .addOnCompleteListener {
                    detectorBusy.set(false)
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun centeredRoi(width: Int, height: Int): Rect {
        val roiWidth = (width * 0.28f).toInt()
        val roiHeight = (height * 0.20f).toInt()
        val centerX = width / 2
        val centerY = (height * 0.42f).toInt()
        return Rect(
            centerX - roiWidth / 2,
            centerY - roiHeight / 2,
            centerX + roiWidth / 2,
            centerY + roiHeight / 2,
        ).clamped(width, height)
    }

    private fun skinSubregion(face: Rect, width: Int, height: Int): Rect {
        val roiWidth = (face.width() * 0.46f).toInt()
        val roiHeight = (face.height() * 0.22f).toInt()
        val centerX = face.centerX()
        val centerY = face.top + (face.height() * 0.36f).toInt()
        return Rect(
            centerX - roiWidth / 2,
            centerY - roiHeight / 2,
            centerX + roiWidth / 2,
            centerY + roiHeight / 2,
        ).clamped(width, height)
    }

    private fun colorStats(imageProxy: ImageProxy, roi: Rect): RoiColorStats {
        val yPlane = imageProxy.planes[0]
        val uPlane = imageProxy.planes[1]
        val vPlane = imageProxy.planes[2]
        var greenSum = 0.0
        var count = 0
        var saturated = 0
        val stepX = (roi.width() / SAMPLE_GRID).coerceAtLeast(1)
        val stepY = (roi.height() / SAMPLE_GRID).coerceAtLeast(1)

        var y = roi.top
        while (y < roi.bottom) {
            var x = roi.left
            while (x < roi.right) {
                val sample = colorAt(
                    x = x,
                    y = y,
                    yPlane = yPlane,
                    uPlane = uPlane,
                    vPlane = vPlane,
                )
                greenSum += sample.green
                if (sample.saturated) saturated++
                count++
                x += stepX
            }
            y += stepY
        }

        return if (count == 0) {
            RoiColorStats()
        } else {
            RoiColorStats(
                averageGreen = greenSum / count,
                saturatedPixelFraction = saturated / count.toDouble(),
            )
        }
    }

    private fun colorAt(
        x: Int,
        y: Int,
        yPlane: ImageProxy.PlaneProxy,
        uPlane: ImageProxy.PlaneProxy,
        vPlane: ImageProxy.PlaneProxy,
    ): YuvColorSample {
        val yValue = yPlane.buffer.unsigned(y * yPlane.rowStride + x)
        val uvX = x / 2
        val uvY = y / 2
        val uIndex = uvY * uPlane.rowStride + uvX * uPlane.pixelStride
        val vIndex = uvY * vPlane.rowStride + uvX * vPlane.pixelStride
        return YuvColorSample.fromYuv(
            y = yValue,
            u = uPlane.buffer.unsigned(uIndex),
            v = vPlane.buffer.unsigned(vIndex),
        )
    }

    private data class RoiColorStats(
        val averageGreen: Double = 0.0,
        val saturatedPixelFraction: Double = 0.0,
    )

    private fun ByteBuffer.unsigned(index: Int): Double {
        return (get(index).toInt() and 0xFF).toDouble()
    }

    private fun Rect.clamped(width: Int, height: Int): Rect {
        return Rect(
            left.coerceIn(0, width - 1),
            top.coerceIn(0, height - 1),
            right.coerceIn(1, width),
            bottom.coerceIn(1, height),
        )
    }

    private fun Rect.toNormalized(width: Int, height: Int): NormalizedRect {
        return NormalizedRect(
            left = left / width.toFloat(),
            top = top / height.toFloat(),
            right = right / width.toFloat(),
            bottom = bottom / height.toFloat(),
        )
    }

    private fun NormalizedRect.toRect(width: Int, height: Int): Rect {
        return Rect(
            (left * width).toInt(),
            (top * height).toInt(),
            (right * width).toInt(),
            (bottom * height).toInt(),
        ).clamped(width, height)
    }

    private fun bandpassFilterFor(settings: AnalysisSettings): BandpassFilter {
        if (settings.lowCutHz != activeLowCutHz || settings.highCutHz != activeHighCutHz) {
            activeLowCutHz = settings.lowCutHz
            activeHighCutHz = settings.highCutHz
            bandpassFilter = newBandpassFilter(settings)
        }
        return bandpassFilter
    }

    private fun newBandpassFilter(settings: AnalysisSettings): BandpassFilter {
        return BandpassFilter(
            lowCutHz = settings.lowCutHz,
            highCutHz = settings.highCutHz,
        )
    }

    companion object {
        private const val DETECTION_INTERVAL_FRAMES = 10
        private const val MAX_MISSED_DETECTIONS = 12
        private const val SAMPLE_GRID = 16
        private const val NANOS_PER_MILLISECOND = 1_000_000.0
    }
}
