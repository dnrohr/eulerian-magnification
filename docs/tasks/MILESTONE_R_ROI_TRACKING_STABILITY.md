# Milestone R - ROI Tracking Stability

Goal: make ROI behavior predictable enough that users can trust what region is being analyzed.

## Tasks

- [x] Keep manual ROI fixed unless the user explicitly drags a new region or clears it.
- [x] Freeze the last good automatic ROI when tracking confidence drops instead of allowing visible wandering.
- [x] Add a tracking-state label such as `Auto ROI`, `Tracking`, `Frozen`, or `Manual ROI`.
- [x] Add unit tests for ROI smoothing/freeze behavior.
- [x] Update README troubleshooting notes for ROI drift and manual selection.

## Completed Slice

- Live auto ROI now holds the last detected region instead of predicting/extrapolating movement between face-detection passes.
- Empty detection results increment a missed-detection counter and show `Held ROI` while the app keeps using the last good region.
- After sustained missed detections, the app resets the tracker/smoother and falls back to `Center ROI`.
- Compact preview labels now show `Manual ROI`, `Tracking`, `Held ROI`, or `Center ROI`.
- Added tracker tests for freeze and reset behavior.

## Done When

- Only one ROI is visible in normal use.
- Manual ROI remains stable.
- Automatic ROI does not jump erratically when the detector/tracker loses confidence.
- Tests and docs are committed and pushed to `main`.
