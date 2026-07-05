# Milestone O - Recorded Export Pipeline

Goal: process a selected video into a shareable output video so algorithm results can be inspected independent of live camera behavior.

## Tasks

- [x] Define a recorded-video processing API that can produce frames or an MP4 output.
- [x] Start with the CPU color magnification path so it matches current live capability.
- [x] Include side-by-side or difference output options for diagnostics.
- [x] Save output and metadata in app storage.
- [x] Add validator coverage for generated files where possible.
- [x] Document how the app-generated output differs from Python/offline diagnostic renders.

## Completed Slice: CPU Processed Frames

- Added `RecordedVideoProcessor`.
- The processor consumes decoded `RgbFrame` inputs and emits processed `RgbFrame` outputs plus the `AnalysisSample` for each frame.
- `Raw`, `Amplified`, `Difference`, and `Split` view modes are supported.
- The CPU color math follows the current live color-amplification behavior: ROI-only RGB deltas from the bandpassed green signal.
- Added JVM tests for raw passthrough, ROI amplification, difference output, and side-by-side split output.

## Remaining Work

- None for the current CPU export path. Future work can improve progress UI and add richer metadata samples.

## Completed Slice: MP4 Exporter

- Added `RecordedVideoMp4Exporter` to encode processed recorded-video frames to H.264 MP4 with `MediaCodec` and `MediaMuxer`.
- The exporter accepts `RecordedVideoProcessedFrame` outputs from `RecordedVideoProcessor` and writes the MP4 to the requested app-accessible file.
- Added `RecordedVideoMp4ExporterInstrumentedTest`, which processes synthetic frames, exports an MP4 in app cache, and validates the result with `EncodedOutputValidator`.
- Verified on the connected Pixel with `connectedDebugAndroidTest` for the exporter test.

## Remaining Work After Exporter

- Improve progress UI for long videos.
- Add richer per-frame/sample metadata for recorded-video exports if needed.

## Completed Slice: UI Export Integration

- `Process Video` now decodes the selected video once, computes the processing summary, runs `RecordedVideoProcessor`, exports `debug_processed.mp4`, and writes `metadata.json`.
- Recorded-video exports use the same `recordings/processed-*` folder shape as live debug recordings.
- Recent recording rows now expose separate `Metadata` and `Video` share buttons when a processed MP4 exists.

## Done When

- The app can process a selected video and save a user-inspectable output artifact.
- The output path has metadata and validation coverage.
- Docs are updated, tests pass, and the task is committed and pushed to `main`.
