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
        return when (mode) {
            MagnificationMode.Pulse -> ModeSetupGuide(
                title = "Pulse setup",
                target = "Target forehead or cheek skin in steady light.",
                stabilize = COMMON_STEPS,
                expected = "Best result is a subtle color pulse in Amp or Diff, not visible face motion.",
            )
            MagnificationMode.Breathing -> ModeSetupGuide(
                title = "Breathing setup",
                target = "Target torso, shoulder, or a clear clothing edge.",
                stabilize = COMMON_STEPS,
                expected = "Best result is a low-frequency breathing signal; the preview is not motion-warped yet.",
            )
            MagnificationMode.Tremor,
            MagnificationMode.ObjectVibration,
            -> ModeSetupGuide(
                title = "Fast motion setup",
                target = "Target a high-contrast edge on a small moving or vibrating object.",
                stabilize = COMMON_STEPS,
                expected = "Best result is signal in Diff/Split; camera shake can overwhelm this band.",
            )
        }
    }

    private val COMMON_STEPS = listOf(
        "Mount the phone or brace it firmly.",
        "Let exposure settle for a few seconds.",
        "Lock AE/AWB.",
        "Drag one manual ROI over the target.",
        "Use Clean preview for validation.",
    )
}
