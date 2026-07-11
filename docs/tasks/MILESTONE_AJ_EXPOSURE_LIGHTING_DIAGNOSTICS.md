# Milestone AJ - Exposure And Lighting Diagnostics

Status: Complete

Importance: Medium. Lighting and exposure pumping can dominate pulse/color results.

Goal: make lighting stability visible and actionable.

## Tasks

- [x] Expose a compact lighting-stability status derived from existing flicker/green history.
- [x] Distinguish likely lighting flicker from subject/camera motion where possible.
- [x] Add guidance to wait, move lighting, or lock AE/AWB.
- [x] Include lighting status in video metadata or timeline where useful.
- [x] Add tests for lighting diagnostic thresholds.
- [x] Update README/troubleshooting docs.

## Notes

- Added `LightingStabilityAnalyzer`, which reports settling, stable, too dark,
  likely flicker, exposure pumping, or lighting mixed with ROI motion.
- Expanded controls now show the lighting diagnostic and action, while compact
  quality warnings include `Exposure unstable` when brightness variation looks
  like exposure pumping.
- Live recording metadata now includes lighting status code, label, action,
  average green, and variation when a diagnostic is available.
- Phone validation was not run because the device is unavailable today; this is
  a local heuristic and metadata slice.

## Done When

- Users can tell when lighting conditions are undermining the result.
- The guidance suggests concrete setup changes.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
