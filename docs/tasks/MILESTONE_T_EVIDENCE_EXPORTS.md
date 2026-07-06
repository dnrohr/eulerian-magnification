# Milestone T - Evidence Exports

Goal: make sample/video validation produce artifacts that clearly show what the app generated.

## Tasks

- [x] Export a side-by-side or split processed MP4 for selected videos.
- [x] Include source name, selected mode, view mode, band, amplification, frame count, and quality summary in metadata.
- [x] Add an optional signal/quality timeline artifact for processed videos.
- [x] Make recent exports easy to share from the app.
- [x] Add tests for export metadata and frame-processing consistency.

## Completed Slice

- The selected-video flow exports `debug_processed.mp4` using the active view mode, including Split when selected.
- Metadata now includes `timelinePath`, `processedFrameCount`, and `qualitySummary`.
- Each processed video also writes `signal_timeline.csv` with frame index, timestamp, FPS, average green, and bandpassed green.
- Recent exports remain shareable through the existing metadata/video buttons.
- Added a JVM test for evidence timeline formatting.

## Done When

- A selected sample video produces inspectable media plus metadata without relying on live phone observation.
- The exported artifact makes Raw vs processed behavior clear.
- Tests and docs are committed and pushed to `main`.
