package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TenSecondValidationFlowTest {
    @Test
    fun setupUsesCurrentModeGuideAndArtifactContract() {
        val state = TenSecondValidationFlow.setup(
            AnalysisSettings(mode = MagnificationMode.Breathing, viewMode = ViewMode.Split),
            nowMillis = 100L,
        )

        assertEquals(TenSecondValidationPhase.Setup, state.phase)
        assertEquals("Breathing setup", state.setupGuide.title)
        assertEquals(10_000L, state.targetDurationMillis)
        assertEquals(3_000L, state.countdownMillis)
        assertEquals(
            listOf(
                ValidationArtifact.ProcessedVideo,
                ValidationArtifact.Metadata,
                ValidationArtifact.SignalTimeline,
                ValidationArtifact.EvidenceReport,
            ),
            TenSecondValidationFlow.requiredArtifacts,
        )
    }

    @Test
    fun tickAdvancesCountdownThenRecordingThenProcessing() {
        val setup = TenSecondValidationFlow.setup(AnalysisSettings(), nowMillis = 0L)
        val countdown = TenSecondValidationFlow.startCountdown(setup, nowMillis = 1_000L)

        assertEquals(TenSecondValidationPhase.Countdown, countdown.phase)
        assertEquals(1_500L, countdown.remainingCountdownMillis(nowMillis = 2_500L))

        val recording = TenSecondValidationFlow.tick(countdown, nowMillis = 4_000L)

        assertEquals(TenSecondValidationPhase.Recording, recording.phase)
        assertEquals(10_000L, recording.remainingRecordingMillis(nowMillis = 4_000L))
        assertEquals(0.5f, recording.progress(nowMillis = 9_000L), 0.0001f)

        val processing = TenSecondValidationFlow.tick(recording, nowMillis = 14_000L)

        assertEquals(TenSecondValidationPhase.Processing, processing.phase)
    }

    @Test
    fun reviewReportsWhetherAllArtifactsExist() {
        val processing = TenSecondValidationFlow.tick(
            TenSecondValidationFlow.tick(
                TenSecondValidationFlow.startCountdown(TenSecondValidationFlow.setup(AnalysisSettings()), nowMillis = 0L),
                nowMillis = 3_000L,
            ),
            nowMillis = 13_000L,
        )

        val partial = TenSecondValidationFlow.review(
            state = processing,
            summary = "signal present",
            artifactPaths = mapOf(ValidationArtifact.Metadata to "metadata.json"),
            nowMillis = 14_000L,
        )

        assertEquals(TenSecondValidationPhase.Review, partial.phase)
        assertFalse(partial.review!!.complete)

        val complete = TenSecondValidationFlow.review(
            state = processing,
            summary = "signal present",
            artifactPaths = TenSecondValidationFlow.requiredArtifacts.associateWith { it.fileName },
            nowMillis = 14_000L,
        )

        assertTrue(complete.review!!.complete)
    }

    @Test
    fun metadataFieldsCaptureValidationPlan() {
        val state = TenSecondValidationFlow.setup(
            AnalysisSettings(
                mode = MagnificationMode.Pulse,
                amplification = 8.0f,
                viewMode = ViewMode.Difference,
            ),
        )

        val metadata = TenSecondValidationFlow.metadataFields(state)

        assertEquals("ten-second", metadata["validationFlow"])
        assertEquals("10000", metadata["targetDurationMillis"])
        assertEquals("Pulse", metadata["mode"])
        assertEquals("Difference", metadata["viewMode"])
        assertEquals("0.7", metadata["lowCutHz"])
        assertEquals("3.0", metadata["highCutHz"])
        assertEquals("8.0", metadata["amplification"])
        assertTrue(metadata["requiredArtifacts"]!!.contains("signal_timeline.csv"))
    }
}
