package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.MagnificationMode

data class ModeSetupGuide(
    val title: String,
    val target: String,
    val stabilize: List<String>,
    val expected: String,
)

object SetupGuide {
    fun forMode(mode: MagnificationMode): ModeSetupGuide {
        val preset = ParityPreset.forMode(mode)
        return ModeSetupGuide(
            title = "${preset.label} setup",
            target = "Target ${preset.target}.",
            stabilize = COMMON_STEPS,
            expected = "Best result is ${preset.expected}.",
        )
    }

    private val COMMON_STEPS = listOf(
        "Mount the phone or brace it firmly.",
        "Let exposure settle for a few seconds.",
        "Lock AE/AWB.",
        "Use the default ROI source first; switch to Manual ROI only for a deliberate box.",
        "Use Clean preview for validation.",
    )
}
