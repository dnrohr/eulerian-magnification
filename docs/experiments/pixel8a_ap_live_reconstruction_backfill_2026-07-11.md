# Pixel 8a AP Live Reconstruction Backfill

Date: 2026-07-11

Device: Pixel 8a `47091JEKB05516`

Build: debug APK from `main` after AQ completion docs.

## Result

The app launched successfully and the camera preview was upright and nonblank.
After switching to Pulse and enabling GL preview, expanded controls reported:

- `Preview: Full-frame linear EVM preview`
- `Renderer: Live linear EVM reconstruction`
- `GL renderer: Live reconstruction`
- `Pyramid: 3 levels / 540x1200 / ready`
- `temporal 3L / band 0.70-3.00Hz`
- `start L0 / gains 0.35/0.75/1.00 / clamp +/-0.18`

Evidence screenshot:

- `docs/experiments/pixel8a_ap_gl_pulse_2026-07-11.png`

## Interpretation

This backfills Pixel evidence that the live GL Pulse path can enter the true
linear reconstruction renderer on-device and report active pyramid, temporal,
gain, and clamp diagnostics. It also confirms the preview is upright and
nonblank in portrait orientation.

This does not complete AP validation. The camera was pointed at an incidental
static indoor scene, not a controlled motion or skin-color target, and CPU
analysis reported low FPS / weak signal. A deliberate known target is still
needed before claiming visible live magnification quality.
