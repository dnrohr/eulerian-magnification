package com.dnrohr.eulerianmagnification.analysis

enum class PreviewRenderPath {
    CameraX,
    Gl,
}

data class PreviewRoiMappingPolicy(
    val renderPath: PreviewRenderPath,
    val mirrorHorizontally: Boolean,
) {
    companion object {
        fun frontCamera(renderPath: PreviewRenderPath): PreviewRoiMappingPolicy {
            return PreviewRoiMappingPolicy(
                renderPath = renderPath,
                mirrorHorizontally = true,
            )
        }
    }
}
