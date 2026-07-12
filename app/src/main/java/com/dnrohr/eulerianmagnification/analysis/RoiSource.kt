package com.dnrohr.eulerianmagnification.analysis

enum class RoiSource(
    val label: String,
    val compactLabel: String,
) {
    Auto("Auto ROI", "Auto ROI"),
    FullFrame("Full frame", "Full frame"),
    Manual("Manual ROI", "Manual ROI"),
}

object RoiSourcePolicy {
    val FULL_FRAME_ROI = NormalizedRect(0.0f, 0.0f, 1.0f, 1.0f)

    fun defaultFor(mode: MagnificationMode): RoiSource {
        return when (mode) {
            MagnificationMode.Breathing,
            MagnificationMode.Pulse,
            MagnificationMode.Tremor,
            MagnificationMode.ObjectVibration -> RoiSource.Auto
        }
    }

    fun activeRoi(
        source: RoiSource,
        autoRoi: NormalizedRect?,
        manualRoi: NormalizedRect?,
    ): NormalizedRect? {
        return when (source) {
            RoiSource.Auto -> autoRoi
            RoiSource.FullFrame -> FULL_FRAME_ROI
            RoiSource.Manual -> manualRoi
        }
    }

    fun labelFor(source: RoiSource, roiState: RoiState): String {
        return when (source) {
            RoiSource.Auto -> roiState.label
            RoiSource.FullFrame -> RoiSource.FullFrame.compactLabel
            RoiSource.Manual -> RoiSource.Manual.compactLabel
        }
    }
}
