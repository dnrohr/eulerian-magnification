package com.dnrohr.eulerianmagnification.gl

import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LivePhaseRenderGraphTest {
    @Test
    fun graphKeepsTemporalStateWithinGlesGuaranteedAttachments() {
        val graph = LivePhaseRenderGraphPlan(roiPlan())

        assertEquals(4, graph.maxColorAttachmentsRequired)
        assertTrue(graph.fitsDeviceAttachmentFloor)
        assertEquals(
            LivePhaseRenderPass.UpdateTemporalState,
            graph.passes.single { it.outputCount == 4 },
        )
    }

    @Test
    fun graphUsesCorrectRoiTargetBudget() {
        val roiPlan = roiPlan()
        val graph = LivePhaseRenderGraphPlan(roiPlan)

        assertEquals(13, graph.roiTargetCount)
        assertEquals(roiPlan.estimatedBytes, graph.estimatedRoiBytes)
        assertTrue(graph.estimatedRoiBytes <= LivePhaseRoiPlan.DEFAULT_MEMORY_BUDGET_BYTES)
    }

    @Test
    fun graphDocumentsRuntimePassOrder() {
        val graph = LivePhaseRenderGraphPlan(roiPlan())

        assertEquals(
            listOf(
                LivePhaseRenderPass.ExtractRoi,
                LivePhaseRenderPass.RieszComponents,
                LivePhaseRenderPass.ProjectPhase,
                LivePhaseRenderPass.UpdateTemporalState,
                LivePhaseRenderPass.AmplifyPhase,
                LivePhaseRenderPass.ReconstructRoi,
                LivePhaseRenderPass.ComposeFullFrame,
            ),
            graph.passes,
        )
    }

    private fun roiPlan(): LivePhaseRoiPlan {
        return LivePhaseRoiPlan(
            surfaceSize = GlTextureSize(640, 480),
            roi = NormalizedRect(0.0f, 0.0f, 1.0f, 1.0f),
            maxTextureDimension = 320,
        )
    }
}
