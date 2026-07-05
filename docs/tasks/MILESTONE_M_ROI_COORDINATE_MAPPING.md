# Milestone M - ROI Coordinate Mapping

Goal: fix the trust-breaking mismatch where automatic ROI can be drawn away from the visible face in portrait/front-camera preview.

## Tasks

- [x] Identify the coordinate spaces for analysis frames, CameraX preview, GL preview, rotation, mirroring, and aspect-fill crop.
- [x] Add a tested mapper from analysis-normalized ROI to preview-normalized ROI.
- [ ] Use the mapper for both automatic ROI drawing and manual ROI input.
- [ ] Verify portrait/front-camera behavior on device.
- [ ] Document the coordinate-space assumptions and any remaining limitations.

## Completed Slice: Preview Mapper Foundation

- Added `PreviewRoiMapper` for analysis-to-preview ROI mapping.
- Added frame width, frame height, and rotation metadata to `AnalysisSample`.
- `PulseRoiAnalyzer` now populates frame geometry from `ImageProxy`.
- The automatic ROI overlay now maps analysis ROI through rotation, front-camera mirroring, and aspect-fill preview geometry before drawing.
- Added JVM coverage for identity mapping, horizontal mirroring, 90-degree rotation, and portrait aspect-fill crop.

## Remaining Work

- Add reverse mapping for manual ROI input so touch-selected preview regions map back into analysis coordinates.
- Verify the front-camera mirror assumption on the Pixel 8a with screenshots.
- Decide whether GL preview and CameraX preview need separate mapping flags.

## Done When

- Auto ROI is drawn over the same face/skin region being analyzed.
- Manual ROI maps to the intended analysis region.
- Unit tests cover rotation, mirroring, and aspect-fill/crop cases.
- Device verification notes are updated, committed, and pushed to `main`.
