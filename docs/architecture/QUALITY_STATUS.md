# Quality Status Overlay

## Current Slice

The app now evaluates basic capture quality from each analysis sample and displays a compact status row in the live overlay.

Current statuses:

- `Good`: keep this setup.
- `Face missing`: frame the face or select a manual ROI.
- `Too dark`: use brighter, steady light.
- `Thermal high`: let the phone cool before validation.
- `Low FPS`: close apps or reduce device load.
- `Full frame slow`: switch to Auto ROI for live preview.
- `Camera FPS low`: hide controls or use Auto ROI.
- `Camera frozen`: restart the preview or app before validating.
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

`Thermal high` is based on Android `PowerManager.currentThermalStatus` and
appears at `moderate` or worse. This mirrors the validation-summary thermal
warning threshold, but surfaces the issue while the user is still setting up the
shot instead of only after evidence export.
At `critical`, `emergency`, or `shutdown`, the live full-frame reconstruction
policy disables full-frame preview and the ROI-source fallback policy switches
`Full frame` back to `Auto ROI`. Runs in that state are not valid evidence for
full-frame FPS, apparent camera freeze, or visual parity.

`Low FPS` is based on CPU analysis samples. If the selected source is `Full
frame`, the same condition becomes `Full frame slow` so the user gets the
specific recovery action. After repeated low-FPS full-frame samples, the app
automatically switches ROI Source back to `Auto ROI`; full-frame analysis can
make the image look frozen even when the camera and GL renderer are still
delivering frames. `Camera FPS low` is based on GL camera frame-arrival cadence
from `SurfaceTexture` callbacks and is only shown after enough camera frames
have arrived to avoid startup-settling false warnings. This separates a slow
analysis path from a camera stream that is actually delivering frames too slowly
for full-frame live magnification.

`Camera frozen` is based on the age of the most recent GL `SurfaceTexture`
camera-frame callback. It can appear even when the UI and renderer are still
smooth, because the GL surface may keep drawing the last texture after the Pixel
camera stack stops delivering fresh frames. Treat this as a validation blocker:
restart the preview or app and confirm the live image is moving before accepting
setup or final evidence.
When `Camera frozen` appears in expanded controls, the `Restart Preview` button
forces the live camera preview to rebind without changing persisted settings.

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
the renderer. The live analyzer also estimates saturated-pixel fraction from the
same ROI sample grid used for green-channel analysis, so clipped ROI input can
dampen Pulse color amplification before it reaches GL.

## Color Output Clamp

Recorded full-frame reconstruction now runs through `ColorOutputClamp` before
frames are exported. The clamp limits per-channel deltas, limits YIQ chroma
shifts relative to the source pixel, keeps changed pixels away from hard display
rails, and dampens broad frame-wide color changes that look like exposure or
white-balance pulses rather than useful pulse-color magnification.

## Next Work

- Verify AE/AWB lock behavior on Pixel 8a under stable lighting.
- Replace the simple flicker heuristic with 50/60 Hz-aware frequency analysis when timestamp history is mature enough.
- Add more robust signal-quality metrics over a rolling window.
