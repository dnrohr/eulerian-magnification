# Milestone D: OpenGL Camera Preview

Goal: establish the zero/low-copy camera-to-GPU preview path before implementing GPU EVM.

## Tasks

- [x] Build camera to `SurfaceTexture` / OES external texture flow.
- [x] Render OES texture to screen through OpenGL ES 3.x.
- [x] Add basic shader compilation, GL error reporting, and frame timing.
- [x] Keep CameraX interop and Camera2 fallback decisions documented.
- [x] Add render smoke tests or device verification notes.
- [x] Commit and push to `main`.

## Completed Slice: GLES Infrastructure

- Added OES camera shader sources.
- Added GLES program compile/link/error helper.
- Added GL frame-timing accumulator.
- Added unit tests for shader source expectations and frame timing.
- Documented CameraX/Camera2 GL path decisions in `docs/architecture/OPENGL_PREVIEW.md`.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Camera OES Preview Path

- Added `CameraOesRenderer`, which creates an OES external texture, connects it to `SurfaceTexture`, provides that surface to CameraX `Preview`, and renders it through GLES.
- The GL preview toggle now switches between CameraX `PreviewView` and the CameraX to `SurfaceTexture` to GLES path.
- Existing CameraX `ImageAnalysis` remains bound beside the GL preview so CPU ROI analysis and overlays continue to work.
- GL frame timing continues to feed the overlay.

Device runtime verification on Pixel 8a is still needed to confirm camera frames render correctly through the GLES path.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: GL Debug Renderer

- Added an optional `GLSurfaceView` debug renderer toggle.
- The renderer compiles and uses a GLES 3.0 shader program, draws a full-screen animated triangle, and reports GL frame FPS/frame time to the overlay.
- This is a render smoke path, not the camera OES texture path yet.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Success Criteria

- Camera preview renders through GLES without CPU frame copies.
- Frame timing overlay reports display FPS and render cost.
