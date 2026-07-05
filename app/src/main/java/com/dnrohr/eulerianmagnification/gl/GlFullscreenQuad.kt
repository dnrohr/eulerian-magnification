package com.dnrohr.eulerianmagnification.gl

object GlFullscreenQuad {
    val EXTERNAL_TEXTURE = floatArrayOf(
        -1.0f, -1.0f, 0.0f, 0.0f,
        1.0f, -1.0f, 1.0f, 0.0f,
        -1.0f, 1.0f, 0.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
    )

    val FRAMEBUFFER_TEXTURE = floatArrayOf(
        -1.0f, -1.0f, 0.0f, 0.0f,
        1.0f, -1.0f, 1.0f, 0.0f,
        -1.0f, 1.0f, 0.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
    )
}
