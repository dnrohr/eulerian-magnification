package com.dnrohr.eulerianmagnification.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LivePhaseDiagnosticsTest {
    @Test
    fun notRequestedSummarizesAsInactive() {
        val diagnostics = LivePhaseDiagnostics(requested = false)

        assertFalse(diagnostics.active)
        assertEquals("phase: not requested", diagnostics.summary)
    }

    @Test
    fun fallbackReasonOverridesWarmupDetails() {
        val diagnostics = LivePhaseDiagnostics(
            requested = true,
            warmupStatus = LivePhaseWarmupStatus.Ready,
            processingSize = GlTextureSize(160, 120),
            fallbackReason = LivePhaseFallbackReason.MissingManualRoi,
        )

        assertFalse(diagnostics.active)
        assertEquals("phase fallback: manual ROI required", diagnostics.summary)
    }

    @Test
    fun readyWithoutFallbackIsActive() {
        val diagnostics = LivePhaseDiagnostics(
            requested = true,
            warmupStatus = LivePhaseWarmupStatus.Ready,
            processingSize = GlTextureSize(160, 120),
            amplitudeGate = LivePhaseAmplitudeGate(
                status = LivePhaseAmplitudeGateStatus.Pass,
                meanAmplitude = 0.08f,
                threshold = 0.03f,
            ),
        )

        assertTrue(diagnostics.active)
        assertEquals("phase: 160x120 / phase ready / amplitude ok 0.080/0.030", diagnostics.summary)
    }

    @Test
    fun warmingDiagnosticsRemainInactiveButVisible() {
        val diagnostics = LivePhaseDiagnostics(
            requested = true,
            warmupStatus = LivePhaseWarmupStatus.Warming,
            processingSize = GlTextureSize(320, 180),
            amplitudeGate = LivePhaseAmplitudeGate.Unknown,
        )

        assertFalse(diagnostics.active)
        assertEquals(
            "phase: 320x180 / phase warmup: filling temporal state / amplitude threshold unknown",
            diagnostics.summary,
        )
    }

    @Test
    fun unknownAmplitudeCanReportKnownThreshold() {
        val gate = LivePhaseAmplitudeGate(
            status = LivePhaseAmplitudeGateStatus.Unknown,
            threshold = 0.03f,
        )

        assertEquals("amplitude threshold 0.030", gate.label)
    }

    @Test
    fun lowAmplitudeGateFormatsThreshold() {
        val gate = LivePhaseAmplitudeGate(
            status = LivePhaseAmplitudeGateStatus.Low,
            meanAmplitude = 0.01f,
            threshold = 0.03f,
        )

        assertEquals("amplitude low 0.010/0.030", gate.label)
    }
}
