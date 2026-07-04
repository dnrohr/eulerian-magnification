# Milestone D: OpenGL Camera Preview

Goal: establish the zero/low-copy camera-to-GPU preview path before implementing GPU EVM.

## Tasks

- [ ] Build camera to `SurfaceTexture` / OES external texture flow.
- [ ] Render OES texture to screen through OpenGL ES 3.x.
- [x] Add basic shader compilation, GL error reporting, and frame timing.
- [x] Keep CameraX interop and Camera2 fallback decisions documented.
- [ ] Add render smoke tests or device verification notes.
- [ ] Commit and push to `main`.

## Completed Slice: GLES Infrastructure

- Added OES camera shader sources.
- Added GLES program compile/link/error helper.
- Added GL frame-timing accumulator.
- Added unit tests for shader source expectations and frame timing.
- Documented CameraX/Camera2 GL path decisions in `docs/architecture/OPENGL_PREVIEW.md`.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Success Criteria

- Camera preview renders through GLES without CPU frame copies.
- Frame timing overlay reports display FPS and render cost.
