package com.dnrohr.eulerianmagnification.gl

import com.dnrohr.eulerianmagnification.LivePhasePreviewDecision
import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.ColorAmplificationGate
import com.dnrohr.eulerianmagnification.analysis.ColorAmplificationGateResult
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import com.dnrohr.eulerianmagnification.quality.ArtifactSuppressor
import com.dnrohr.eulerianmagnification.quality.LightingDiagnostic

object ColorMagnificationShaderSource {
    const val VERTEX = """#version 300 es
        layout(location = 0) in vec4 aPosition;
        layout(location = 1) in vec2 aTexCoord;
        out vec2 vTexCoord;

        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
    """

    const val FRAGMENT = """#version 300 es
        precision mediump float;
        uniform sampler2D uInputTexture;
        uniform vec4 uRoi;
        uniform float uAmplifiedSignal;
        uniform int uDifferenceMode;
        uniform int uFullFrameMode;
        in vec2 vTexCoord;
        out vec4 outColor;

        bool insideRoi(vec2 uv) {
            return uv.x >= uRoi.x && uv.y >= uRoi.y && uv.x <= uRoi.z && uv.y <= uRoi.w;
        }

        void main() {
            vec4 raw = texture(uInputTexture, vTexCoord);
            if (uFullFrameMode == 0 && !insideRoi(vTexCoord)) {
                outColor = raw;
                return;
            }

            vec3 delta = vec3(uAmplifiedSignal, uAmplifiedSignal * 0.55, uAmplifiedSignal * 0.35);
            if (uDifferenceMode == 1) {
                float strength = clamp(abs(uAmplifiedSignal), 0.0, 1.0);
                vec3 base = vec3(0.06);
                vec3 positive = vec3(1.0, 0.35, 0.05);
                vec3 negative = vec3(0.05, 0.45, 1.0);
                vec3 signedColor = uAmplifiedSignal >= 0.0 ? positive : negative;
                outColor = vec4(mix(base, signedColor, strength), raw.a);
            } else {
                outColor = vec4(clamp(raw.rgb + delta, 0.0, 1.0), raw.a);
            }
        }
    """
}

class ColorMagnificationParameters(
    private val artifactSuppressor: ArtifactSuppressor = ArtifactSuppressor(),
    private val colorAmplificationGate: ColorAmplificationGate = ColorAmplificationGate(),
) {
    fun from(
        sample: AnalysisSample,
        settings: AnalysisSettings,
        fullFrameMode: Boolean = false,
        presentationTimestampNanos: Long = sample.frameTimestampNanos.coerceAtLeast(0L),
        livePhasePreviewDecision: LivePhasePreviewDecision? = null,
        lightingDiagnostic: LightingDiagnostic? = null,
    ): ColorMagnificationUniforms {
        val signal = artifactSuppressor.amplify(sample.bandpassedGreen, settings.amplification)
        val colorGate = lightingDiagnostic?.let {
            colorAmplificationGate.evaluate(
                mode = settings.mode,
                lighting = it,
                saturatedPixelFraction = sample.saturatedPixelFraction,
            )
        } ?: ColorAmplificationGateResult.Stable
        val amplifiedSignal = if (settings.viewMode == ViewMode.Raw) {
            0.0
        } else {
            (signal.value / ArtifactSuppressor.DEFAULT_MAX_AMPLIFIED_MAGNITUDE) *
                colorGate.effectiveAmplificationScale
        }
        val reconstructionAmplification = if (settings.viewMode == ViewMode.Raw) {
            0.0f
        } else {
            settings.amplification * colorGate.effectiveAmplificationScale
        }
        return ColorMagnificationUniforms(
            roi = sample.roi ?: NormalizedRect(0.0f, 0.0f, 0.0f, 0.0f),
            amplifiedSignal = amplifiedSignal
                .coerceIn(-1.0, 1.0)
                .toFloat(),
            differenceMode = settings.viewMode == ViewMode.Difference,
            splitMode = settings.viewMode == ViewMode.Split,
            fullFrameMode = fullFrameMode,
            presentationTimestampNanos = presentationTimestampNanos.coerceAtLeast(0L),
            amplification = settings.amplification,
            reconstructionAmplification = reconstructionAmplification,
            lowCutHz = settings.lowCutHz,
            highCutHz = settings.highCutHz,
            reconstructionProfile = LivePyramidReconstructionProfile.forMode(settings.mode),
            colorGate = colorGate,
            livePhaseRoiPlan = livePhasePreviewDecision?.roiPlan,
            livePhaseDiagnostics = livePhasePreviewDecision?.diagnostics ?: LivePhaseDiagnostics(requested = false),
        )
    }
}

data class ColorMagnificationUniforms(
    val roi: NormalizedRect,
    val amplifiedSignal: Float,
    val differenceMode: Boolean,
    val splitMode: Boolean,
    val fullFrameMode: Boolean = false,
    val presentationTimestampNanos: Long = 0L,
    val amplification: Float = 0.0f,
    val reconstructionAmplification: Float = amplification,
    val lowCutHz: Double = 0.7,
    val highCutHz: Double = 3.0,
    val reconstructionProfile: LivePyramidReconstructionProfile = LivePyramidReconstructionProfile.PulseColor,
    val colorGate: ColorAmplificationGateResult = ColorAmplificationGateResult.Stable,
    val livePhaseRoiPlan: LivePhaseRoiPlan? = null,
    val livePhaseDiagnostics: LivePhaseDiagnostics = LivePhaseDiagnostics(requested = false),
)
