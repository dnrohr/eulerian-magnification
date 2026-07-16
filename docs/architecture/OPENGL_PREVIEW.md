# OpenGL Preview Path

## Current State

The app now has an optional GLES preview path behind the `Use GL Preview`
toggle. CameraX still supplies camera frames, but the display path can route
CameraX `Preview` into a `SurfaceTexture` backed by an external OES texture and
render that texture through GL while `ImageAnalysis` remains bound for CPU ROI
analysis and overlays.

The active GL preview pieces are:

- `OesShaderSource` defines GLES 3.0 shaders for sampling a camera `samplerExternalOES` texture with a transform matrix.
- The OES-to-RGB pass applies `SurfaceTexture.getTransformMatrix()` and uses an
  aspect-fill viewport so portrait Pixel preview framing preserves camera aspect
  and clips overflow similarly to CameraX `FILL_CENTER`.
- RGB framebuffer display passes use regular GL texture coordinates, separate
  from the external camera texture coordinate layout.
- `GlProgram` compiles shaders, links programs, and throws `GlException` on shader, link, or GL errors.
- `GlFrameTimer` tracks render-frame duration separately from camera frame
  arrival cadence. The debug overlay reports GL camera FPS from
  `SurfaceTexture` frame callbacks and render milliseconds from draw duration,
  so full-frame fallback decisions can catch a slow or stalled camera stream
  even when each rendered frame is cheap.
- The earlier animated debug renderer remains useful as a reference pattern, but
  the user-facing toggle now targets the live camera OES path.

## Verification

- JVM tests cover shader-source expectations and frame-timing math.
- JVM tests cover the OES/RGB coordinate layouts and aspect-fill crop
  calculations used to avoid upside-down or stretched portrait output.
- Android build verifies GLES API usage compiles into the debug APK.
- Pixel 8a smoke testing found and fixed the GLSL version-placement crash and
  the portrait GL aspect/stretch issue. Remaining Pixel validation must still
  prove watched target output is upright, nonblank, not stretched, and visibly
  magnified for the active live EVM modes.

## Next Work

- Run the watched Pixel validation plans for full-frame linear and phase live
  output. The GL path should not be marked complete from smoke screenshots alone.
- Use the same GL output as the source for processed encoder rendering.
