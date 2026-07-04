package com.dnrohr.eulerianmagnification.gl

object OesShaderSource {
    const val VERTEX = """
        #version 300 es
        layout(location = 0) in vec4 aPosition;
        layout(location = 1) in vec2 aTexCoord;
        uniform mat4 uTexTransform;
        out vec2 vTexCoord;

        void main() {
            gl_Position = aPosition;
            vec4 transformed = uTexTransform * vec4(aTexCoord, 0.0, 1.0);
            vTexCoord = transformed.xy;
        }
    """

    const val FRAGMENT = """
        #version 300 es
        #extension GL_OES_EGL_image_external_essl3 : require
        precision mediump float;
        uniform samplerExternalOES uCameraTexture;
        in vec2 vTexCoord;
        out vec4 outColor;

        void main() {
            outColor = texture(uCameraTexture, vTexCoord);
        }
    """
}
