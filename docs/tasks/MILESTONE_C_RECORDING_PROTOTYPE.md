# Milestone C: Recording Prototype

Goal: record processed output, not the raw camera feed.

## Tasks

- [ ] Add start/stop recording controls.
- [ ] Encode composited processed frames with monotonic timestamps.
- [ ] Save MP4 to app-specific media storage.
- [ ] Save sidecar metadata JSON with FPS, resolution, algorithm settings, ROI, dropped frames, and thermal state.
- [ ] Add recording indicator and elapsed time.
- [ ] Add export/share affordance.
- [ ] Add encoder output validity checks.
- [ ] Document recording architecture.
- [ ] Commit and push to `main`.

## Success Criteria

- Recorded MP4 visually matches preview.
- Preview does not stutter substantially when recording starts.
- Audio is explicitly optional for the first version.
