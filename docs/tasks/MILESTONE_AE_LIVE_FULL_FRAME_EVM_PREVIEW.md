# Milestone AE - Live Full-Frame EVM Preview

Status: In progress

Importance: High. The live app needs true reconstructed output before it can feel like motion magnification.

Goal: integrate the full-frame EVM renderer into the live preview path.

## Tasks

- [x] Choose the first live implementation path: GL Pulse full-frame color preview bridge first, then GPU pyramid reconstruction.
- [x] Maintain temporal state across live frames for each pyramid level.
- [x] Render reconstructed output into GL preview.
- [x] Keep Raw, Amp, Diff, and Split views truthful while the bridge is limited to Pulse color.
- [x] Add frame-rate and latency safeguards that disable or degrade gracefully.
- [ ] Validate on Pixel in portrait orientation with a known target.
- [x] Update UI labels so users can distinguish ROI signal visualization from full-frame color preview.

## Implementation Notes

- Added `LiveEvmPreviewPolicy` to decide when live GL can use full-frame Pulse
  color output.
- Added a `fullFrameMode` uniform to the GL color pass. When enabled, the live
  Pulse color signal is applied across the frame instead of only inside the ROI.
- Suppressed the Compose ROI tint overlay during the full-frame GL bridge so the
  image is easier to inspect.
- Added an expanded-controls `Preview:` label.
- The initial bridge was not live MIT-style pyramid reconstruction. Later AE
  slices wired temporal pyramid state into GL reconstruction; the remaining AE
  work is Pixel portrait validation with a known target.

## Completed Slice: GL Camera Cadence Guard

- Split GL render-cost timing from camera frame-arrival cadence. `GlFrameTimer`
  now records `SurfaceTexture` frame callbacks for camera FPS while preserving
  render milliseconds for GPU cost.
- Updated full-frame live preview policy coverage so fast rendering does not
  mask a slow or stalled camera stream.
- Updated debug and benchmark labels to say `GL camera` FPS and render
  milliseconds explicitly.

## Completed Slice: Reconstruction Shader Plan

- Added `LivePyramidReconstructionPlan` to describe the half-resolution pyramid,
  temporal ping-pong target count, and pass count expected by the live GL path.
- Added `TemporalBandpassCoefficients` using the same first-order low/high
  alpha formula as the CPU temporal pyramid.
- Added shader-source contracts for downsample, temporal bandpass, and
  reconstruction passes.
- Added JVM coverage for pyramid sizing, temporal coefficients, invalid band
  rejection, and shader uniform/source expectations.
- This slice did not wire the runtime GL pass into `CameraOesRenderer`; later
  AE slices connected the pass graph. AE remains in progress until reconstructed
  output is validated on device.

## Completed Slice: Temporal State Targets

- Updated `GlTemporalState` so each pyramid level owns separate lowpass and
  highpass ping-pong targets plus a current bandpass target.
- Added a temporal update framebuffer that binds lowpass, highpass, and
  bandpass textures as multiple render outputs for the temporal shader.
- Updated `LivePyramidReconstructionPlan` to count five temporal render targets
  per level and to represent downsample, temporal update, and reconstruction
  passes.
- Updated shader contracts and JVM coverage so the temporal pass exposes both
  highpass history output and bandpass reconstruction output.
- This slice did not call the temporal shader from `CameraOesRenderer`; later
  AE slices compile/link and invoke the pass programs between RGB capture and
  output display.

## Completed Slice: Live Reconstruction Pass Graph

- Added explicit `GlRenderTargetFormat` support so temporal lowpass, highpass,
  and bandpass textures use signed half-float storage instead of clamping
  negative deltas into unsigned RGBA.
- Passed amplification and mode frequency bands into the GL uniform model so
  live reconstruction uses the same UI-selected settings as CPU analysis.
- Wired `CameraOesRenderer` to compile the downsample, temporal bandpass, and
  reconstruction shaders.
- When the existing full-frame preview policy enables GL full-frame output, the
  renderer now downsamples the RGB frame, updates temporal state per pyramid
  level, reconstructs into the processed render target, and uses that target for
  amplified and split display/export.
- The path builds and has JVM coverage for settings/format contracts, but it
  has not been visually checked on the phone yet. AE remains in progress until
  the Pixel portrait run proves the output is nonblank, upright, and visibly
  magnified.

## Completed Slice: Half-Float Capability Fallback

- Added a GL extension capability check for renderable half-float/float color
  buffers before allocating temporal bandpass targets.
- If the device does not advertise support, or if temporal target allocation
  fails, the renderer disables the live reconstruction path and falls back to
  the previous GL color preview bridge instead of crashing the camera surface.
- If the live reconstruction pass later raises a GL error, the renderer disables
  the reconstruction path for the session and renders the same frame with the
  GL color bridge fallback.
- Added JVM coverage for extension parsing so the fallback gate stays explicit.
- Device behavior still needs to be confirmed on the Pixel because local JVM
  tests can prove the gate logic, not the actual driver capability.

## Completed Slice: Temporal Warm Start

