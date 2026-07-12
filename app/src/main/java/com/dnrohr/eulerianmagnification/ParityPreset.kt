package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.ViewMode

enum class ParityPreset(
    val label: String,
    val settings: AnalysisSettings,
    val target: String,
    val lighting: String,
    val support: String,
    val distance: String,
    val expected: String,
) {
    PulseColor(
        label = "Pulse color",
        settings = AnalysisSettings(
            mode = MagnificationMode.Pulse,
            amplification = 12.0f,
            viewMode = ViewMode.Amplified,
        ),
        target = "forehead or cheek skin",
        lighting = "bright, steady, diffuse light",
        support = "phone mounted; subject still and quiet",
        distance = "face fills much of the frame",
        expected = "subtle rhythmic skin-color change, not visible face motion",
    ),
    BreathingSlowMotion(
        label = "Breathing",
        settings = AnalysisSettings(
            mode = MagnificationMode.Breathing,
            amplification = 16.0f,
            viewMode = ViewMode.Difference,
        ),
        target = "torso, shoulder, or a clear clothing edge",
        lighting = "steady room light without flicker",
        support = "phone mounted several feet away",
        distance = "upper torso visible with background context",
        expected = "low-frequency breathing signal; live preview is not motion-warped yet",
    ),
    ObjectVibration(
        label = "Object vib",
        settings = AnalysisSettings(
            mode = MagnificationMode.ObjectVibration,
            amplification = 18.0f,
            viewMode = ViewMode.Split,
        ),
        target = "high-contrast edge on a vibrating object",
        lighting = "bright enough for short exposure and low blur",
        support = "phone and object support isolated from each other",
        distance = "object edge large enough to inspect in Split",
        expected = "processed edge differs from raw when motion is in band",
    ),
    FastTremor(
        label = "Fast tremor",
        settings = AnalysisSettings(
            mode = MagnificationMode.Tremor,
            amplification = 20.0f,
            viewMode = ViewMode.Split,
        ),
        target = "small high-contrast biological or mechanical motion",
        lighting = "bright enough for short exposure and low blur",
        support = "tripod required; avoid hand-held validation",
        distance = "target region large enough for stable tracking",
        expected = "Split/Diff signal only when camera motion is lower than target motion",
    );

    val bandLabel: String
        get() = "${settings.lowCutHz.formatHz()}-${settings.highCutHz.formatHz()} Hz"

    companion object {
        fun forMode(mode: MagnificationMode): ParityPreset {
            return when (mode) {
                MagnificationMode.Pulse -> PulseColor
                MagnificationMode.Breathing -> BreathingSlowMotion
                MagnificationMode.ObjectVibration -> ObjectVibration
                MagnificationMode.Tremor -> FastTremor
            }
        }
    }
}

object ParityPresetWarnings {
    fun forPreset(preset: ParityPreset, measuredFps: Double): List<String> {
        return buildList {
            if (measuredFps > 0.0 && measuredFps < preset.settings.highCutHz * FPS_HEADROOM_FACTOR) {
                add("Measured FPS is close to the ${preset.bandLabel} band; lower the band or improve lighting.")
            }
            if (preset == ParityPreset.ObjectVibration || preset == ParityPreset.FastTremor) {
                add("Object vibration and fast tremor bands intentionally overlap; target setup and renderer evidence matter more than the label.")
                add("Phone motion, rolling exposure, and heartbeat-scale movement can contaminate this band.")
            }
            if (preset == ParityPreset.PulseColor) {
                add("Pulse color is vulnerable to exposure pumping, skin motion, and lighting flicker.")
            }
        }
    }

    private const val FPS_HEADROOM_FACTOR = 2.5
}

private fun Double.formatHz(): String {
    return if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        toString()
    }
}
