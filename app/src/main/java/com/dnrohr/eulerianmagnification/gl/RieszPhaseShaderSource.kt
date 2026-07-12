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

    const val LIVE_PHASE_TEMPORAL_FRAGMENT = """#version 300 es
        precision mediump float;
        uniform sampler2D uCurrentPhaseTexture;
        uniform sampler2D uPreviousWrappedPhaseTexture;
        uniform sampler2D uPreviousUnwrappedPhaseTexture;
        uniform sampler2D uPreviousLowTexture;
        uniform sampler2D uPreviousHighTexture;
        uniform float uLowAlpha;
        uniform float uHighAlpha;
        uniform float uAmplification;
        uniform float uAmplitudeThreshold;
        uniform int uInitialized;
        in vec2 vTexCoord;
        layout(location = 0) out vec4 outWrappedPhase;
        layout(location = 1) out vec4 outUnwrappedPhase;
        layout(location = 2) out vec4 outLowpass;
        layout(location = 3) out vec4 outHighpass;
        layout(location = 4) out vec4 outAmplifiedPhase;

        const float PI = 3.141592653589793;

        float decodePhase(float encoded) {
            return (encoded - 0.5) * 2.0 * PI;
        }

        float encodePhase(float phase) {
            return atan(sin(phase), cos(phase)) / (2.0 * PI) + 0.5;
        }

        float wrapDelta(float delta) {
            return atan(sin(delta), cos(delta));
        }

        void main() {
            vec4 current = texture(uCurrentPhaseTexture, vTexCoord);
            float currentPhase = decodePhase(current.r);
            if (uInitialized == 0) {
                outWrappedPhase = vec4(encodePhase(currentPhase), current.gba);
                outUnwrappedPhase = vec4(currentPhase, current.gba);
                outLowpass = vec4(currentPhase, current.gba);
                outHighpass = vec4(currentPhase, current.gba);
                outAmplifiedPhase = vec4(encodePhase(currentPhase), current.gba);
                return;
            }

            vec4 previousWrapped = texture(uPreviousWrappedPhaseTexture, vTexCoord);
            float previousWrappedPhase = decodePhase(previousWrapped.r);
            float previousUnwrapped = texture(uPreviousUnwrappedPhaseTexture, vTexCoord).r;
            float unwrappedPhase = previousUnwrapped + wrapDelta(currentPhase - previousWrappedPhase);
            float previousLow = texture(uPreviousLowTexture, vTexCoord).r;
            float previousHigh = texture(uPreviousHighTexture, vTexCoord).r;
            float low = mix(previousLow, unwrappedPhase, uLowAlpha);
            float high = mix(previousHigh, unwrappedPhase, uHighAlpha);
            float bandpassedPhase = high - low;
            float gatedAmplification = current.g >= uAmplitudeThreshold ? uAmplification : 0.0;
            float amplifiedPhase = currentPhase + bandpassedPhase * gatedAmplification;

            outWrappedPhase = vec4(encodePhase(currentPhase), current.gba);
            outUnwrappedPhase = vec4(unwrappedPhase, current.gba);
            outLowpass = vec4(low, current.gba);
            outHighpass = vec4(high, current.gba);
            outAmplifiedPhase = vec4(encodePhase(amplifiedPhase), current.gba);
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

    const val LIVE_PHASE_COMPOSE_FRAGMENT = """#version 300 es
        precision mediump float;
        uniform sampler2D uRawTexture;
        uniform sampler2D uPhaseReconstructedTexture;
        uniform vec4 uRoi;
        uniform int uViewMode;
        in vec2 vTexCoord;
        out vec4 outColor;

        const int VIEW_AMPLIFIED = 0;
        const int VIEW_DIFFERENCE = 1;
        const int VIEW_SPLIT = 2;

        bool insideRoi(vec2 uv) {
            return uv.x >= uRoi.x && uv.x <= uRoi.z && uv.y >= uRoi.y && uv.y <= uRoi.w;
        }

        vec2 roiUv(vec2 uv) {
            return (uv - uRoi.xy) / max(uRoi.zw - uRoi.xy, vec2(0.0001));
        }

        vec3 differenceColor(vec3 raw, vec3 reconstructed) {
            float delta = reconstructed.r - dot(raw, vec3(0.299, 0.587, 0.114));
            float strength = clamp(abs(delta) * 8.0, 0.0, 1.0);
            vec3 positive = vec3(1.0, 0.48, 0.08);
            vec3 negative = vec3(0.08, 0.42, 1.0);
            return mix(vec3(0.06), delta >= 0.0 ? positive : negative, strength);
        }

        void main() {
            vec4 raw = texture(uRawTexture, vTexCoord);
            if (!insideRoi(vTexCoord)) {
                outColor = raw;
                return;
            }

            vec4 reconstructed = texture(uPhaseReconstructedTexture, roiUv(vTexCoord));
            vec3 phaseRgb = vec3(reconstructed.r);
            if (uViewMode == VIEW_DIFFERENCE) {
                outColor = vec4(differenceColor(raw.rgb, phaseRgb), raw.a);
                return;
            }
            if (uViewMode == VIEW_SPLIT) {
                if (vTexCoord.x < 0.5) {
                    outColor = raw;
                } else {
                    outColor = vec4(phaseRgb, raw.a);
                }
                return;
            }
            outColor = vec4(phaseRgb, raw.a);
        }
    """
}
