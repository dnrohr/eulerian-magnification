# Milestone AM - Gated Rate Estimates

Status: Complete

Importance: Low for now. Rate estimates are tempting but risky before signal validation is stronger.

Goal: only add heart/breath-rate estimates after evidence quality is strong enough to avoid misleading claims.

## Tasks

- [x] Define strict prerequisites for showing any rate estimate.
- [x] Implement estimates only as experimental/non-diagnostic values.
- [x] Gate display behind stable signal, timing, ROI, lighting, and motion checks.
- [x] Compare estimates against known synthetic/recorded references.
- [x] Add tests for estimator behavior and gating.
- [x] Update README warnings and validation docs.

## Notes

- Added `GatedRateEstimator` for experimental pulse and breathing estimates from
  timestamped bandpassed signals.
- Estimates remain hidden unless the gate passes: supported mode, enough frames,
  usable FPS, monotonic timestamps, ROI present, stable lighting, low motion,
  sufficient energy, and sufficient peak magnitude.
- Recorded-video summaries and metadata now include either an experimental
  non-diagnostic rate or the reason the rate is hidden.
- JVM tests cover clean synthetic pulse and breathing references plus each gate
  failure reason.
- Phone validation was not run because the device is unavailable today.

## Done When

- Rate estimates are hidden unless quality gates pass.
- Documentation clearly states that estimates are experimental and non-diagnostic.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
