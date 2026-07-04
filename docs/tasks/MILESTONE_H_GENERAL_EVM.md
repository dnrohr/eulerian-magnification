# Milestone H: General EVM

Goal: implement simple linear Eulerian magnification with a Laplacian pyramid.

## Tasks

- [ ] Add full-frame downsampled pyramid.
- [ ] Add per-level temporal bandpass filters.
- [ ] Amplify selected levels and reconstruct frame.
- [x] Add presets for pulse, breathing, tremor, and object vibration.
- [x] Warn for invalid amplification, frequency, and camera-motion combinations.
- [ ] Add controlled synthetic video tests.
- [ ] Commit and push to `main`.

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
