# Milestone H: General EVM

Goal: implement simple linear Eulerian magnification with a Laplacian pyramid.

## Tasks

- [x] Add full-frame downsampled pyramid.
- [x] Add per-level temporal bandpass filters.
- [x] Amplify selected levels and reconstruct frame.
- [x] Add presets for pulse, breathing, tremor, and object vibration.
- [x] Warn for invalid amplification, frequency, and camera-motion combinations.
- [x] Add controlled synthetic video tests.
- [ ] Commit and push to `main`.

## Completed Slice: Controlled Synthetic EVM Tests

- Added end-to-end CPU reference EVM tests over synthetic video-like frame sequences.
- The tests build full-frame pyramids, apply per-level temporal bandpass filtering, reconstruct amplified output, and measure processed green-channel variation.
- Pulse-band tests verify in-band variation is amplified more than slow drift.
- Zero-amplification tests verify reconstruction can preserve raw variation.
- Breathing-mode tests verify the breathing band favors 0.25 Hz motion over pulse-band motion.

These tests prove the CPU/reference EVM path on controlled synthetic color variation. Real recorded samples and device/GPU integration remain separate validation steps.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.ControlledSyntheticEvmTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: High-Frequency Mode Warnings

- Quality evaluation now receives the active analysis settings.
- Tremor and Object Vibration modes warn on subtle camera motion before the general camera-motion threshold.
- Tremor and Object Vibration modes warn when amplification is above 18x.
- Warnings show through the existing quality overlay as `Mode motion risk` and `Amplification risk`.
- Added tests for high-frequency motion risk, pulse-mode non-triggering, and high-amplification risk.

These warnings are conservative guardrails. They do not replace measured device validation.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.quality.QualityEvaluatorTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: CPU Reference Downsample Pyramid

- Added `RgbFramePyramidBuilder` for full-frame RGB downsample pyramids.
- The builder preserves frame timestamps across levels.
- Downsampling uses deterministic 2x2 RGB channel averaging with clamping at one-pixel dimensions.
- Added tests for requested level counts, pixel averaging, odd dimensions, timestamp preservation, and invalid level counts.

This is a CPU/reference pyramid. GPU pyramid allocation already exists in Milestone E, but temporal filtering and reconstruction remain pending here.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.RgbFramePyramidBuilderTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: CPU Reference Pyramid Temporal Bandpass

- Added `RgbPyramidTemporalBandpass` for per-level RGB temporal filtering.
- The filter keeps independent low/high-pass state for every RGB channel at every pyramid level.
- First frames and pyramid-shape changes reset state and emit zero bandpass output.
- Bandpass output preserves each level's dimensions and timestamp.
- Added tests for first-frame reset, level sizing, shape reset, and stronger pulse-band response than slow drift.

This produces filtered pyramid levels only. Amplification and frame reconstruction remain pending.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.RgbPyramidTemporalBandpassTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: CPU Reference Pyramid Reconstruction

- Added `RgbPyramidReconstructor` to amplify filtered pyramid levels and add them back to the base RGB frame.
- Coarser filtered levels are nearest-neighbor upsampled to base-frame coordinates.
- Reconstruction clamps output channels to displayable 0-255 values.
- Supports skipping fine levels with `startLevel`.
- Added tests for no-op amplification, amplified output, coarse-level upsampling, channel clamping, and start-level selection.

This is a CPU/reference reconstruction path. It is not yet connected to live preview, recording output, or GPU shaders.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.RgbPyramidReconstructorTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: General EVM Presets

- Added Tremor and Object Vibration modes alongside the existing Pulse and Breathing modes.
- Tremor uses a 4.0-12.0 Hz band.
- Object Vibration uses a 3.0-12.0 Hz band.
- Mode controls, metadata recording, debug MP4 labels, and recorded-video analysis all read the shared `MagnificationMode` settings.
- Added tests for preset band exposure and high-frequency bandpass behavior.

Invalid-combination warnings for high-frequency modes remain pending.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.AnalysisSettingsTest" --tests "com.dnrohr.eulerianmagnification.analysis.BandpassFilterTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Recorded-Frame Synthetic Analysis Fixture

- Added `RecordedVideoAnalyzer` for timestamped RGB-frame inputs with a fixed normalized ROI.
- The analyzer reuses `AnalysisSettings`, `BandpassFilter`, `FpsMeter`, and `TimestampTracker` from the live path.
- Added synthetic recorded-clip tests for fixed ROI green extraction, pulse-band energy, and non-monotonic timestamps.
- Added `RecordedVideoAnalysisRunner` to aggregate recorded-frame reports with FPS, green-channel, bandpassed-energy, max-magnitude, and timestamp-monotonicity metrics.
- Added `RecordedVideoFrameDecoder` to sample local Android-readable video files into timestamped `RgbFrame` inputs.
- Added `RecordedVideoValidator` to run local-video decode and report generation as one validation step.
- Added an in-app `Validate Video` picker that runs the recorded-video validator against a selected local video and displays summary metrics.
- Documented the recorded-video validation workflow and public sample plan in `docs/testing/RECORDED_VIDEO_VALIDATION.md`.

This is recorded-frame analysis and decode plumbing only. Public sample execution and full-frame EVM reconstruction tests are still pending, so the broader controlled-video checklist item remains open.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.RecordedVideoAnalyzerTest"`
- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.RecordedVideoAnalysisRunnerTest"`
- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.RecordedVideoDecodePlanTest"`
- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.RecordedVideoValidationTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Success Criteria

- Small periodic object motion is visible in controlled tests.
- Processed output can be recorded.
