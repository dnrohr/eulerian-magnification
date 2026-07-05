package com.dnrohr.eulerianmagnification.gl

object RieszPhaseShaderSource {
    const val VERTEX = """#version 300 es
        layout(location = 0) in vec4 aPosition;
        layout(location = 1) in vec2 aTexCoord;
        out vec2 vTexCoord;

        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
    """

    const val RIESZ_COMPONENT_FRAGMENT = """#version 300 es
        precision mediump float;
        uniform sampler2D uInputTexture;
        uniform vec2 uTexelSize;
        in vec2 vTexCoord;
        out vec4 outColor;

        float luminanceAt(vec2 uv) {
            vec3 rgb = texture(uInputTexture, clamp(uv, vec2(0.0), vec2(1.0))).rgb;
            return dot(rgb, vec3(0.299, 0.587, 0.114));
        }

        void main() {
            float center = luminanceAt(vTexCoord);
            float left = luminanceAt(vTexCoord - vec2(uTexelSize.x, 0.0));
            float right = luminanceAt(vTexCoord + vec2(uTexelSize.x, 0.0));
            float up = luminanceAt(vTexCoord - vec2(0.0, uTexelSize.y));
            float down = luminanceAt(vTexCoord + vec2(0.0, uTexelSize.y));
            float rieszX = (right - left) * 0.5;
            float rieszY = (down - up) * 0.5;
            float amplitude = length(vec3(center, rieszX, rieszY));
            outColor = vec4(rieszX * 0.5 + 0.5, rieszY * 0.5 + 0.5, amplitude, center);
        }
    """

    const val PHASE_PROJECT_FRAGMENT = """#version 300 es
        precision mediump float;
        uniform sampler2D uRieszTexture;
        uniform float uOrientationRadians;
        in vec2 vTexCoord;
        out vec4 outColor;

        const float PI = 3.141592653589793;

        float encodePhase(float phase) {
            return phase / (2.0 * PI) + 0.5;
        }

        void main() {
            vec4 packed = texture(uRieszTexture, vTexCoord);
            float rieszX = packed.r * 2.0 - 1.0;
            float rieszY = packed.g * 2.0 - 1.0;
            float source = packed.a;
            float oriented = rieszX * cos(uOrientationRadians) + rieszY * sin(uOrientationRadians);
            float amplitude = length(vec2(source, oriented));
            float phase = atan(oriented, source);
            outColor = vec4(encodePhase(phase), amplitude, oriented * 0.5 + 0.5, source);
        }
    """

    const val PHASE_AMPLIFY_FRAGMENT = """#version 300 es
        precision mediump float;
        uniform sampler2D uReferencePhaseTexture;
        uniform sampler2D uCurrentPhaseTexture;
        uniform float uAmplification;
        in vec2 vTexCoord;
        out vec4 outColor;

        const float PI = 3.141592653589793;

        float decodePhase(float encoded) {
            return (encoded - 0.5) * 2.0 * PI;
        }

        float encodePhase(float phase) {
            return atan(sin(phase), cos(phase)) / (2.0 * PI) + 0.5;
        }

        void main() {
            vec4 reference = texture(uReferencePhaseTexture, vTexCoord);
            vec4 current = texture(uCurrentPhaseTexture, vTexCoord);
            float referencePhase = decodePhase(reference.r);
            float currentPhase = decodePhase(current.r);
            float delta = atan(sin(currentPhase - referencePhase), cos(currentPhase - referencePhase));
            float amplifiedPhase = referencePhase + delta * uAmplification;
            outColor = vec4(encodePhase(amplifiedPhase), current.g, current.b, current.a);
        }
    """

    const val PHASE_RECONSTRUCT_FRAGMENT = """#version 300 es
        precision mediump float;
        uniform sampler2D uAmplifiedPhaseTexture;
        in vec2 vTexCoord;
        out vec4 outColor;

        const float PI = 3.141592653589793;

        float decodePhase(float encoded) {
            return (encoded - 0.5) * 2.0 * PI;
        }

        void main() {
            vec4 phaseSample = texture(uAmplifiedPhaseTexture, vTexCoord);
            float phase = decodePhase(phaseSample.r);
            float amplitude = phaseSample.g;
            float reconstructed = clamp(amplitude * cos(phase), 0.0, 1.0);
            outColor = vec4(vec3(reconstructed), 1.0);
        }
    """
}
