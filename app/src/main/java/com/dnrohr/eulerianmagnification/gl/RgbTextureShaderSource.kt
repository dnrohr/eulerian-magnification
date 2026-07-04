package com.dnrohr.eulerianmagnification.gl

object RgbTextureShaderSource {
    const val VERTEX = """
        #version 300 es
        layout(location = 0) in vec4 aPosition;
        layout(location = 1) in vec2 aTexCoord;
        out vec2 vTexCoord;

        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
    """

    const val FRAGMENT = """
        #version 300 es
        precision mediump float;
        uniform sampler2D uInputTexture;
        in vec2 vTexCoord;
        out vec4 outColor;

        void main() {
            outColor = texture(uInputTexture, vTexCoord);
        }
    """
}
