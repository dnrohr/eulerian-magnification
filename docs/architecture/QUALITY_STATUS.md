# Quality Status Overlay

## Current Slice

The app now evaluates basic capture quality from each analysis sample and displays a compact status row in the live overlay.

Current statuses:

- `Good`: keep this setup.
- `Face missing`: frame the face or select a manual ROI.
- `Too dark`: use brighter, steady light.
- `Low FPS`: close apps or reduce device load.
- `Timing unstable`: restart the preview if timing keeps jumping.
- `Lighting flicker`: try daylight or a non-flickering lamp.
- `ROI motion`: mount the phone or redraw a stable ROI.
- `Mode motion risk`: use a tripod for high-frequency modes.
- `Amplification risk`: lower amplification below 18x.
- `Signal weak`: use steadier light or choose a stronger ROI.

The compact overlay shows only the status labels. Expanded controls show the
same labels plus these short actions, so the user can respond without losing the
mostly unobstructed preview.

The evaluator is intentionally conservative. It does not decide whether the visualization is medically meaningful; it only flags capture and timing conditions that commonly produce poor Eulerian magnification output.

Lighting flicker detection uses a rolling average-green heuristic. It looks for repeated, above-threshold alternation in brightness deltas, which catches obvious unstable LED/light-source behavior. It is not yet a 50/60 Hz frequency-domain detector.

ROI motion detection currently uses `TranslationEstimator`, which estimates normalized frame-to-frame movement from the smoothed/tracked ROI center. This can be caused by phone movement, head or torso movement, heartbeat-visible face motion, or detector/tracker drift. It is a practical warning signal for the CPU MVP, not a full global optical-flow compensation pass.

High-frequency modes are more sensitive to small camera motion and aggressive amplification. Fast Motion adds `Mode motion risk` when normalized translation reaches 0.008 and `Amplification risk` when amplification is above 18x. Pulse and Breathing do not use those high-frequency guardrails.

## Artifact Suppression

`ArtifactSuppressor` applies two guardrails before a bandpassed signal drives visual amplification:

- signals below the configured noise floor are suppressed to zero
- amplified signals are clamped to a maximum magnitude

The live ROI tint and debug MP4 renderer both use this same suppressor, so preview and recorded debug output share the same basic artifact-control behavior.

## Next Work

- Verify AE/AWB lock behavior on Pixel 8a under stable lighting.
- Add thermal throttling status from the power service.
- Replace the simple flicker heuristic with 50/60 Hz-aware frequency analysis when timestamp history is mature enough.
- Add more robust signal-quality metrics over a rolling window.
