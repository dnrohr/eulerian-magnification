# Milestone AF - Phase Motion Renderer Integration

Status: Complete

Importance: High. Phase/Riesz magnification is the better route for subtle motion and fewer linear-EVM artifacts.

Goal: promote the existing Riesz/phase reference work into an app renderer path.

## Tasks

- [x] Decide whether phase/Riesz starts as recorded-only or live-preview capable.
- [x] Convert the existing shader-source foundation into an executable render pass.
- [x] Track local phase/amplitude over time with temporal filtering.
- [x] Amplify wrapped phase deltas with amplitude gating.
- [x] Reconstruct output frames for recorded motion exports.
- [x] Validate against synthetic motion samples and compare with linear EVM behavior.
- [x] Add tests for phase state, wrapping, amplitude gating, and reconstruction behavior.
- [x] Document when to use phase motion versus linear EVM.

## Implementation Notes

- Phase/Riesz starts as a recorded-only CPU renderer because it can be validated
  locally without phone access.
- Added `RieszPhaseMotionRenderer` for luminance/Riesz projection, wrapped phase
  tracking, temporal filtering, phase amplification, and luminance
  reconstruction.
- `RecordedVideoProcessor` now routes non-Pulse `Amplified` and `Split` exports
  through the phase-motion renderer. Pulse remains on full-frame linear EVM.
- Tests verify stationary stability, in-band motion response, out-of-band
  rejection, lower clipping than linear EVM on a high-contrast translating edge,
  and processor routing for Fast Motion.
- Live GL phase rendering remains future work; no phone validation was run
  because the device is unavailable today.

## Evidence

- `docs/experiments/recorded_riesz_phase_motion_renderer.md`

## Done When

- The app has a real phase/Riesz motion output path beyond shader source scaffolding.
- Validation shows it improves at least one subtle-motion sample over linear EVM.
- Relevant tests/build/device checks pass, and the task is committed and pushed to `main`.
