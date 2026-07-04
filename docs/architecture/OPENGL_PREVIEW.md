# OpenGL Preview Path

## Current Slice

Milestone D now has the first GLES infrastructure pieces:

- `OesShaderSource` defines GLES 3.0 shaders for sampling a camera `samplerExternalOES` texture with a transform matrix.
- `GlProgram` compiles shaders, links programs, and throws `GlException` on shader, link, or GL errors.
- `GlFrameTimer` tracks render-frame duration, average frame time, and average FPS.

This is infrastructure only. The app still uses CameraX `PreviewView` for the live camera image. The next Milestone D slice should attach a `GLSurfaceView` or equivalent EGL surface, create an OES texture, connect it to `SurfaceTexture`, and bind CameraX/Camera2 output to that surface.

The app also includes an optional GL preview toggle. It creates a `GLSurfaceView`, allocates an OES external texture, connects it to `SurfaceTexture`, gives that surface to CameraX `Preview`, and renders the camera texture with GLES. CameraX `ImageAnalysis` stays bound beside the GL preview so the CPU ROI analysis and overlay controls continue to work during GL preview testing.

The earlier animated debug renderer remains useful as a reference pattern, but the active toggle now targets the camera OES path.

## Verification

- JVM tests cover shader-source expectations and frame-timing math.
- Android build verifies GLES API usage compiles into the debug APK.

## Next Work

- Run Pixel 8a device verification for the GL preview path.
- Fix orientation/aspect handling once device screenshots show the exact framing.
- Use the same GL output as the source for processed encoder rendering.
