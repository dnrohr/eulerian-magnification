package com.dnrohr.eulerianmagnification

import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewPathPolicyTest {
    @Test
    fun usesRequestedGlPreviewWhenAvailable() {
        assertTrue(
            PreviewPathPolicy.useGlPreview(
                settings = AnalysisSettings(viewMode = ViewMode.Amplified),
                requestedGlPreview = true,
                glPreviewAvailable = true,
            )
        )
    }

    @Test
    fun splitViewUsesGlPreviewWhenAvailable() {
        assertTrue(
            PreviewPathPolicy.useGlPreview(
                settings = AnalysisSettings(viewMode = ViewMode.Split),
                requestedGlPreview = false,
                glPreviewAvailable = true,
            )
        )
    }

    @Test
    fun cannotUseGlPreviewWhenUnavailable() {
        assertFalse(
            PreviewPathPolicy.useGlPreview(
                settings = AnalysisSettings(viewMode = ViewMode.Split),
                requestedGlPreview = true,
                glPreviewAvailable = false,
            )
        )
    }
}
