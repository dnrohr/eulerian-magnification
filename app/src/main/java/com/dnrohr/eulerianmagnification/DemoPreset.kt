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
        settings = AnalysisSettings(
            mode = MagnificationMode.Pulse,
            amplification = 12.0f,
            viewMode = ViewMode.Amplified,
        ),
    ),
    Breathing(
        label = "Breathing demo",
        settings = AnalysisSettings(
            mode = MagnificationMode.Breathing,
            amplification = 16.0f,
            viewMode = ViewMode.Difference,
        ),
    ),
    FastMotion(
        label = "Motion demo",
        settings = AnalysisSettings(
            mode = MagnificationMode.Tremor,
            amplification = 20.0f,
            viewMode = ViewMode.Split,
        ),
    );
}
