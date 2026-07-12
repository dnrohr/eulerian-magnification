# Milestone AS - Processed Live Recording Parity

Status: Planned

Importance: High. True feature parity needs the exported MP4 to match the processed preview, not raw camera video or UI-only overlays.

Goal: ensure live preview output and recorded output share the same reconstructed renderer path and export enough metadata to reproduce a run.

## Tasks

- [ ] Route live reconstructed frames to the encoder surface for supported renderer paths.
- [ ] Add a clean-output recording option that excludes debug UI, boxes, and guides.
- [ ] Add an annotated-output option for evidence capture when validation overlays are useful.
- [x] Record metadata for renderer path, fallback state, pyramid settings, temporal warmup, dropped frames, and quality diagnostics.
- [ ] Verify that recorded frames match preview mode for Amplified, Difference, and Split views.
- [ ] Add a device smoke test that records a short processed clip and validates that the file is playable and nonblank.

## Completed Slice: Renderer Diagnostics Metadata

- Live recording metadata now includes the active preview path, GL render path,
  GL FPS/frame timing, reconstruction diagnostic summary/fallback, and live phase
  diagnostic summary/fallback when recording stops.
- The GL preview path already forwards emitted `ProcessedGlFrame` textures to
  `GlProcessedMp4Recorder`; this slice makes the exported sidecar explain which
  renderer/fallback state produced those frames.
- Added JVM coverage for renderer diagnostics in `metadata.json`.

## Done When

- The app can export a processed MP4 that visually matches the reconstructed live preview.
- Evidence exports make it clear whether an output came from linear EVM, phase EVM, fallback color bridge, or signal visualization.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.
