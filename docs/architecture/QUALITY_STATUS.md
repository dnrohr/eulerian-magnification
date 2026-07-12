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
- `Exposure unstable`: wait for exposure to settle, then lock AE/AWB.
- `ROI motion`: mount the phone or redraw a stable ROI.
- `Mode motion risk`: use a tripod for high-frequency modes.
- `Amplification risk`: lower amplification below 18x.
- `Signal weak`: use steadier light or choose a stronger ROI.

The compact overlay shows only the status labels. Expanded controls show the
same labels plus these short actions, so the user can respond without losing the
mostly unobstructed preview.

The evaluator is intentionally conservative. It does not decide whether the visualization is medically meaningful; it only flags capture and timing conditions that commonly produce poor Eulerian magnification output.

Lighting diagnostics use a rolling average-green history. The app reports:

- `Lighting settling` while the history is too short to judge.
- `Lighting stable` when brightness variance is low.
- `Lighting too dark` when ROI brightness is below the low-light threshold.
- `Lighting flicker` when above-threshold brightness deltas alternate repeatedly.
- `Exposure unstable` when brightness varies without the alternation pattern.
- `Lighting mixed with ROI motion` when brightness instability coincides with
  enough ROI motion that the app should not blame lighting alone.

The expanded controls show the lighting label and action. Live recording
metadata stores the lighting status code, label, action, average green, and
variation when a diagnostic is available. The detector is still heuristic; it is
not yet a 50/60 Hz frequency-domain detector.

ROI motion detection currently uses `TranslationEstimator`, which estimates normalized frame-to-frame movement from the smoothed/tracked ROI center. This can be caused by phone movement, head or torso movement, heartbeat-visible face motion, or detector/tracker drift. It is a practical warning signal for the CPU MVP, not a full global optical-flow compensation pass.

High-frequency modes are more sensitive to small camera motion and aggressive amplification. Fast Motion adds `Mode motion risk` when normalized translation reaches 0.008 and `Amplification risk` when amplification is above 18x. Pulse and Breathing do not use those high-frequency guardrails.

## Artifact Suppression

`ArtifactSuppressor` applies two guardrails before a bandpassed signal drives visual amplification:

- signals below the configured noise floor are suppressed to zero
- amplified signals are clamped to a maximum magnitude

The live ROI tint and debug MP4 renderer both use this same suppressor, so preview and recorded debug output share the same basic artifact-control behavior.

## Color Amplification Gate

Recorded pulse processing now converts lighting diagnostics and ROI color
clipping into a color-amplification gate. Stable pulse input keeps full
amplification. Lighting settling, flicker, exposure pumping, motion-contaminated
lighting, darkness, and saturated/clipped ROI pixels reduce or disable color
amplification before the full-frame linear renderer reconstructs the frame.

The processed-frame timeline CSV includes the gate reason, gain, and saturated
pixel fraction so exports can explain when the app intentionally dampened color
output instead of letting unstable input flash the whole frame. This is the
recorded-side contract.

Live GL Pulse preview now applies the same lighting-based color gate to both
the ROI color bridge signal and the live linear reconstruction amplification.
Lighting settling, flicker, exposure pumping, motion-contaminated lighting, and
darkness reduce or disable live Pulse color amplification before uniforms reach
the renderer. Live saturation gating is not wired yet because the analyzer does
not currently expose a per-frame saturated-pixel fraction.

## Color Output Clamp

Recorded full-frame reconstruction now runs through `ColorOutputClamp` before
frames are exported. The clamp limits per-channel deltas, limits YIQ chroma
shifts relative to the source pixel, keeps changed pixels away from hard display
rails, and dampens broad frame-wide color changes that look like exposure or
white-balance pulses rather than useful pulse-color magnification.

## Next Work

- Verify AE/AWB lock behavior on Pixel 8a under stable lighting.
- Add thermal throttling status from the power service.
- Replace the simple flicker heuristic with 50/60 Hz-aware frequency analysis when timestamp history is mature enough.
- Add more robust signal-quality metrics over a rolling window.
