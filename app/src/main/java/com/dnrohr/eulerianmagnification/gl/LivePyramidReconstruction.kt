package com.dnrohr.eulerianmagnification.gl

import kotlin.math.PI
import kotlin.math.exp

data class LivePyramidReconstructionPlan(
    val surfaceSize: GlTextureSize,
    val levelCount: Int = DEFAULT_LEVEL_COUNT,
) {
    val pyramidSizes: List<GlTextureSize> = GlPyramid.pyramidSizes(
        baseSize = GlTextureSize(
            width = (surfaceSize.width / 2).coerceAtLeast(1),
            height = (surfaceSize.height / 2).coerceAtLeast(1),
        ),
        levelCount = levelCount,
    )

    val temporalRenderTargetCount: Int get() = pyramidSizes.size * TEMPORAL_TARGETS_PER_LEVEL
    val passCount: Int get() = 1 + pyramidSizes.size + 1

    companion object {
        const val DEFAULT_LEVEL_COUNT = 3
        private const val TEMPORAL_TARGETS_PER_LEVEL = 2
    }
}

data class TemporalBandpassCoefficients(
    val lowAlpha: Float,
    val highAlpha: Float,
) {
    init {
        require(lowAlpha in 0.0f..1.0f) { "lowAlpha must be in 0..1" }
        require(highAlpha in 0.0f..1.0f) { "highAlpha must be in 0..1" }
    }

    companion object {
        fun from(
            lowCutHz: Double,
            highCutHz: Double,
            dtSeconds: Double,
        ): TemporalBandpassCoefficients {
            require(lowCutHz > 0.0) { "lowCutHz must be positive" }
            require(highCutHz > lowCutHz) { "highCutHz must be above lowCutHz" }
            require(dtSeconds > 0.0) { "dtSeconds must be positive" }
            return TemporalBandpassCoefficients(
                lowAlpha = alpha(lowCutHz, dtSeconds).toFloat(),
                highAlpha = alpha(highCutHz, dtSeconds).toFloat(),
            )
        }

        private fun alpha(cutoffHz: Double, dtSeconds: Double): Double {
            return 1.0 - exp(-2.0 * PI * cutoffHz * dtSeconds)
        }
    }
}

object LivePyramidShaderSource {
    const val VERTEX = RgbTextureShaderSource.VERTEX

    const val DOWNSAMPLE_FRAGMENT = """#version 300 es
        precision mediump float;
        uniform sampler2D uInputTexture;
        uniform vec2 uTexelSize;
        in vec2 vTexCoord;
        out vec4 outColor;

        void main() {
            vec4 center = texture(uInputTexture, vTexCoord);
            vec4 left = texture(uInputTexture, vTexCoord + vec2(-uTexelSize.x, 0.0));
            vec4 right = texture(uInputTexture, vTexCoord + vec2(uTexelSize.x, 0.0));
            vec4 up = texture(uInputTexture, vTexCoord + vec2(0.0, -uTexelSize.y));
            vec4 down = texture(uInputTexture, vTexCoord + vec2(0.0, uTexelSize.y));
            outColor = (center * 4.0 + left + right + up + down) / 8.0;
        }
    """

    const val TEMPORAL_BANDPASS_FRAGMENT = """#version 300 es
        precision mediump float;
        uniform sampler2D uCurrentTexture;
        uniform sampler2D uPreviousLowTexture;
        uniform sampler2D uPreviousHighTexture;
        uniform float uLowAlpha;
        uniform float uHighAlpha;
        in vec2 vTexCoord;
        layout(location = 0) out vec4 outLowpass;
        layout(location = 1) out vec4 outBandpass;

        void main() {
            vec4 current = texture(uCurrentTexture, vTexCoord);
            vec4 previousLow = texture(uPreviousLowTexture, vTexCoord);
            vec4 previousHigh = texture(uPreviousHighTexture, vTexCoord);
            vec4 low = mix(previousLow, current, uLowAlpha);
            vec4 high = mix(previousHigh, current, uHighAlpha);
            outLowpass = low;
            outBandpass = high - low;
        }
    """

    const val RECONSTRUCT_FRAGMENT = """#version 300 es
        precision mediump float;
        uniform sampler2D uBaseTexture;
        uniform sampler2D uBandpassTexture0;
        uniform sampler2D uBandpassTexture1;
        uniform sampler2D uBandpassTexture2;
        uniform float uAmplification;
        uniform int uStartLevel;
        in vec2 vTexCoord;
        out vec4 outColor;

        vec3 levelDelta(int level) {
            if (level == 0) {
                return texture(uBandpassTexture0, vTexCoord).rgb;
            }
            if (level == 1) {
                return texture(uBandpassTexture1, vTexCoord).rgb;
            }
            return texture(uBandpassTexture2, vTexCoord).rgb;
        }

        void main() {
            vec4 base = texture(uBaseTexture, vTexCoord);
            vec3 delta = vec3(0.0);
            for (int level = 0; level < 3; level++) {
                if (level >= uStartLevel) {
                    delta += levelDelta(level);
                }
            }
            outColor = vec4(clamp(base.rgb + delta * uAmplification, 0.0, 1.0), base.a);
        }
    """
}
