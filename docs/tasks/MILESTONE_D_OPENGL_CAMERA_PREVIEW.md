# Milestone D: OpenGL Camera Preview

Goal: establish the zero/low-copy camera-to-GPU preview path before implementing GPU EVM.

## Tasks

- [ ] Build camera to `SurfaceTexture` / OES external texture flow.
- [ ] Render OES texture to screen through OpenGL ES 3.x.
- [ ] Add basic shader compilation, GL error reporting, and frame timing.
- [ ] Keep CameraX interop and Camera2 fallback decisions documented.
- [ ] Add render smoke tests or device verification notes.
- [ ] Commit and push to `main`.

## Success Criteria

- Camera preview renders through GLES without CPU frame copies.
- Frame timing overlay reports display FPS and render cost.
