package com.dnrohr.eulerianmagnification.gl

import com.dnrohr.eulerianmagnification.analysis.MagnificationMode

data class LiveReconstructionContractReport(
    val activeLevels: Int,
    val temporalStateTargets: Int,
    val pulseStartLevel: Int,
    val breathingStartLevel: Int,
    val usesPerLevelBandpassTextures: Boolean,
    val avoidsRoiScalarSignal: Boolean,
    val usesLaplacianDelta: Boolean,
) {
    val passed: Boolean
        get() = activeLevels >= REQUIRED_LEVELS &&
            temporalStateTargets >= activeLevels * GlTemporalStatePlan.TARGETS_PER_LEVEL &&
            pulseStartLevel == 0 &&
            breathingStartLevel >= 1 &&
            usesPerLevelBandpassTextures &&
            avoidsRoiScalarSignal &&
            usesLaplacianDelta

    fun summary(): String {
        val status = if (passed) "passed" else "failed"
        return "Live reconstruction contract: $status, " +
            "$activeLevels levels, $temporalStateTargets temporal targets, " +
            "Pulse starts L$pulseStartLevel, Breathing starts L$breathingStartLevel"
    }

    companion object {
        private const val REQUIRED_LEVELS = 3
    }
}

object LiveReconstructionContract {
    fun evaluate(surfaceSize: GlTextureSize): LiveReconstructionContractReport {
        val plan = LivePyramidReconstructionPlan(surfaceSize)
        val reconstructShader = LivePyramidShaderSource.RECONSTRUCT_FRAGMENT
        return LiveReconstructionContractReport(
            activeLevels = plan.pyramidSizes.size,
            temporalStateTargets = plan.temporalRenderTargetCount,
            pulseStartLevel = LivePyramidReconstructionProfile.forMode(MagnificationMode.Pulse).startLevel,
            breathingStartLevel = LivePyramidReconstructionProfile.forMode(MagnificationMode.Breathing).startLevel,
            usesPerLevelBandpassTextures = listOf(
                "uBandpassTexture0",
                "uBandpassTexture1",
                "uBandpassTexture2",
            ).all(reconstructShader::contains),
            avoidsRoiScalarSignal = !reconstructShader.contains("uAmplifiedSignal"),
            usesLaplacianDelta = reconstructShader.contains("laplacianDelta(level)"),
        )
    }
}
