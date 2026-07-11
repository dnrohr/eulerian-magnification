package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.abs

data class RateSignalSample(
    val timestampNanos: Long,
    val value: Double,
)

enum class RateEstimateKind(
    val label: String,
    val unitLabel: String,
) {
    Pulse("experimental pulse estimate", "bpm"),
    Breathing("experimental breathing estimate", "br/min"),
}

data class RateEstimate(
    val kind: RateEstimateKind,
    val perMinute: Double,
    val diagnostic: Boolean = false,
    val experimental: Boolean = true,
) {
    fun summary(): String {
        return "${kind.label}: ${perMinute.oneDecimal()} ${kind.unitLabel} (experimental, non-diagnostic)"
    }
}

enum class RateEstimateHiddenReason(val message: String) {
    UnsupportedMode("rate hidden: unsupported mode"),
    TooFewFrames("rate hidden: not enough frames"),
    LowFps("rate hidden: low FPS"),
    TimingUnstable("rate hidden: unstable timing"),
    MissingRoi("rate hidden: missing ROI"),
    LightingUnstable("rate hidden: lighting is not stable"),
    MotionUnstable("rate hidden: ROI/camera motion is too high"),
    WeakSignal("rate hidden: signal is too weak"),
    NoPeriodicSignal("rate hidden: no stable periodic signal"),
}

data class RateEstimateGate(
    val frameCount: Int,
    val averageFps: Double,
    val timestampsMonotonic: Boolean,
    val hasRoi: Boolean,
    val lightingStable: Boolean,
    val motionMagnitude: Float,
    val bandpassedEnergy: Double,
    val maxBandpassedMagnitude: Double,
)

data class GatedRateEstimate(
    val estimate: RateEstimate?,
    val hiddenReason: RateEstimateHiddenReason?,
) {
    val visible: Boolean get() = estimate != null
}

object GatedRateEstimator {
    fun estimate(
        mode: MagnificationMode,
        samples: List<RateSignalSample>,
        gate: RateEstimateGate,
    ): GatedRateEstimate {
        val gateFailure = gateFailure(mode, gate)
        if (gateFailure != null) {
            return GatedRateEstimate(estimate = null, hiddenReason = gateFailure)
        }
        val estimatedHz = estimateFrequencyHz(samples)
            ?: return GatedRateEstimate(estimate = null, hiddenReason = RateEstimateHiddenReason.NoPeriodicSignal)
        if (estimatedHz !in mode.lowCutHz..mode.highCutHz) {
            return GatedRateEstimate(estimate = null, hiddenReason = RateEstimateHiddenReason.NoPeriodicSignal)
        }
        return GatedRateEstimate(
            estimate = RateEstimate(
                kind = if (mode == MagnificationMode.Breathing) RateEstimateKind.Breathing else RateEstimateKind.Pulse,
                perMinute = estimatedHz * SECONDS_PER_MINUTE,
            ),
            hiddenReason = null,
        )
    }

    private fun gateFailure(
        mode: MagnificationMode,
        gate: RateEstimateGate,
    ): RateEstimateHiddenReason? {
        if (mode != MagnificationMode.Pulse && mode != MagnificationMode.Breathing) {
            return RateEstimateHiddenReason.UnsupportedMode
        }
        if (gate.frameCount < MIN_FRAMES) return RateEstimateHiddenReason.TooFewFrames
        if (gate.averageFps < MIN_FPS) return RateEstimateHiddenReason.LowFps
        if (!gate.timestampsMonotonic) return RateEstimateHiddenReason.TimingUnstable
        if (!gate.hasRoi) return RateEstimateHiddenReason.MissingRoi
        if (!gate.lightingStable) return RateEstimateHiddenReason.LightingUnstable
        if (gate.motionMagnitude > MAX_MOTION_MAGNITUDE) return RateEstimateHiddenReason.MotionUnstable
        if (gate.bandpassedEnergy < MIN_BANDPASSED_ENERGY || gate.maxBandpassedMagnitude < MIN_PEAK_MAGNITUDE) {
            return RateEstimateHiddenReason.WeakSignal
        }
        return null
    }

    private fun estimateFrequencyHz(samples: List<RateSignalSample>): Double? {
        val crossings = samples
            .zipWithNext()
            .mapNotNull { (previous, current) ->
                if (previous.value < 0.0 && current.value >= 0.0) {
                    interpolateCrossingNanos(previous, current)
                } else {
                    null
                }
            }
        if (crossings.size < MIN_CROSSINGS) return null
        val periodsSeconds = crossings.zipWithNext { previous, current ->
            (current - previous) / NANOS_PER_SECOND
        }.filter { it > 0.0 }
        if (periodsSeconds.size < MIN_PERIODS) return null
        val averagePeriod = periodsSeconds.average()
        if (averagePeriod <= 0.0) return null
        return 1.0 / averagePeriod
    }

    private fun interpolateCrossingNanos(
        previous: RateSignalSample,
        current: RateSignalSample,
    ): Double {
        val span = current.timestampNanos - previous.timestampNanos
        if (span <= 0L) return current.timestampNanos.toDouble()
        val denominator = abs(previous.value) + abs(current.value)
        if (denominator <= 0.0) return current.timestampNanos.toDouble()
        return previous.timestampNanos + span * (abs(previous.value) / denominator)
    }

    private const val MIN_FRAMES = 150
    private const val MIN_FPS = 24.0
    private const val MIN_BANDPASSED_ENERGY = 8.0
    private const val MIN_PEAK_MAGNITUDE = 0.08
    private const val MAX_MOTION_MAGNITUDE = 0.008f
    private const val MIN_CROSSINGS = 3
    private const val MIN_PERIODS = 2
    private const val SECONDS_PER_MINUTE = 60.0
    private const val NANOS_PER_SECOND = 1_000_000_000.0
}

private fun Double.oneDecimal(): String {
    val rounded = kotlin.math.round(this * 10.0).toInt()
    return "${rounded / 10}.${kotlin.math.abs(rounded % 10)}"
}