- Added a temporal initialization uniform to the live bandpass shader.
- On the first reconstructed frame after enabling/resetting the live path, the
  shader seeds lowpass and highpass history from the current pyramid level and
  emits zero bandpass.
- Subsequent frames use the normal high-minus-low bandpass output.
- This matches the CPU `BandpassFilter` startup behavior and avoids a startup
  flash that could be mistaken for real magnified motion.

## Completed Slice: Renderer Path Diagnostics

- Added a GL render-path field to frame stats.
- The expanded debug UI now reports whether the GL renderer is using the color
  bridge, live reconstruction, or live reconstruction fallback.
- This gives the Pixel validation run a direct way to confirm which path is
  visible before evaluating magnification quality.

## Completed Slice: Scrollable Expanded Controls

- During Pixel validation, expanded controls overflowed below the screen and the
  GL preview toggle could not be reached.
- Made the expanded controls vertically scrollable so renderer diagnostics,
  preview toggles, recording controls, and validation actions remain accessible
  on the Pixel 8a portrait layout.

## Completed Slice: Pixel Live Reconstruction Smoke

- Installed the updated debug APK on connected Pixel 8a `47091JEKB05516`.
- Enabled GL preview from the now-scrollable expanded controls.
- Captured an unattended portrait screenshot showing `GL renderer: Live
  reconstruction`.
- The preview was upright and nonblank in the screenshot.
- The frame did not include a known pulse target, so this does not complete AE
  visual magnification validation. A deliberate target/face run is still needed
  before marking AE complete.

## Supporting Slice: Scripted Debug Panel Capture

- Added a validation launch override for the expanded controls panel and exposed
  it through `tools/capture_live_validation_evidence.ps1 -Panel`.
- Pixel 8a smoke capture with `-Panel Debug` now launches directly into the
  diagnostics panel after the tabbed overlay split.
- Captured evidence bundle:
  `sample-videos/exports/live-validation/20260712-161755-expanded-debug-panel-launch`.
- The screenshot shows `Preview: Full-frame linear EVM preview`,
  `Renderer: Live linear EVM reconstruction`, `GL renderer: Live
  reconstruction`, and ready three-level pyramid diagnostics.
- This restores unattended renderer-path evidence capture for AE/AP validation,
  but it still does not complete known-target visual validation.

## Supporting Slice: Evidence Summary Tool

- Added `tools/summarize_live_validation_evidence.ps1` so live reconstruction
  captures can be summarized into `evidence_summary.json`.
- The summary includes launch state, screenshot dimensions, gfx frame pacing,
  and runtime crash/ANR/GL-error findings.
- Verified the tool on the Pixel 8a `expanded-debug-panel-launch` bundle. The
  runtime smoke passed with no crash, ANR, or GL-error findings.
- This improves evidence review consistency for AE validation, but known-target
  visual validation remains open.

## Supporting Slice: Screenshot Content Summary

- `evidence_summary.json` now includes sampled screenshot content metrics:
  mean luminance, luminance standard deviation, dark/light pixel fractions,
  `nonBlank`, and portrait orientation.
- Summaries warn when the screenshot appears blank/near-uniform or is not
  portrait-oriented.
- This strengthens unattended AE/AP smoke evidence by proving basic screenshot
  content health before human target review. It still does not prove visual
  magnification without a known target.

## Supporting Slice: Thermal Full-Frame Fallback

- Live full-frame preview now disables reconstruction when Android thermal
  state reaches `severe`, `critical`, `emergency`, or `shutdown`.
- If `ROI Source` is `Full frame` in that state, the app switches back to
  `Auto ROI` instead of continuing an expensive full-frame analysis path that
  can look frozen under thermal throttling.
- Moderate thermal state still appears as `Thermal high`; severe or worse
  forces the full-frame fallback.
- This protects AE/AP smoke runs from hot-device false negatives, but known
  target visual validation remains open.

## Supporting Slice: Camera Cadence Full-Frame Fallback

- The full-frame ROI fallback now also observes settled GL camera cadence.
- If full-frame mode has at least ten GL camera-frame samples and cadence falls
  below the live threshold, the app switches back to Auto ROI even when CPU
  analysis FPS still looks healthy.
- This targets the observed failure mode where full-frame preview appears frozen
  or extremely low-FPS before the analysis fallback alone can react.
- Startup cadence is still ignored until the GL camera statistics settle, so
  normal preview warmup should not force an unnecessary ROI change.

## Evidence

- `docs/experiments/live_full_frame_preview_bridge.md`
- `docs/experiments/pixel8a_live_linear_validation.md`
- `docs/testing/ROI_DEVICE_VALIDATION.md`

## Done When

- Live preview can show full-frame reconstructed EVM output for at least one mode.
- Split view compares raw vs reconstructed output, not raw vs ROI tint.
- Pixel portrait validation follows the watched live linear protocol and closes
  only with `-RequireFinalVisualEvidence` from a clean, target-visible run.
- Relevant tests/device checks pass, and the task is committed and pushed to `main`.
