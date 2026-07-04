# Milestone E: GPU Color Magnification

Goal: move color magnification from CPU analysis toward texture processing.

## Tasks

- [x] Render camera OES texture to internal RGB texture.
- [x] Add downsample pyramid textures.
- [x] Add temporal filter state textures.
- [x] Implement color amplification shader pass.
- [x] Add ROI-limited processing when possible.
- [ ] Render processed output to display and encoder surfaces.
- [x] Add side-by-side raw/amplified view.
- [ ] Benchmark against CPU MVP.
- [ ] Commit and push to `main`.

## Completed Slice: CPU/GL Benchmark Readout Foundation

- Added a `PerformanceBenchmark` model that compares CPU analysis FPS/latency with GL render FPS/frame time.
- GL preview overlay now shows a benchmark summary with the 30 fps target status and CPU/GL FPS delta.
- Added unit tests for benchmark mapping, target detection, and summary formatting.

The actual CPU MVP comparison still needs a Pixel 8a runtime pass, so the benchmark checklist item remains open until measured values are recorded.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Supporting Slice: Recorded-Frame Analysis Fixture

- Added a synthetic recorded-frame analyzer and tests under the analysis package.
- This gives CPU/GPU benchmark work a repeatable pre-device input path before phone testing.
- See `docs/testing/RECORDED_VIDEO_VALIDATION.md` for the sample-video workflow.

## Completed Slice: GL Raw/Amplified Split View

- Added a `Split` view mode alongside Raw, Amplified, and Difference.
- GL preview split mode draws the raw RGB render target on the left and the processed color-magnified render target on the right.
- Added a viewport layout helper for deterministic full-screen and horizontal split viewport sizing.
- Added unit tests for split-mode uniform mapping and odd/tiny split viewport behavior.

The split view is display-only. Encoder-surface rendering remains pending.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: ROI Color Amplification Shader

- Added GLES shader source for ROI-limited color amplification.
- Added difference-mode support in the shader source.
- Added parameter mapping from `AnalysisSample` and `AnalysisSettings` to shader-style uniforms.
- Reuses `ArtifactSuppressor` for low-signal suppression and amplification caps.
- Added tests for shader source expectations and uniform mapping.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Color Pass Display Wiring

- Camera GL renderer now runs the ROI color magnification shader over the internal RGB texture.
- The processed texture is rendered to the display path in GL preview mode.
- CPU analysis still supplies the ROI and bandpassed signal uniforms while temporal filtering is being moved toward GPU state.

Encoder-surface rendering is still pending, so the combined display/encoder checklist item remains open.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Temporal State Texture Foundation

- Added ping-pong temporal state render targets for each pyramid level.
- Camera GL renderer now allocates temporal state alongside the downsample pyramid and swaps state each frame.
- Added tests for temporal state sizing and two-target-per-level planning.

This allocates state textures only; the actual temporal filter shader update pass is still pending.

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
