package com.dnrohr.eulerianmagnification.gl

import org.junit.Assert.assertTrue
import org.junit.Test

class RieszPhaseShaderSourceTest {
    @Test
    fun shadersStartWithVersionDeclaration() {
        assertTrue(RieszPhaseShaderSource.VERTEX.startsWith("#version 300 es"))
        assertTrue(RieszPhaseShaderSource.RIESZ_COMPONENT_FRAGMENT.startsWith("#version 300 es"))
        assertTrue(RieszPhaseShaderSource.PHASE_PROJECT_FRAGMENT.startsWith("#version 300 es"))
        assertTrue(RieszPhaseShaderSource.PHASE_AMPLIFY_FRAGMENT.startsWith("#version 300 es"))
        assertTrue(RieszPhaseShaderSource.LIVE_PHASE_TEMPORAL_FRAGMENT.startsWith("#version 300 es"))
        assertTrue(RieszPhaseShaderSource.PHASE_RECONSTRUCT_FRAGMENT.startsWith("#version 300 es"))
    }

    @Test
    fun componentShaderComputesLuminanceAndCentralDifferences() {
        val shader = RieszPhaseShaderSource.RIESZ_COMPONENT_FRAGMENT

        assertTrue(shader.contains("dot(rgb, vec3(0.299, 0.587, 0.114))"))
        assertTrue(shader.contains("right - left"))
        assertTrue(shader.contains("down - up"))
        assertTrue(shader.contains("uTexelSize"))
    }

    @Test
    fun phaseProjectShaderUsesDominantOrientationUniform() {
        val shader = RieszPhaseShaderSource.PHASE_PROJECT_FRAGMENT

        assertTrue(shader.contains("uOrientationRadians"))
        assertTrue(shader.contains("cos(uOrientationRadians)"))
        assertTrue(shader.contains("sin(uOrientationRadians)"))
        assertTrue(shader.contains("atan(oriented, source)"))
    }

    @Test
    fun phaseAmplifyShaderWrapsPhaseDelta() {
        val shader = RieszPhaseShaderSource.PHASE_AMPLIFY_FRAGMENT

        assertTrue(shader.contains("uAmplification"))
        assertTrue(shader.contains("atan(sin(currentPhase - referencePhase), cos(currentPhase - referencePhase))"))
        assertTrue(shader.contains("delta * uAmplification"))
    }

    @Test
    fun liveTemporalShaderPortsWrappedPhaseStateUpdate() {
        val shader = RieszPhaseShaderSource.LIVE_PHASE_TEMPORAL_FRAGMENT

        assertTrue(shader.contains("uPreviousWrappedPhaseTexture"))
        assertTrue(shader.contains("uPreviousUnwrappedPhaseTexture"))
        assertTrue(shader.contains("wrapDelta(currentPhase - previousWrappedPhase)"))
        assertTrue(shader.contains("previousUnwrapped + wrapDelta"))
        assertTrue(shader.contains("outUnwrappedPhase"))
    }

    @Test
    fun liveTemporalShaderUsesWarmupBandpassAndAmplitudeGate() {
        val shader = RieszPhaseShaderSource.LIVE_PHASE_TEMPORAL_FRAGMENT

        assertTrue(shader.contains("uInitialized"))
        assertTrue(shader.contains("if (uInitialized == 0)"))
        assertTrue(shader.contains("outAmplifiedPhase = vec4(encodePhase(currentPhase)"))
        assertTrue(shader.contains("uPreviousLowTexture"))
        assertTrue(shader.contains("uPreviousHighTexture"))
        assertTrue(shader.contains("mix(previousLow, unwrappedPhase, uLowAlpha)"))
        assertTrue(shader.contains("mix(previousHigh, unwrappedPhase, uHighAlpha)"))
        assertTrue(shader.contains("float bandpassedPhase = high - low"))
        assertTrue(shader.contains("current.g >= uAmplitudeThreshold ? uAmplification : 0.0"))
        assertTrue(shader.contains("bandpassedPhase * gatedAmplification"))
    }

    @Test
    fun phaseReconstructShaderUsesAmplitudeAndCosine() {
        val shader = RieszPhaseShaderSource.PHASE_RECONSTRUCT_FRAGMENT

        assertTrue(shader.contains("amplitude * cos(phase)"))
        assertTrue(shader.contains("uAmplifiedPhaseTexture"))
    }
}
