# Milestone R - ROI Tracking Stability

Goal: make ROI behavior predictable enough that users can trust what region is being analyzed.

## Tasks

- [ ] Keep manual ROI fixed unless the user explicitly drags a new region or clears it.
- [ ] Freeze the last good automatic ROI when tracking confidence drops instead of allowing visible wandering.
- [ ] Add a tracking-state label such as `Auto ROI`, `Tracking`, `Frozen`, or `Manual ROI`.
- [ ] Add unit tests for ROI smoothing/freeze behavior.
- [ ] Update README troubleshooting notes for ROI drift and manual selection.

## Done When

- Only one ROI is visible in normal use.
- Manual ROI remains stable.
- Automatic ROI does not jump erratically when the detector/tracker loses confidence.
- Tests and docs are committed and pushed to `main`.
