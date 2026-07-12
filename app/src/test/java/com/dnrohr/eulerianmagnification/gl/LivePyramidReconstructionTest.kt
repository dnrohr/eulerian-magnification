package com.dnrohr.eulerianmagnification.gl

import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LivePyramidReconstructionTest {
    @Test
    fun planUsesHalfResolutionPyramidAndTemporalPingPongTargets() {
        val plan = LivePyramidReconstructionPlan(GlTextureSize(1280, 720))

        assertEquals(
            listOf(
                GlTextureSize(640, 360),
                GlTextureSize(320, 180),
                GlTextureSize(160, 90),
            ),
            plan.pyramidSizes,
        )
        assertEquals(15, plan.temporalRenderTargetCount)
        assertEquals(7, plan.passCount)
    }

    @Test
    fun coefficientsMatchTemporalBandpassFormula() {
        val coefficients = TemporalBandpassCoefficients.from(
            lowCutHz = 0.7,
            highCutHz = 3.0,
            dtSeconds = 1.0 / 30.0,
        )

        assertTrue(coefficients.lowAlpha > 0.0f)
        assertTrue(coefficients.highAlpha > coefficients.lowAlpha)
        assertTrue(coefficients.highAlpha < 1.0f)
    }

    @Test
    fun coefficientsRejectInvalidBands() {
        assertThrows(IllegalArgumentException::class.java) {
            TemporalBandpassCoefficients.from(
                lowCutHz = 3.0,
                highCutHz = 0.7,
                dtSeconds = 1.0 / 30.0,
            )
        }
    }

    @Test
    fun levelPolicyAttenuatesFineLevelsAndClampsDelta() {
        val policy = LivePyramidLevelPolicy()

        assertEquals(0.35f, policy.gainFor(0), 0.0f)
        assertEquals(0.75f, policy.gainFor(1), 0.0f)
        assertEquals(1.0f, policy.gainFor(2), 0.0f)
        assertEquals(1.0f, policy.gainFor(10), 0.0f)
        assertEquals(0.18f, policy.maxDelta, 0.0f)
    }

    @Test
    fun levelPolicyRejectsInvalidValues() {
        assertThrows(IllegalArgumentException::class.java) {
            LivePyramidLevelPolicy(levelGains = emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            LivePyramidLevelPolicy(levelGains = listOf(1.0f, -0.1f))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LivePyramidLevelPolicy(maxDelta = 0.0f)
        }
    }

    @Test
    fun profilesChooseFineColorOrCoarseSlowMotionLevels() {
        val pulse = LivePyramidReconstructionProfile.forMode(MagnificationMode.Pulse)
        val breathing = LivePyramidReconstructionProfile.forMode(MagnificationMode.Breathing)

        assertEquals(0, pulse.startLevel)
        assertEquals(0.35f, pulse.levelPolicy.gainFor(0), 0.0f)
        assertEquals(0.18f, pulse.levelPolicy.maxDelta, 0.0f)

        assertEquals(1, breathing.startLevel)
        assertEquals(0.0f, breathing.levelPolicy.gainFor(0), 0.0f)
        assertEquals(0.85f, breathing.levelPolicy.gainFor(1), 0.0f)
        assertEquals(0.16f, breathing.levelPolicy.maxDelta, 0.0f)
    }

    @Test
    fun profileRejectsInvalidStartLevel() {
        assertThrows(IllegalArgumentException::class.java) {
            LivePyramidReconstructionProfile(
                startLevel = -1,
                levelPolicy = LivePyramidLevelPolicy(),
            )
        }
    }

    @Test
    fun shadersExposeDownsampleTemporalAndReconstructionUniforms() {
        assertTrue(LivePyramidShaderSource.VERTEX.startsWith("#version 300 es"))
        assertTrue(LivePyramidShaderSource.DOWNSAMPLE_FRAGMENT.contains("uTexelSize"))
        assertTrue(LivePyramidShaderSource.DOWNSAMPLE_FRAGMENT.contains("center * 4.0"))

        val temporal = LivePyramidShaderSource.TEMPORAL_BANDPASS_FRAGMENT
        assertTrue(temporal.startsWith("#version 300 es"))
        assertTrue(temporal.contains("uPreviousLowTexture"))
        assertTrue(temporal.contains("uPreviousHighTexture"))
        assertTrue(temporal.contains("uInitialized"))
        assertTrue(temporal.contains("outBandpass = vec4(0.0)"))
        assertTrue(temporal.contains("outHighpass = high"))
        assertTrue(temporal.contains("outBandpass = high - low"))

        val reconstruct = LivePyramidShaderSource.RECONSTRUCT_FRAGMENT
        assertTrue(reconstruct.startsWith("#version 300 es"))
        assertTrue(reconstruct.contains("uBandpassTexture0"))
        assertTrue(reconstruct.contains("uBandpassTexture1"))
        assertTrue(reconstruct.contains("uBandpassTexture2"))
        assertTrue(reconstruct.contains("uAmplification"))
        assertTrue(!reconstruct.contains("uAmplifiedSignal"))
        assertTrue(reconstruct.contains("uLevelGain0"))
        assertTrue(reconstruct.contains("uLevelGain1"))
        assertTrue(reconstruct.contains("uLevelGain2"))
        assertTrue(reconstruct.contains("uMaxDelta"))
        assertTrue(reconstruct.contains("uDifferenceMode"))
        assertTrue(reconstruct.contains("uStartLevel"))
        assertTrue(reconstruct.contains("vec3 laplacianDelta(int level)"))
        assertTrue(reconstruct.contains("gaussianBandpass(0) - gaussianBandpass(1)"))
        assertTrue(reconstruct.contains("gaussianBandpass(1) - gaussianBandpass(2)"))
        assertTrue(reconstruct.contains("return gaussianBandpass(2)"))
        assertTrue(reconstruct.contains("laplacianDelta(level) * gain"))
        assertTrue(reconstruct.contains("clamp(delta * uAmplification"))
        assertTrue(reconstruct.contains("if (uDifferenceMode == 1)"))
        assertTrue(reconstruct.contains("length(amplifiedDelta)"))
        assertTrue(reconstruct.contains("float displayHeadroom(float base, float value)"))
        assertTrue(reconstruct.contains("clamp(bounded, 0.015686, 0.984314)"))
        assertTrue(reconstruct.contains("applyDisplayHeadroom(base.rgb, base.rgb + amplifiedDelta)"))
    }
}
