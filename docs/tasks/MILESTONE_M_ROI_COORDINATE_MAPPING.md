# Milestone M - ROI Coordinate Mapping

Goal: fix the trust-breaking mismatch where automatic ROI can be drawn away from the visible face in portrait/front-camera preview.

## Tasks

- [ ] Identify the coordinate spaces for analysis frames, CameraX preview, GL preview, rotation, mirroring, and aspect-fill crop.
- [ ] Add a tested mapper from analysis-normalized ROI to preview-normalized ROI.
- [ ] Use the mapper for both automatic ROI drawing and manual ROI input.
- [ ] Verify portrait/front-camera behavior on device.
- [ ] Document the coordinate-space assumptions and any remaining limitations.

## Done When

- Auto ROI is drawn over the same face/skin region being analyzed.
- Manual ROI maps to the intended analysis region.
- Unit tests cover rotation, mirroring, and aspect-fill/crop cases.
- Device verification notes are updated, committed, and pushed to `main`.
