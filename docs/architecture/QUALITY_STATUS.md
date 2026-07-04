# Quality Status Overlay

## Current Slice

The app now evaluates basic capture quality from each analysis sample and displays a compact status row in the live overlay.

Current statuses:

- `Good`
- `Face missing`
- `Too dark`
- `Low FPS`
- `Timing unstable`
- `Signal weak`

The evaluator is intentionally conservative. It does not decide whether the visualization is medically meaningful; it only flags capture and timing conditions that commonly produce poor Eulerian magnification output.

## Artifact Suppression

`ArtifactSuppressor` applies two guardrails before a bandpassed signal drives visual amplification:

- signals below the configured noise floor are suppressed to zero
- amplified signals are clamped to a maximum magnitude

The live ROI tint and debug MP4 renderer both use this same suppressor, so preview and recorded debug output share the same basic artifact-control behavior.

## Next Work

- Add AE/AWB lock state once camera controls are exposed.
- Add thermal throttling status from the power service.
- Add lighting flicker detection.
- Add more robust signal-quality metrics over a rolling window.
