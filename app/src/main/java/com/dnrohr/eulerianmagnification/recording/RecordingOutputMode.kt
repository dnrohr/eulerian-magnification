package com.dnrohr.eulerianmagnification.recording

enum class RecordingOutputMode(
    val label: String,
    val description: String,
) {
    Clean(
        label = "Clean",
        description = "Records the processed preview texture without app controls when GL preview is active.",
    ),
    Annotated(
        label = "Annotated",
        description = "Records evidence frames with labels, ROI, signal, mode, FPS, and latency.",
    ),
}

enum class RecordingOutputKind(val metadataValue: String) {
    CleanPreview("clean_preview"),
    AnnotatedEvidence("annotated_evidence"),
}

object RecordingOutputPolicy {
    fun outputKind(
        requestedMode: RecordingOutputMode,
        usingGlPreview: Boolean,
    ): RecordingOutputKind {
        return if (requestedMode == RecordingOutputMode.Clean && usingGlPreview) {
            RecordingOutputKind.CleanPreview
        } else {
            RecordingOutputKind.AnnotatedEvidence
        }
    }
}
