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
    val outputLabel: String,
    val compactOutputLabel: String,
) {
    Pulse("Pulse", 0.7, 3.0, "Color amplification", "Color amp"),
    Breathing("Breathing", 0.1, 0.6, "Breathing signal", "Breath sig"),
    Tremor("Fast Motion", 4.0, 12.0, "Experimental fast-motion analysis", "Motion exp"),
    ObjectVibration("Object", 3.0, 12.0, "Experimental object vibration", "Object exp"),
}

enum class ViewMode(val label: String) {
    Raw("Raw"),
    Amplified("Amplified"),
    Difference("Difference"),
    Split("Split"),
}
