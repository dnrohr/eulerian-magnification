# Milestone E: GPU Color Magnification

Goal: move color magnification from CPU analysis toward texture processing.

## Tasks

- [x] Render camera OES texture to internal RGB texture.
- [x] Add downsample pyramid textures.
- [x] Add temporal filter state textures.
- [x] Implement color amplification shader pass.
- [x] Add ROI-limited processing when possible.
- [x] Render processed output to display and encoder surfaces.
- [x] Add side-by-side raw/amplified view.
- [x] Benchmark against CPU MVP.
- [x] Commit and push to `main`.

## Completed Slice: Pixel 8a Short-Run GL Benchmark

- Captured a CameraX CPU preview sample and a GL preview sample on Pixel 8a.
- Documented the benchmark in `docs/experiments/pixel8a_gpu_benchmark.md`.
- CameraX preview sample: 854 frames, 15 ms median frame time, 29 ms p99, and 32.55% janky frames.
- GL preview sample: 350 frames over the short run, 11 ms median frame time, 16 ms p99, and 1.14% janky frames.
- The GL display path met the 30 FPS display target in the short sample while CPU ROI analysis remained active.
- A follow-up GL smoke attempt exposed a Pixel 8a shader compile crash caused by shader source formatting; this was fixed in the GL shader version-declaration slice.

This closes the short-run CPU MVP comparison. Sustained CPU load, thermal behavior, and battery impact remain tracked under Milestone J polish/long-run benchmarks.

## Completed Slice: Pixel 8a GL Shader Version Fix

- Fixed GLES shader source constants so `#version 300 es` is the first line of each shader.
- Covered OES, RGB texture, color magnification, and debug renderer shader sources.
- Added unit coverage for public shader source version placement.
- Verified the crash root cause through `adb logcat -b crash` and `dumpsys activity exit-info`.
- Reinstalled the fixed APK on Pixel 8a and confirmed GL preview stays alive with no new package crash in the cleared crash buffer.
- Follow-up smoke showed the GL preview image upright after clean app reboot, but stretched/cropped differently from CameraX; this was fixed by orienting the camera buffer aspect to the surface and aspect-filling the OES-to-RGB pass.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.gl.OesShaderSourceTest" --tests "com.dnrohr.eulerianmagnification.gl.RgbTextureShaderSourceTest" --tests "com.dnrohr.eulerianmagnification.gl.ColorMagnificationPassTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`
- `adb logcat -b crash -d`
- Pixel 8a GL preview smoke screenshot after reinstall

## Completed Slice: GL Preview Framing Fix

- Added named fullscreen quads for external `SurfaceTexture` sampling and regular framebuffer texture sampling.
- Camera OES input now uses normal OpenGL coordinates and relies on `SurfaceTexture.getTransformMatrix()` for orientation.
- Color-processing and screen-display passes now use regular OpenGL framebuffer texture coordinates.
- The OES-to-RGB pass now orients the camera buffer aspect to the surface and draws through an aspect-fill viewport before color processing/display.
- Added unit coverage for the two coordinate layouts and aspect-fill crop calculations.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.gl.GlFullscreenQuadTest"`
- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.gl.GlViewportLayoutTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`
- `adb install -r app\build\outputs\apk\debug\app-debug.apk`
- Pixel 8a live CameraX preview sample
- Pixel 8a live GL preview `dumpsys gfxinfo` sample

## Completed Slice: CPU/GL Benchmark Readout Foundation

- Added a `PerformanceBenchmark` model that compares CPU analysis FPS/latency with GL render FPS/frame time.
- GL preview overlay now shows a benchmark summary with the 30 fps target status and CPU/GL FPS delta.
- Added unit tests for benchmark mapping, target detection, and summary formatting.

The actual CPU MVP comparison is now documented in the Pixel 8a short-run benchmark slice. Sustained thermal and CPU-load benchmarking remains separate polish work.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Supporting Slice: Recorded-Frame Analysis Fixture

- Added a synthetic recorded-frame analyzer and tests under the analysis package.
- Added a report runner for repeatable recorded-frame FPS and signal-energy metrics.
- Added Android local-video frame decoding into the recorded-frame input format.
- Added a validator facade that combines local-video decode and recorded-frame reporting.
- Added an in-app local-video picker that runs the validator before phone-camera testing.
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

## Supporting Slice: Processed GL Frame Export Hook

- Added `ProcessedGlFrame` as the renderer-to-encoder handoff model for the
  processed texture id, texture size, split-mode flag, and presentation
  timestamp.
- `CameraOesRenderer` now emits the processed texture after the color pass and
  before drawing to the display surface.
- `ColorMagnificationUniforms` carries the presentation timestamp that a future
  EGL encoder-surface renderer should apply with `eglPresentationTimeANDROID`.
- Documented the remaining encoder-surface assignment in the GPU and recording
  architecture notes.

This exposes the display texture for encoder work, but does not yet render it to
`MediaCodec`'s input surface. The combined display/encoder checklist item stays
open.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.gl.ColorMagnificationPassTest" --tests "com.dnrohr.eulerianmagnification.gl.ProcessedGlFrameTest"`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

## Supporting Slice: GL Recording Timestamp Wiring

- GL preview now updates color uniforms after the recording session assigns a
  monotonic presentation timestamp.
- `ProcessedGlFrame.presentationTimestampNanos` matches the recording metadata
  timeline while recording is active.

The display path is unchanged. Encoder-surface rendering remains the final open
piece of the combined display/encoder checklist item.

## Verification

- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

## Supporting Slice: EGL Encoder Surface Renderer Foundation

- Added `GlEncoderSurfaceRenderer` to render a processed GL texture onto a
  recordable `MediaCodec` input surface.
- The renderer uses a shared GLES 3 context and assigns the processed frame's
  presentation timestamp with `eglPresentationTimeANDROID`.
- Added unit coverage for recordable EGL surface config attributes.

This is the encoder rendering component, but it is not yet connected to live
recording. The display/encoder checklist item remains open until live GL frames
are written through this path.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.gl.GlEncoderSurfaceRendererTest"`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

## Completed Slice: Live GL Processed MP4 Recorder

- GL preview still renders the processed color-magnified texture to display.
- While recording in GL preview, the same emitted `ProcessedGlFrame` is forwarded
  into `GlProcessedMp4Recorder`.
- The recorder draws that processed texture to the `MediaCodec` encoder input
  surface with the frame's monotonic presentation timestamp.
- Added Pixel 8a instrumentation coverage for the real GL texture to MP4 encoder
  surface path.

## Verification

- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug assembleDebugAndroidTest`
- `.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.dnrohr.eulerianmagnification.recording.GlProcessedMp4RecorderInstrumentedTest'`

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
