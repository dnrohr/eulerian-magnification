# Milestone C: Recording Prototype

Goal: record processed output, not the raw camera feed.

## Tasks

- [x] Add start/stop recording controls.
- [ ] Encode composited processed frames with monotonic timestamps.
- [ ] Save MP4 to app-specific media storage.
- [x] Save sidecar metadata JSON with FPS, resolution, algorithm settings, ROI, dropped frames, and thermal state.
- [x] Add recording indicator and elapsed time.
- [ ] Add export/share affordance.
- [ ] Add encoder output validity checks.
- [x] Document recording architecture.
- [ ] Commit and push to `main`.

## Completed Slice: Metadata Recording Prototype

- Added start/stop recording controls to the overlay.
- Added elapsed recording indicator.
- Added app-specific metadata JSON output under `recordings/processed-*/metadata.json`.
- Captures mode, band, amplification, thermal state, sample count, dropped-frame estimate, ROI, FPS, latency, average green, and bandpassed signal.
- Added unit tests for metadata writing and dropped-frame estimate.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Success Criteria

- Recorded MP4 visually matches preview.
- Preview does not stutter substantially when recording starts.
- Audio is explicitly optional for the first version.
