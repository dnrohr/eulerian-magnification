package com.dnrohr.eulerianmagnification.gl

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveReconstructionContractTest {
    @Test
    fun liveContractPassesForCurrentControlledTargetRequirements() {
        val report = LiveReconstructionContract.evaluate(GlTextureSize(1280, 720))

        assertTrue(report.summary(), report.passed)
        assertTrue(report.usesPerLevelBandpassTextures)
        assertTrue(report.avoidsRoiScalarSignal)
        assertTrue(report.usesLaplacianDelta)
        assertTrue(report.summary().contains("Pulse starts L0"))
        assertTrue(report.summary().contains("Breathing starts L1"))
    }

    @Test
    fun liveContractFailsWhenSpatialOrTemporalStructureIsMissing() {
        val report = LiveReconstructionContractReport(
            activeLevels = 1,
            temporalStateTargets = GlTemporalStatePlan.TARGETS_PER_LEVEL,
            pulseStartLevel = 0,
            breathingStartLevel = 1,
            usesPerLevelBandpassTextures = false,
            avoidsRoiScalarSignal = false,
            usesLaplacianDelta = false,
        )

        assertFalse(report.passed)
        assertTrue(report.summary().contains("failed"))
    }
}
