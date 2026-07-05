package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewRoiMapperTest {
    @Test
    fun keepsRoiWhenGeometryAlreadyMatches() {
        val mapped = PreviewRoiMapper.mapAnalysisToPreview(
            roi = NormalizedRect(0.2f, 0.3f, 0.4f, 0.5f),
            frameSize = PreviewSize(640, 480),
            previewSize = PreviewSize(640, 480),
            rotationDegrees = 0,
            mirrorHorizontally = false,
        )

        assertRect(0.2f, 0.3f, 0.4f, 0.5f, mapped)
    }

    @Test
    fun mirrorsFrontCameraRoiHorizontally() {
        val mapped = PreviewRoiMapper.mapAnalysisToPreview(
            roi = NormalizedRect(0.1f, 0.2f, 0.3f, 0.4f),
            frameSize = PreviewSize(640, 480),
            previewSize = PreviewSize(640, 480),
            rotationDegrees = 0,
            mirrorHorizontally = true,
        )

        assertRect(0.7f, 0.2f, 0.9f, 0.4f, mapped)
    }

    @Test
    fun rotatesAnalysisRoiClockwiseForPortraitPreview() {
        val mapped = PreviewRoiMapper.mapAnalysisToPreview(
            roi = NormalizedRect(0.1f, 0.2f, 0.3f, 0.5f),
            frameSize = PreviewSize(640, 480),
            previewSize = PreviewSize(480, 640),
            rotationDegrees = 90,
            mirrorHorizontally = false,
        )

        assertRect(0.2f, 0.7f, 0.5f, 0.9f, mapped)
    }

    @Test
    fun accountsForAspectFillHorizontalCropAfterRotation() {
        val mapped = PreviewRoiMapper.mapAnalysisToPreview(
            roi = NormalizedRect(0.25f, 0.25f, 0.75f, 0.75f),
            frameSize = PreviewSize(640, 480),
            previewSize = PreviewSize(1080, 2400),
            rotationDegrees = 90,
            mirrorHorizontally = true,
        )

        assertRect(0.0833f, 0.25f, 0.9167f, 0.75f, mapped, tolerance = 0.0002f)
    }

    private fun assertRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        actual: NormalizedRect,
        tolerance: Float = 0.0001f,
    ) {
        assertEquals(left, actual.left, tolerance)
        assertEquals(top, actual.top, tolerance)
        assertEquals(right, actual.right, tolerance)
        assertEquals(bottom, actual.bottom, tolerance)
    }
}
