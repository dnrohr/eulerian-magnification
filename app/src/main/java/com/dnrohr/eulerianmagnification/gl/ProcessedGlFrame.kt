package com.dnrohr.eulerianmagnification.gl

data class ProcessedGlFrame(
    val textureId: Int,
    val rawTextureId: Int? = null,
    val size: GlTextureSize,
    val presentationTimestampNanos: Long,
    val splitMode: Boolean,
) {
    init {
        require(textureId > 0) { "textureId must be a valid GL texture" }
        require(rawTextureId == null || rawTextureId > 0) { "rawTextureId must be a valid GL texture when provided" }
        require(presentationTimestampNanos >= 0L) { "presentationTimestampNanos must be non-negative" }
    }
}
