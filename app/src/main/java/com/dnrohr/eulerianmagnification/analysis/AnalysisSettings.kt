package com.dnrohr.eulerianmagnification.analysis

data class AnalysisSettings(
    val mode: MagnificationMode = MagnificationMode.Pulse,
    val amplification: Float = 12.0f,
    val viewMode: ViewMode = ViewMode.Amplified,
) {
    val lowCutHz: Double get() = mode.lowCutHz
    val highCutHz: Double get() = mode.highCutHz
}

enum class MagnificationMode(
    val label: String,
    val lowCutHz: Double,
    val highCutHz: Double,
) {
    Pulse("Pulse", 0.7, 3.0),
    Breathing("Breathing", 0.1, 0.6),
}

enum class ViewMode(val label: String) {
    Raw("Raw"),
    Amplified("Amplified"),
    Difference("Difference"),
}
