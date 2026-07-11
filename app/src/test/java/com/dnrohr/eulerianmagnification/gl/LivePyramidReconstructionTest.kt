package com.dnrohr.eulerianmagnification.gl

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
    fun shadersExposeDownsampleTemporalAndReconstructionUniforms() {
        assertTrue(LivePyramidShaderSource.VERTEX.startsWith("#version 300 es"))
        assertTrue(LivePyramidShaderSource.DOWNSAMPLE_FRAGMENT.contains("uTexelSize"))
        assertTrue(LivePyramidShaderSource.DOWNSAMPLE_FRAGMENT.contains("center * 4.0"))

        val temporal = LivePyramidShaderSource.TEMPORAL_BANDPASS_FRAGMENT
        assertTrue(temporal.startsWith("#version 300 es"))
        assertTrue(temporal.contains("uPreviousLowTexture"))
        assertTrue(temporal.contains("uPreviousHighTexture"))
        assertTrue(temporal.contains("outHighpass = high"))
        assertTrue(temporal.contains("outBandpass = high - low"))

        val reconstruct = LivePyramidShaderSource.RECONSTRUCT_FRAGMENT
        assertTrue(reconstruct.startsWith("#version 300 es"))
        assertTrue(reconstruct.contains("uBandpassTexture0"))
        assertTrue(reconstruct.contains("uAmplification"))
        assertTrue(reconstruct.contains("uStartLevel"))
        assertTrue(reconstruct.contains("delta * uAmplification"))
    }
}
