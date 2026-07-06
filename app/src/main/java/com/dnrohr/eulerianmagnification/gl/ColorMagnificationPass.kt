package com.dnrohr.eulerianmagnification.gl

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import com.dnrohr.eulerianmagnification.quality.ArtifactSuppressor

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
        in vec2 vTexCoord;
        out vec4 outColor;

        bool insideRoi(vec2 uv) {
            return uv.x >= uRoi.x && uv.y >= uRoi.y && uv.x <= uRoi.z && uv.y <= uRoi.w;
        }

        void main() {
            vec4 raw = texture(uInputTexture, vTexCoord);
            if (!insideRoi(vTexCoord)) {
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
) {
    fun from(
        sample: AnalysisSample,
        settings: AnalysisSettings,
        presentationTimestampNanos: Long = sample.frameTimestampNanos.coerceAtLeast(0L),
    ): ColorMagnificationUniforms {
        val signal = artifactSuppressor.amplify(sample.bandpassedGreen, settings.amplification)
        val amplifiedSignal = if (settings.viewMode == ViewMode.Raw) {
            0.0
        } else {
            signal.value / ArtifactSuppressor.DEFAULT_MAX_AMPLIFIED_MAGNITUDE
        }
        return ColorMagnificationUniforms(
            roi = sample.roi ?: NormalizedRect(0.0f, 0.0f, 0.0f, 0.0f),
            amplifiedSignal = amplifiedSignal
                .coerceIn(-1.0, 1.0)
                .toFloat(),
            differenceMode = settings.viewMode == ViewMode.Difference,
            splitMode = settings.viewMode == ViewMode.Split,
            presentationTimestampNanos = presentationTimestampNanos.coerceAtLeast(0L),
        )
    }
}

data class ColorMagnificationUniforms(
    val roi: NormalizedRect,
    val amplifiedSignal: Float,
    val differenceMode: Boolean,
    val splitMode: Boolean,
    val presentationTimestampNanos: Long = 0L,
)
