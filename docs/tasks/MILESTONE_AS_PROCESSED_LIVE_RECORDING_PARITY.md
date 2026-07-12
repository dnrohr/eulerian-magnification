# Milestone AS - Processed Live Recording Parity

Status: In progress

Importance: High. True feature parity needs the exported MP4 to match the processed preview, not raw camera video or UI-only overlays.

Goal: ensure live preview output and recorded output share the same reconstructed renderer path and export enough metadata to reproduce a run.

## Tasks

- [ ] Route live reconstructed frames to the encoder surface for supported renderer paths.
- [x] Add a clean-output recording option that excludes debug UI, boxes, and guides.
- [x] Add an annotated-output option for evidence capture when validation overlays are useful.
- [x] Record metadata for renderer path, fallback state, pyramid settings, temporal warmup, dropped frames, and quality diagnostics.
- [ ] Verify that recorded frames match preview mode for Amplified, Difference, and Split views.
- [x] Add a device smoke test that records a short processed clip and validates that the file is playable and nonblank.

## Completed Slice: Renderer Diagnostics Metadata

- Live recording metadata now includes the active preview path, GL render path,
  GL FPS/frame timing, reconstruction diagnostic summary/fallback, and live phase
  diagnostic summary/fallback when recording stops.
- The GL preview path already forwards emitted `ProcessedGlFrame` textures to
  `GlProcessedMp4Recorder`; this slice makes the exported sidecar explain which
  renderer/fallback state produced those frames.
- Added JVM coverage for renderer diagnostics in `metadata.json`.

## Completed Slice: Clean vs Annotated Recording Mode

- Added a persisted recording output mode with `Clean` and `Annotated` choices in
  expanded controls.
- `Clean` records the processed GL preview texture without app controls when GL
  preview is active. If GL preview is not active, the recording output policy
  falls back to annotated evidence frames because CameraX clean processed export
  is not supported yet.
- `Annotated` records evidence frames with labels, ROI, signal, mode, FPS, and
  latency.
- Recording metadata now stores the requested output mode and actual output kind
  so exports can distinguish clean preview captures from annotated evidence.
- Added JVM coverage for output-mode persistence, output policy, and recording
  metadata.

## Completed Slice: Processed Session Device Smoke

- Added a Pixel/instrumented smoke test that creates a `ProcessedRecordingSession`,
  records five clean GL processed frames through `GlProcessedMp4Recorder`, stops
  the session, and validates the generated `debug_processed.mp4` with
  `EncodedOutputValidator`.
- The test also checks the MP4 is larger than a tiny placeholder and verifies
  `metadata.json` records `clean_preview`, the debug video path, and five
  analysis samples.
- Verified on connected Pixel 8a with:
  `.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.dnrohr.eulerianmagnification.recording.ProcessedRecordingSessionInstrumentedTest"`

## Done When

- The app can export a processed MP4 that visually matches the reconstructed live preview.
- Evidence exports make it clear whether an output came from linear EVM, phase EVM, fallback color bridge, or signal visualization.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.
