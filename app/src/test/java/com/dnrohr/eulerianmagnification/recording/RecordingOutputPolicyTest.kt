package com.dnrohr.eulerianmagnification.recording

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingOutputPolicyTest {
    @Test
    fun cleanModeUsesCleanPreviewWhenGlPreviewIsActive() {
        val kind = RecordingOutputPolicy.outputKind(
            requestedMode = RecordingOutputMode.Clean,
            usingGlPreview = true,
        )

        assertEquals(RecordingOutputKind.CleanPreview, kind)
    }

    @Test
    fun cleanModeFallsBackToAnnotatedEvidenceWithoutGlPreview() {
        val kind = RecordingOutputPolicy.outputKind(
            requestedMode = RecordingOutputMode.Clean,
            usingGlPreview = false,
        )

        assertEquals(RecordingOutputKind.AnnotatedEvidence, kind)
    }

    @Test
    fun annotatedModeAlwaysUsesAnnotatedEvidence() {
        assertEquals(
            RecordingOutputKind.AnnotatedEvidence,
            RecordingOutputPolicy.outputKind(RecordingOutputMode.Annotated, usingGlPreview = true),
        )
        assertEquals(
            RecordingOutputKind.AnnotatedEvidence,
            RecordingOutputPolicy.outputKind(RecordingOutputMode.Annotated, usingGlPreview = false),
        )
    }
}
