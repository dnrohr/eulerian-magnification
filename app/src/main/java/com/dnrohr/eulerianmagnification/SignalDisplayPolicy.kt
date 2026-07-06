package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.MagnificationMode

object SignalDisplayPolicy {
    fun compactSignalHistory(
        mode: MagnificationMode,
        pulseHistory: List<Double>,
        breathingHistory: List<Double>,
    ): List<Double> {
        return if (mode == MagnificationMode.Breathing) {
            breathingHistory
        } else {
            pulseHistory
        }
    }
}
