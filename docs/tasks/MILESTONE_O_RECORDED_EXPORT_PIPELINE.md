# Milestone O - Recorded Export Pipeline

Goal: process a selected video into a shareable output video so algorithm results can be inspected independent of live camera behavior.

## Tasks

- [x] Define a recorded-video processing API that can produce frames or an MP4 output.
- [x] Start with the CPU color magnification path so it matches current live capability.
- [x] Include side-by-side or difference output options for diagnostics.
- [ ] Save output and metadata in app storage.
- [x] Add validator coverage for generated files where possible.
- [x] Document how the app-generated output differs from Python/offline diagnostic renders.

## Completed Slice: CPU Processed Frames

- Added `RecordedVideoProcessor`.
- The processor consumes decoded `RgbFrame` inputs and emits processed `RgbFrame` outputs plus the `AnalysisSample` for each frame.
- `Raw`, `Amplified`, `Difference`, and `Split` view modes are supported.
- The CPU color math follows the current live color-amplification behavior: ROI-only RGB deltas from the bandpassed green signal.
- Added JVM tests for raw passthrough, ROI amplification, difference output, and side-by-side split output.

## Remaining Work

- Encode processed frames to MP4 on Android.
- Save processed output and metadata in app storage.
- Add output-file validation around the generated MP4.
- Wire `Process Video` to produce a user-inspectable artifact rather than only a metrics summary.

## Done When

- The app can process a selected video and save a user-inspectable output artifact.
- The output path has metadata and validation coverage.
- Docs are updated, tests pass, and the task is committed and pushed to `main`.
