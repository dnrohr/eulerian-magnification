# Milestone AS - Processed Live Recording Parity

Status: Planned

Importance: High. True feature parity needs the exported MP4 to match the processed preview, not raw camera video or UI-only overlays.

Goal: ensure live preview output and recorded output share the same reconstructed renderer path and export enough metadata to reproduce a run.

## Tasks

- [ ] Route live reconstructed frames to the encoder surface for supported renderer paths.
- [ ] Add a clean-output recording option that excludes debug UI, boxes, and guides.
- [ ] Add an annotated-output option for evidence capture when validation overlays are useful.
- [ ] Record metadata for renderer path, fallback state, pyramid settings, temporal warmup, dropped frames, and quality diagnostics.
- [ ] Verify that recorded frames match preview mode for Amplified, Difference, and Split views.
- [ ] Add a device smoke test that records a short processed clip and validates that the file is playable and nonblank.

## Done When

- The app can export a processed MP4 that visually matches the reconstructed live preview.
- Evidence exports make it clear whether an output came from linear EVM, phase EVM, fallback color bridge, or signal visualization.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.
