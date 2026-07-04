package com.dnrohr.eulerianmagnification.analysis

import kotlin.math.abs

data class RecordedVideoAnalysisReport(
    val frameCount: Int,
    val averageFps: Double,
    val averageGreen: Double,
    val bandpassedEnergy: Double,
    val maxBandpassedMagnitude: Double,
    val timestampsMonotonic: Boolean,
) {
    val hasFrames: Boolean get() = frameCount > 0
}

class RecordedVideoAnalysisRunner(
    private val settings: AnalysisSettings,
    private val roi: NormalizedRect = RecordedVideoAnalyzer.DEFAULT_ROI,
) {
    fun analyze(frames: Iterable<RgbFrame>): RecordedVideoAnalysisReport {
        val analyzer = RecordedVideoAnalyzer(settings, roi)
        var frameCount = 0
        var fpsSum = 0.0
        var fpsCount = 0
        var greenSum = 0.0
        var bandpassedEnergy = 0.0
        var maxBandpassedMagnitude = 0.0
        var timestampsMonotonic = true

        frames.forEach { frame ->
            val sample = analyzer.analyze(frame)
            val bandpassedMagnitude = abs(sample.bandpassedGreen)
            frameCount++
            if (sample.analysisFps > 0.0) {
                fpsSum += sample.analysisFps
                fpsCount++
            }
            greenSum += sample.averageGreen
            bandpassedEnergy += bandpassedMagnitude
            maxBandpassedMagnitude = maxOf(maxBandpassedMagnitude, bandpassedMagnitude)
            timestampsMonotonic = timestampsMonotonic && sample.timestampMonotonic
        }

        return RecordedVideoAnalysisReport(
            frameCount = frameCount,
            averageFps = if (fpsCount == 0) 0.0 else fpsSum / fpsCount,
            averageGreen = if (frameCount == 0) 0.0 else greenSum / frameCount,
            bandpassedEnergy = bandpassedEnergy,
            maxBandpassedMagnitude = maxBandpassedMagnitude,
            timestampsMonotonic = timestampsMonotonic,
        )
    }
}
