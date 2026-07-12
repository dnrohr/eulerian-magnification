package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewRoiMappingPolicyTest {
    @Test
    fun frontCameraPolicyMirrorsCameraXAndGlPreviewPaths() {
        val cameraX = PreviewRoiMappingPolicy.frontCamera(PreviewRenderPath.CameraX)
        val gl = PreviewRoiMappingPolicy.frontCamera(PreviewRenderPath.Gl)

        assertEquals(PreviewRenderPath.CameraX, cameraX.renderPath)
        assertEquals(PreviewRenderPath.Gl, gl.renderPath)
        assertTrue(cameraX.mirrorHorizontally)
        assertTrue(gl.mirrorHorizontally)
    }

    @Test
    fun cameraXAndGlUseSameMappedRoiForFrontCameraPortraitPreview() {
        val roi = NormalizedRect(0.18f, 0.24f, 0.42f, 0.58f)
        val cameraX = PreviewRoiMapper.mapAnalysisToPreview(
            roi = roi,
            frameSize = PreviewSize(640, 480),
            previewSize = PreviewSize(1080, 2400),
            rotationDegrees = 90,
            mirrorHorizontally = PreviewRoiMappingPolicy.frontCamera(PreviewRenderPath.CameraX).mirrorHorizontally,
        )
        val gl = PreviewRoiMapper.mapAnalysisToPreview(
            roi = roi,
            frameSize = PreviewSize(640, 480),
            previewSize = PreviewSize(1080, 2400),
            rotationDegrees = 90,
            mirrorHorizontally = PreviewRoiMappingPolicy.frontCamera(PreviewRenderPath.Gl).mirrorHorizontally,
        )

        assertRect(cameraX, gl)
    }

    private fun assertRect(
        expected: NormalizedRect,
        actual: NormalizedRect,
        tolerance: Float = 0.0001f,
    ) {
        assertEquals(expected.left, actual.left, tolerance)
        assertEquals(expected.top, actual.top, tolerance)
        assertEquals(expected.right, actual.right, tolerance)
        assertEquals(expected.bottom, actual.bottom, tolerance)
    }
}
