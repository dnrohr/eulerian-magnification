package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.ViewMode

object PreviewPathPolicy {
    fun useGlPreview(
        settings: AnalysisSettings,
        requestedGlPreview: Boolean,
        glPreviewAvailable: Boolean,
    ): Boolean {
        if (!glPreviewAvailable) return false
        return requestedGlPreview || settings.viewMode == ViewMode.Split
    }
}
