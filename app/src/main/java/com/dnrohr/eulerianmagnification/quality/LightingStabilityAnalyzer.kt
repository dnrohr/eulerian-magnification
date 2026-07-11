package com.dnrohr.eulerianmagnification.quality

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import kotlin.math.pow
import kotlin.math.sqrt

enum class LightingDiagnosticStatus(
    val code: String,
    val label: String,
    val action: String,
) {
    Settling("settling", "Lighting settling", "Wait a few seconds before locking AE/AWB."),
    Stable("stable", "Lighting stable", "Keep this lighting and lock AE/AWB if it looks good."),
    TooDark("too_dark", "Lighting too dark", "Use brighter, diffuse light before validating."),
    FlickerLikely("flicker_likely", "Lighting flicker", "Try daylight or a non-flickering lamp, then lock AE/AWB."),
    ExposurePumping("exposure_pumping", "Exposure unstable", "Wait for exposure to settle, then lock AE/AWB."),
    MotionContaminated("motion_contaminated", "Lighting mixed with ROI motion", "Mount the phone or redraw the ROI before judging lighting."),
}

data class LightingDiagnostic(
    val status: LightingDiagnosticStatus,
    val averageGreen: Double,
    val coefficientOfVariation: Double,
    val flickerLikely: Boolean,
    val motionMagnitude: Float,
) {
    val label: String get() = status.label
    val action: String get() = status.action
    val metadataValue: String get() = status.code
}

class LightingStabilityAnalyzer(
    private val windowSize: Int = 30,
    private val minSamples: Int = 8,
    private val darkGreenThreshold: Double = 45.0,
    private val exposureVariationThreshold: Double = 0.055,
    private val motionThreshold: Float = 0.018f,
    private val flickerDetector: LightingFlickerDetector = LightingFlickerDetector(),
) {
    private val greenSamples = ArrayDeque<Double>()

    fun update(sample: AnalysisSample): LightingDiagnostic {
        greenSamples.addLast(sample.averageGreen)
        while (greenSamples.size > windowSize) {
            greenSamples.removeFirst()
        }

        val averageGreen = greenSamples.average()
        val coefficientOfVariation = coefficientOfVariation(averageGreen)
        val flickerLikely = flickerDetector.update(sample.averageGreen)
        val motionMagnitude = sample.translation.magnitude
        val status = when {
            greenSamples.size < minSamples -> LightingDiagnosticStatus.Settling
            averageGreen in 0.01..darkGreenThreshold -> LightingDiagnosticStatus.TooDark
            flickerLikely && motionMagnitude >= motionThreshold -> LightingDiagnosticStatus.MotionContaminated
            flickerLikely -> LightingDiagnosticStatus.FlickerLikely
            coefficientOfVariation >= exposureVariationThreshold && motionMagnitude >= motionThreshold ->
                LightingDiagnosticStatus.MotionContaminated
            coefficientOfVariation >= exposureVariationThreshold -> LightingDiagnosticStatus.ExposurePumping
            else -> LightingDiagnosticStatus.Stable
        }

        return LightingDiagnostic(
            status = status,
            averageGreen = averageGreen,
            coefficientOfVariation = coefficientOfVariation,
            flickerLikely = flickerLikely,
            motionMagnitude = motionMagnitude,
        )
    }

    private fun coefficientOfVariation(mean: Double): Double {
        if (greenSamples.isEmpty() || mean <= 0.0) return 0.0
        val variance = greenSamples
            .map { value -> (value - mean).pow(2.0) }
            .average()
        return sqrt(variance) / mean
    }
}
