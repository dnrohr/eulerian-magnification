# OpenGL Preview Path

## Current Slice

Milestone D now has the first GLES infrastructure pieces:

- `OesShaderSource` defines GLES 3.0 shaders for sampling a camera `samplerExternalOES` texture with a transform matrix.
- `GlProgram` compiles shaders, links programs, and throws `GlException` on shader, link, or GL errors.
- `GlFrameTimer` tracks render-frame duration, average frame time, and average FPS.

This is infrastructure only. The app still uses CameraX `PreviewView` for the live camera image. The next Milestone D slice should attach a `GLSurfaceView` or equivalent EGL surface, create an OES texture, connect it to `SurfaceTexture`, and bind CameraX/Camera2 output to that surface.

The app also includes an optional GL debug renderer toggle. It creates a `GLSurfaceView`, compiles a GLES 3.0 shader program, renders an animated full-screen triangle, and reports GL frame timing in the overlay. This proves the render loop and timing plumbing compile and can be device-smoke-tested without changing the active CameraX preview path.

## Verification

- JVM tests cover shader-source expectations and frame-timing math.
- Android build verifies GLES API usage compiles into the debug APK.

## Next Work

- Create a GLSurfaceView renderer.
- Allocate camera OES texture and `SurfaceTexture`.
- Bind the camera stream to the GL surface.
- Render the OES texture full-screen.
- Surface GL frame timing in the app overlay.
