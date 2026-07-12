package com.dnrohr.eulerianmagnification.gl

import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
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

    val temporalRenderTargetCount: Int get() = GlTemporalStatePlan(pyramidSizes).renderTargetCount
    val passCount: Int get() = pyramidSizes.size + pyramidSizes.size + 1

    companion object {
        const val DEFAULT_LEVEL_COUNT = 3
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

data class LivePyramidLevelPolicy(
    val levelGains: List<Float> = DEFAULT_LEVEL_GAINS,
    val maxDelta: Float = DEFAULT_MAX_DELTA,
) {
    init {
        require(levelGains.isNotEmpty()) { "levelGains must not be empty" }
        require(levelGains.all { it >= 0.0f }) { "level gains must be non-negative" }
        require(maxDelta > 0.0f) { "maxDelta must be positive" }
    }

    fun gainFor(level: Int): Float {
        require(level >= 0) { "level must be non-negative" }
        return levelGains.getOrElse(level) { levelGains.last() }
    }

    companion object {
        val DEFAULT_LEVEL_GAINS = listOf(0.35f, 0.75f, 1.0f)
        const val DEFAULT_MAX_DELTA = 0.18f
    }
}

data class LivePyramidReconstructionProfile(
    val startLevel: Int,
    val levelPolicy: LivePyramidLevelPolicy,
) {
    init {
        require(startLevel >= 0) { "startLevel must be non-negative" }
    }

    companion object {
        val PulseColor = LivePyramidReconstructionProfile(
            startLevel = 0,
            levelPolicy = LivePyramidLevelPolicy(
                levelGains = listOf(0.35f, 0.75f, 1.0f),
                maxDelta = 0.18f,
            ),
        )
        val SlowMotion = LivePyramidReconstructionProfile(
            startLevel = 1,
            levelPolicy = LivePyramidLevelPolicy(
                levelGains = listOf(0.0f, 0.85f, 1.0f),
                maxDelta = 0.16f,
            ),
        )

        fun forMode(mode: MagnificationMode): LivePyramidReconstructionProfile {
            return if (mode == MagnificationMode.Breathing) SlowMotion else PulseColor
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
        uniform int uInitialized;
        in vec2 vTexCoord;
        layout(location = 0) out vec4 outLowpass;
        layout(location = 1) out vec4 outHighpass;
        layout(location = 2) out vec4 outBandpass;

        void main() {
            vec4 current = texture(uCurrentTexture, vTexCoord);
            vec4 previousLow = texture(uPreviousLowTexture, vTexCoord);
            vec4 previousHigh = texture(uPreviousHighTexture, vTexCoord);
            if (uInitialized == 0) {
                outLowpass = current;
                outHighpass = current;
                outBandpass = vec4(0.0);
                return;
            }
            vec4 low = mix(previousLow, current, uLowAlpha);
            vec4 high = mix(previousHigh, current, uHighAlpha);
            outLowpass = low;
            outHighpass = high;
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
        uniform float uLevelGain0;
        uniform float uLevelGain1;
        uniform float uLevelGain2;
        uniform float uMaxDelta;
        uniform int uDifferenceMode;
        uniform int uStartLevel;
        in vec2 vTexCoord;
        out vec4 outColor;

        vec3 gaussianBandpass(int level) {
            if (level == 0) {
                return texture(uBandpassTexture0, vTexCoord).rgb;
            }
            if (level == 1) {
                return texture(uBandpassTexture1, vTexCoord).rgb;
            }
            return texture(uBandpassTexture2, vTexCoord).rgb;
        }

        vec3 laplacianDelta(int level) {
            if (level == 0) {
                return gaussianBandpass(0) - gaussianBandpass(1);
            }
            if (level == 1) {
                return gaussianBandpass(1) - gaussianBandpass(2);
            }
            return gaussianBandpass(2);
        }

        float displayHeadroom(float base, float value) {
            float bounded = clamp(value, 0.0, 1.0);
            if (abs(bounded - base) < 0.0001) {
                return bounded;
            }
            return clamp(bounded, 0.015686, 0.984314);
        }

        vec3 applyDisplayHeadroom(vec3 base, vec3 candidate) {
            return vec3(
                displayHeadroom(base.r, candidate.r),
                displayHeadroom(base.g, candidate.g),
                displayHeadroom(base.b, candidate.b)
            );
        }

        void main() {
            vec4 base = texture(uBaseTexture, vTexCoord);
            vec3 delta = vec3(0.0);
            for (int level = 0; level < 3; level++) {
                if (level >= uStartLevel) {
                    float gain = level == 0 ? uLevelGain0 : (level == 1 ? uLevelGain1 : uLevelGain2);
                    delta += laplacianDelta(level) * gain;
                }
            }
            vec3 amplifiedDelta = clamp(delta * uAmplification, vec3(-uMaxDelta), vec3(uMaxDelta));
            if (uDifferenceMode == 1) {
                float strength = clamp(length(amplifiedDelta) * 4.0, 0.0, 1.0);
                vec3 baseDiff = vec3(0.06);
                vec3 activeDiff = vec3(1.0, 0.45, 0.05);
                outColor = vec4(mix(baseDiff, activeDiff, strength), base.a);
                return;
            }
            outColor = vec4(applyDisplayHeadroom(base.rgb, base.rgb + amplifiedDelta), base.a);
        }
    """
}
