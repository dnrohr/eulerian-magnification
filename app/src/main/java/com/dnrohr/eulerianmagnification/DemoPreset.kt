package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.ViewMode

enum class DemoPreset(
    val label: String,
    val settings: AnalysisSettings,
) {
    Pulse(
        label = "Pulse demo",
        settings = ParityPreset.PulseColor.settings,
    ),
    Breathing(
        label = "Breathing demo",
        settings = ParityPreset.BreathingSlowMotion.settings,
    ),
    ObjectVibration(
        label = "Object demo",
        settings = ParityPreset.ObjectVibration.settings,
    ),
    FastMotion(
        label = "Motion demo",
        settings = ParityPreset.FastTremor.settings,
    );
}
