# Milestone E: GPU Color Magnification

Goal: move color magnification from CPU analysis toward texture processing.

## Tasks

- [ ] Render camera OES texture to internal RGB texture.
- [ ] Add downsample pyramid textures.
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

## Success Criteria

- Sustained 30 fps at 720p display with reduced internal processing size.
- CPU load is meaningfully lower than the CPU MVP.
- Processed recording still works.
