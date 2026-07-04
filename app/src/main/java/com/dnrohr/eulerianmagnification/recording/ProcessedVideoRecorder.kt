package com.dnrohr.eulerianmagnification.recording

import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import java.io.File

interface ProcessedVideoRecorder {
    val outputFile: File

    fun record(
        sample: AnalysisSample,
        settings: AnalysisSettings,
    )

    fun stop()
}
