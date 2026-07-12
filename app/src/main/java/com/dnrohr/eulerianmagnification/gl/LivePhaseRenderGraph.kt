package com.dnrohr.eulerianmagnification.gl

data class LivePhaseRenderGraphPlan(
    val roiPlan: LivePhaseRoiPlan,
    val passes: List<LivePhaseRenderPass> = DEFAULT_PASSES,
) {
    val maxColorAttachmentsRequired: Int = passes.maxOf { it.outputCount }
    val roiTargetCount: Int = roiPlan.renderTargetCount
    val estimatedRoiBytes: Int = roiPlan.estimatedBytes
    val fitsDeviceAttachmentFloor: Boolean =
        maxColorAttachmentsRequired <= GLES3_GUARANTEED_COLOR_ATTACHMENTS

    companion object {
        const val GLES3_GUARANTEED_COLOR_ATTACHMENTS = 4

        val DEFAULT_PASSES = listOf(
            LivePhaseRenderPass.ExtractRoi,
            LivePhaseRenderPass.RieszComponents,
            LivePhaseRenderPass.ProjectPhase,
            LivePhaseRenderPass.UpdateTemporalState,
            LivePhaseRenderPass.AmplifyPhase,
            LivePhaseRenderPass.ReconstructRoi,
            LivePhaseRenderPass.ComposeFullFrame,
        )
    }
}

enum class LivePhaseRenderPass(
    val label: String,
    val outputCount: Int,
) {
    ExtractRoi("extract ROI", 1),
    RieszComponents("Riesz components", 1),
    ProjectPhase("phase projection", 1),
    UpdateTemporalState("temporal state update", 4),
    AmplifyPhase("amplified phase", 1),
    ReconstructRoi("reconstruct ROI", 1),
    ComposeFullFrame("compose full frame", 1),
}
