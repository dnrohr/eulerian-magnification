package com.dnrohr.eulerianmagnification.quality

data class QualityCueState(
    val lastCueAtMillis: Long? = null,
    val lastCueStatus: QualityStatus? = null,
)

data class QualityCueDecision(
    val shouldCue: Boolean,
    val nextState: QualityCueState,
    val reason: QualityCueReason? = null,
)

enum class QualityCueReason {
    QualityRecovered,
    QualityRegressed,
    PrimaryIssueChanged,
}

object QualityCuePolicy {
    const val MIN_INTERVAL_MILLIS: Long = 10_000L

    fun decide(
        previousStatuses: List<QualityStatus>,
        currentStatuses: List<QualityStatus>,
        state: QualityCueState,
        nowMillis: Long,
        enabled: Boolean,
        systemHapticsAllowed: Boolean,
    ): QualityCueDecision {
        val previous = previousStatuses.primaryCueStatus()
        val current = currentStatuses.primaryCueStatus()
        val reason = cueReason(previous, current)
        val rateLimited = state.lastCueAtMillis
            ?.let { nowMillis - it < MIN_INTERVAL_MILLIS }
            ?: false
        val shouldCue = enabled &&
            systemHapticsAllowed &&
            reason != null &&
            !rateLimited &&
            current != state.lastCueStatus

        return QualityCueDecision(
            shouldCue = shouldCue,
            nextState = if (shouldCue) {
                QualityCueState(
                    lastCueAtMillis = nowMillis,
                    lastCueStatus = current,
                )
            } else {
                state
            },
            reason = if (shouldCue) reason else null,
        )
    }

    private fun cueReason(
        previous: QualityStatus,
        current: QualityStatus,
    ): QualityCueReason? {
        if (previous == current) return null
        if (previous == QualityStatus.Good && current != QualityStatus.Good) {
            return QualityCueReason.QualityRegressed
        }
        if (previous != QualityStatus.Good && current == QualityStatus.Good) {
            return QualityCueReason.QualityRecovered
        }
        return if (previous.severity != current.severity || previous != current) {
            QualityCueReason.PrimaryIssueChanged
        } else {
            null
        }
    }

    private fun List<QualityStatus>.primaryCueStatus(): QualityStatus {
        return filter { it.cueEligible }
            .maxByOrNull { it.severity }
            ?: QualityStatus.Good
    }

    private val QualityStatus.cueEligible: Boolean
        get() = when (this) {
            QualityStatus.Good,
            QualityStatus.FaceMissing,
            QualityStatus.TooDark,
            QualityStatus.ThermalHigh,
            QualityStatus.LowFps,
            QualityStatus.FullFrameSlow,
            QualityStatus.CameraFpsLow,
            QualityStatus.TimingUnstable,
            QualityStatus.LightingFlicker,
            QualityStatus.LightingUnstable,
            QualityStatus.CameraMotion,
            -> true
            QualityStatus.ModeMotionRisk,
            QualityStatus.AmplificationRisk,
            QualityStatus.SignalWeak,
            -> false
        }

    private val QualityStatus.severity: Int
        get() = when (this) {
            QualityStatus.Good -> 0
            QualityStatus.SignalWeak -> 1
            QualityStatus.ModeMotionRisk,
            QualityStatus.AmplificationRisk,
            -> 2
            QualityStatus.LowFps,
            QualityStatus.FullFrameSlow,
            QualityStatus.CameraFpsLow,
            QualityStatus.LightingUnstable,
            -> 3
            QualityStatus.CameraMotion,
            QualityStatus.LightingFlicker,
            QualityStatus.ThermalHigh,
            -> 4
            QualityStatus.FaceMissing,
            QualityStatus.TooDark,
            QualityStatus.TimingUnstable,
            -> 5
        }
}
