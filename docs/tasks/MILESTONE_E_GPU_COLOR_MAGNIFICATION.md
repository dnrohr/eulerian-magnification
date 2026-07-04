# Milestone E: GPU Color Magnification

Goal: move color magnification from CPU analysis toward texture processing.

## Tasks

- [x] Render camera OES texture to internal RGB texture.
- [x] Add downsample pyramid textures.
- [ ] Add temporal filter state textures.
- [x] Implement color amplification shader pass.
- [x] Add ROI-limited processing when possible.
- [ ] Render processed output to display and encoder surfaces.
- [ ] Add side-by-side raw/amplified view.
- [ ] Benchmark against CPU MVP.
- [ ] Commit and push to `main`.

## Completed Slice: ROI Color Amplification Shader

- Added GLES shader source for ROI-limited color amplification.
- Added difference-mode support in the shader source.
- Added parameter mapping from `AnalysisSample` and `AnalysisSettings` to shader-style uniforms.
- Reuses `ArtifactSuppressor` for low-signal suppression and amplification caps.
- Added tests for shader source expectations and uniform mapping.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Downsample Pyramid Texture Foundation

- Added `GlPyramid` for framebuffer-backed reduced-resolution texture levels.
- Camera GL renderer now allocates a three-level downsample pyramid beside the internal RGB target.
- Added tests for pyramid sizing, one-pixel clamping, and invalid level counts.

This allocates pyramid textures only; the actual downsample shader pass is still pending.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: OES To RGB Render Target

- Added framebuffer-backed RGBA render target support.
- Added RGB texture display shader source.
- Camera GL renderer now renders the OES camera texture into an internal RGBA texture before displaying it.
- Added tests for render-target sizing and RGB shader source expectations.

Device runtime verification is still needed to confirm the OES-to-RGB path renders correctly on Pixel 8a.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Success Criteria

- Sustained 30 fps at 720p display with reduced internal processing size.
- CPU load is meaningfully lower than the CPU MVP.
- Processed recording still works.
