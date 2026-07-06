# Milestone M - ROI Coordinate Mapping

Goal: fix the trust-breaking mismatch where automatic ROI can be drawn away from the visible face in portrait/front-camera preview.

## Tasks

- [x] Identify the coordinate spaces for analysis frames, CameraX preview, GL preview, rotation, mirroring, and aspect-fill crop.
- [x] Add a tested mapper from analysis-normalized ROI to preview-normalized ROI.
- [x] Use the mapper for both automatic ROI drawing and manual ROI input.
- [ ] Verify portrait/front-camera behavior on device.
- [x] Document the coordinate-space assumptions and any remaining limitations.

## Completed Slice: Preview Mapper Foundation

- Added `PreviewRoiMapper` for analysis-to-preview ROI mapping.
- Added frame width, frame height, and rotation metadata to `AnalysisSample`.
- `PulseRoiAnalyzer` now populates frame geometry from `ImageProxy`.
- The automatic ROI overlay now maps analysis ROI through rotation, front-camera mirroring, and aspect-fill preview geometry before drawing.
- Added JVM coverage for identity mapping, horizontal mirroring, 90-degree rotation, and portrait aspect-fill crop.

## Remaining Work

- Verify the front-camera mirror assumption on the Pixel 8a with a face or known target in frame.
- Decide whether GL preview and CameraX preview need separate mapping flags.

## Completed Slice: Manual ROI Reverse Mapping

- Added `PreviewRoiMapper.mapPreviewToAnalysis`.
- Manual ROI drag input now converts preview-space rectangles back to analysis-space rectangles before rebinding the analyzer.
- Manual ROI drawing maps the stored analysis ROI back through the same preview transform used by automatic ROI drawing.
- Added round-trip tests for preview-to-analysis mapping with rotation, mirroring, and aspect-fill crop.
- Installed and launched the updated debug APK on the connected Pixel as a smoke test; visual face/target alignment verification remains open.

## Completed Slice: Tint Overlay Mapping

- Captured a non-committed Pixel screenshot for verification.
- The screenshot did not include a face or known target, so it could not prove face alignment.
- It did reveal that the amplified tint rectangle was still using unmapped analysis ROI coordinates while the outline used mapped preview coordinates.
- Updated `AmplifiedTintOverlay` to draw the tint through the same `PreviewRoiMapper.mapAnalysisToPreview` transform as the ROI outline.
- Reinstalled the updated APK and captured a second non-committed Pixel screenshot; the tint and outline now overlap in portrait preview.

## Coordinate Assumptions

- `AnalysisSample.roi` is normalized to the `ImageProxy` analysis buffer.
- `AnalysisSample.rotationDegrees` is the CameraX rotation needed to orient analysis coordinates for display.
- The front camera is mirrored horizontally for the user-facing preview.
- The preview uses aspect-fill behavior, so mapped ROI coordinates can be affected by horizontal or vertical crop.
- GL preview and CameraX preview currently share the same mapper flags; this still needs device verification.

## Done When

- Auto ROI is drawn over the same face/skin region being analyzed.
- Manual ROI maps to the intended analysis region.
- Unit tests cover rotation, mirroring, and aspect-fill/crop cases.
- Device verification notes are updated, committed, and pushed to `main`.
