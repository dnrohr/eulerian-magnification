# Milestone H: General EVM

Goal: implement simple linear Eulerian magnification with a Laplacian pyramid.

## Tasks

- [ ] Add full-frame downsampled pyramid.
- [ ] Add per-level temporal bandpass filters.
- [ ] Amplify selected levels and reconstruct frame.
- [ ] Add presets for pulse, breathing, tremor, and object vibration.
- [ ] Warn for invalid amplification, frequency, and camera-motion combinations.
- [ ] Add controlled synthetic video tests.
- [ ] Commit and push to `main`.

## Completed Slice: Recorded-Frame Synthetic Analysis Fixture

- Added `RecordedVideoAnalyzer` for timestamped RGB-frame inputs with a fixed normalized ROI.
- The analyzer reuses `AnalysisSettings`, `BandpassFilter`, `FpsMeter`, and `TimestampTracker` from the live path.
- Added synthetic recorded-clip tests for fixed ROI green extraction, pulse-band energy, and non-monotonic timestamps.
- Added `RecordedVideoAnalysisRunner` to aggregate recorded-frame reports with FPS, green-channel, bandpassed-energy, max-magnitude, and timestamp-monotonicity metrics.
- Added `RecordedVideoFrameDecoder` to sample local Android-readable video files into timestamped `RgbFrame` inputs.
- Documented the recorded-video validation workflow and public sample plan in `docs/testing/RECORDED_VIDEO_VALIDATION.md`.

This is recorded-frame analysis and decode plumbing only. Public sample execution and full-frame EVM reconstruction tests are still pending, so the broader controlled-video checklist item remains open.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.RecordedVideoAnalyzerTest"`
- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.RecordedVideoAnalysisRunnerTest"`
- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.RecordedVideoDecodePlanTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Success Criteria

- Small periodic object motion is visible in controlled tests.
- Processed output can be recorded.
