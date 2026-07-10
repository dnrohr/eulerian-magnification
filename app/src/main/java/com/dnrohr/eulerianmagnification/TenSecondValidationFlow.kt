package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import java.util.Locale
import kotlin.math.ceil

enum class TenSecondValidationPhase {
    Setup,
    Countdown,
    Recording,
    Processing,
    Review,
}

enum class ValidationArtifact(
    val fileName: String,
    val label: String,
) {
    ProcessedVideo("debug_processed.mp4", "processed MP4"),
    Metadata("metadata.json", "metadata JSON"),
    SignalTimeline("signal_timeline.csv", "signal CSV"),
    EvidenceReport("evidence_report.html", "evidence report"),
}

data class TenSecondValidationReview(
    val summary: String,
    val artifactPaths: Map<ValidationArtifact, String>,
) {
    val complete: Boolean
        get() = TenSecondValidationFlow.requiredArtifacts.all { artifactPaths.containsKey(it) }
}

data class TenSecondValidationState(
    val phase: TenSecondValidationPhase,
    val settings: AnalysisSettings,
    val setupGuide: ModeSetupGuide,
    val targetDurationMillis: Long,
    val countdownMillis: Long,
    val phaseStartedAtMillis: Long,
    val review: TenSecondValidationReview? = null,
) {
    val title: String
        get() = when (phase) {
            TenSecondValidationPhase.Setup -> setupGuide.title
            TenSecondValidationPhase.Countdown -> "Validation starts in ${remainingCountdownSeconds()}s"
            TenSecondValidationPhase.Recording -> "Recording validation"
            TenSecondValidationPhase.Processing -> "Processing evidence"
            TenSecondValidationPhase.Review -> "Validation review"
        }

    fun remainingCountdownMillis(nowMillis: Long): Long {
        if (phase != TenSecondValidationPhase.Countdown) return 0L
        return (countdownMillis - (nowMillis - phaseStartedAtMillis)).coerceAtLeast(0L)
    }

    fun remainingRecordingMillis(nowMillis: Long): Long {
        if (phase != TenSecondValidationPhase.Recording) return 0L
        return (targetDurationMillis - (nowMillis - phaseStartedAtMillis)).coerceAtLeast(0L)
    }

    fun progress(nowMillis: Long): Float {
        return when (phase) {
            TenSecondValidationPhase.Countdown -> elapsedFraction(nowMillis, countdownMillis)
            TenSecondValidationPhase.Recording -> elapsedFraction(nowMillis, targetDurationMillis)
            TenSecondValidationPhase.Setup,
            TenSecondValidationPhase.Processing,
            TenSecondValidationPhase.Review,
            -> 0.0f
        }
    }

    private fun remainingCountdownSeconds(): Long {
        return ceil(countdownMillis / 1000.0).toLong().coerceAtLeast(1L)
    }

    private fun elapsedFraction(nowMillis: Long, durationMillis: Long): Float {
        if (durationMillis <= 0L) return 1.0f
        return ((nowMillis - phaseStartedAtMillis).toFloat() / durationMillis.toFloat()).coerceIn(0.0f, 1.0f)
    }
}

object TenSecondValidationFlow {
    const val TARGET_DURATION_MILLIS: Long = 10_000L
    const val COUNTDOWN_MILLIS: Long = 3_000L

    val requiredArtifacts: List<ValidationArtifact> = listOf(
        ValidationArtifact.ProcessedVideo,
        ValidationArtifact.Metadata,
        ValidationArtifact.SignalTimeline,
        ValidationArtifact.EvidenceReport,
    )

    fun setup(
        settings: AnalysisSettings,
        nowMillis: Long = 0L,
    ): TenSecondValidationState {
        return TenSecondValidationState(
            phase = TenSecondValidationPhase.Setup,
            settings = settings,
            setupGuide = SetupGuide.forMode(settings.mode),
            targetDurationMillis = TARGET_DURATION_MILLIS,
            countdownMillis = COUNTDOWN_MILLIS,
            phaseStartedAtMillis = nowMillis,
        )
    }

    fun startCountdown(
        state: TenSecondValidationState,
        nowMillis: Long,
    ): TenSecondValidationState {
        return state.copy(
            phase = TenSecondValidationPhase.Countdown,
            setupGuide = SetupGuide.forMode(state.settings.mode),
            phaseStartedAtMillis = nowMillis,
            review = null,
        )
    }

    fun tick(
        state: TenSecondValidationState,
        nowMillis: Long,
    ): TenSecondValidationState {
        return when (state.phase) {
            TenSecondValidationPhase.Countdown -> {
                if (nowMillis - state.phaseStartedAtMillis >= state.countdownMillis) {
                    state.copy(
                        phase = TenSecondValidationPhase.Recording,
                        phaseStartedAtMillis = state.phaseStartedAtMillis + state.countdownMillis,
                    )
                } else {
                    state
                }
            }
            TenSecondValidationPhase.Recording -> {
                if (nowMillis - state.phaseStartedAtMillis >= state.targetDurationMillis) {
                    state.copy(
                        phase = TenSecondValidationPhase.Processing,
                        phaseStartedAtMillis = state.phaseStartedAtMillis + state.targetDurationMillis,
                    )
                } else {
                    state
                }
            }
            TenSecondValidationPhase.Setup,
            TenSecondValidationPhase.Processing,
            TenSecondValidationPhase.Review,
            -> state
        }
    }

    fun review(
        state: TenSecondValidationState,
        summary: String,
        artifactPaths: Map<ValidationArtifact, String>,
        nowMillis: Long,
    ): TenSecondValidationState {
        return state.copy(
            phase = TenSecondValidationPhase.Review,
            phaseStartedAtMillis = nowMillis,
            review = TenSecondValidationReview(
                summary = summary,
                artifactPaths = artifactPaths,
            ),
        )
    }

    fun metadataFields(state: TenSecondValidationState): Map<String, String> {
        return linkedMapOf(
            "validationFlow" to "ten-second",
            "targetDurationMillis" to state.targetDurationMillis.toString(),
            "countdownMillis" to state.countdownMillis.toString(),
            "mode" to state.settings.mode.label,
            "viewMode" to state.settings.viewMode.label,
            "lowCutHz" to "%.1f".format(Locale.US, state.settings.lowCutHz),
            "highCutHz" to "%.1f".format(Locale.US, state.settings.highCutHz),
            "amplification" to "%.1f".format(Locale.US, state.settings.amplification),
            "requiredArtifacts" to requiredArtifacts.joinToString(",") { it.fileName },
        )
    }
}
