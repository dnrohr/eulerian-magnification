package com.dnrohr.eulerianmagnification.analysis

import com.dnrohr.eulerianmagnification.quality.LightingDiagnostic
import com.dnrohr.eulerianmagnification.quality.LightingDiagnosticStatus

enum class ColorAmplificationGateReason(
    val code: String,
    val label: String,
    val action: String,
) {
    Stable("stable", "Color amplification stable", "Keep this setup."),
    LightingSettling("lighting_settling", "Lighting settling", "Wait before judging color amplification."),
    TooDark("too_dark", "Too dark for color amplification", "Use brighter, steady light."),
    LightingFlicker("lighting_flicker", "Lighting flicker dampened", "Try daylight or a non-flickering lamp."),
    ExposurePumping("exposure_pumping", "Exposure pumping dampened", "Wait for exposure to settle, then lock AE/AWB."),
    MotionContaminated("motion_contaminated", "Motion-contaminated color dampened", "Mount the phone or redraw the ROI."),
    Saturated("saturated", "Color clipping dampened", "Reduce brightness or lower amplification."),
}

data class ColorAmplificationGateResult(
    val gain: Float,
    val reason: ColorAmplificationGateReason,
    val saturatedPixelFraction: Double = 0.0,
) {
    val attenuated: Boolean get() = gain < 1.0f
    val effectiveAmplificationScale: Float get() = gain.coerceIn(0.0f, 1.0f)

    companion object {
        val Stable = ColorAmplificationGateResult(
            gain = 1.0f,
            reason = ColorAmplificationGateReason.Stable,
        )
    }
}

class ColorAmplificationGate(
    private val saturationFractionThreshold: Double = DEFAULT_SATURATION_FRACTION_THRESHOLD,
) {
    fun evaluate(
        mode: MagnificationMode,
        lighting: LightingDiagnostic,
        saturatedPixelFraction: Double,
    ): ColorAmplificationGateResult {
        if (mode != MagnificationMode.Pulse) return ColorAmplificationGateResult.Stable

        val lightingResult = when (lighting.status) {
            LightingDiagnosticStatus.Stable -> ColorAmplificationGateResult.Stable
            LightingDiagnosticStatus.Settling -> result(
                gain = 0.35f,
                reason = ColorAmplificationGateReason.LightingSettling,
                saturatedPixelFraction = saturatedPixelFraction,
            )
            LightingDiagnosticStatus.TooDark -> result(
                gain = 0.0f,
                reason = ColorAmplificationGateReason.TooDark,
                saturatedPixelFraction = saturatedPixelFraction,
            )
            LightingDiagnosticStatus.FlickerLikely -> result(
                gain = 0.25f,
                reason = ColorAmplificationGateReason.LightingFlicker,
                saturatedPixelFraction = saturatedPixelFraction,
            )
            LightingDiagnosticStatus.ExposurePumping -> result(
                gain = 0.25f,
                reason = ColorAmplificationGateReason.ExposurePumping,
                saturatedPixelFraction = saturatedPixelFraction,
            )
            LightingDiagnosticStatus.MotionContaminated -> result(
                gain = 0.5f,
                reason = ColorAmplificationGateReason.MotionContaminated,
                saturatedPixelFraction = saturatedPixelFraction,
            )
        }

        if (saturatedPixelFraction < saturationFractionThreshold) return lightingResult
        val saturatedResult = result(
            gain = 0.4f,
            reason = ColorAmplificationGateReason.Saturated,
            saturatedPixelFraction = saturatedPixelFraction,
        )
        return if (saturatedResult.gain < lightingResult.gain) saturatedResult else lightingResult
    }

    private fun result(
        gain: Float,
        reason: ColorAmplificationGateReason,
        saturatedPixelFraction: Double,
    ): ColorAmplificationGateResult {
        return ColorAmplificationGateResult(
            gain = gain,
            reason = reason,
            saturatedPixelFraction = saturatedPixelFraction,
        )
    }

    companion object {
        private const val DEFAULT_SATURATION_FRACTION_THRESHOLD = 0.02
    }
}
