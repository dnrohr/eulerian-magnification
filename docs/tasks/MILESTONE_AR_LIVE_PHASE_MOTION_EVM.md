# Milestone AR - Live Phase Motion EVM

Status: In progress

Importance: High. Linear EVM is useful, but phase/Riesz magnification is the likely path to convincing subtle motion with fewer blur artifacts.

Goal: bring the recorded Riesz/phase motion renderer into a live-preview path for controlled motion targets.

## Tasks

- [x] Decide the first live phase scope: reduced-resolution full frame or manually selected ROI.
- [ ] Port the recorded phase state update to a live-capable renderer path with bounded memory and latency.
- [ ] Add amplitude gating, phase wrapping, and temporal warmup diagnostics to the live UI/debug metadata.
- [ ] Add live Difference and Split views that compare raw video against phase-reconstructed output.
- [ ] Validate on synthetic moving-edge and `local-euler` samples before phone-camera validation.
- [ ] Validate on Pixel 8a with a controlled object-motion setup and document expected artifacts.

## Done When

- Live Motion mode visibly magnifies a controlled subtle movement target beyond what linear color/tint modes can show.
- The app can fall back gracefully when phase confidence is too low or the device cannot sustain the path.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.

## Completed Slice: Live Phase Scope Decision

- Chose a manually selected, reduced-resolution ROI as the first live phase
  path.
- Documented the decision, constraints, fallback expectations, and validation
  order in `docs/architecture/LIVE_PHASE_SCOPE.md`.
- Deferred full-frame live phase processing until the ROI path proves quality,
  latency, and thermal behavior on Pixel 8a.
- Re-ran the standard JVM/build verification and reinstalled the current debug
  build on the Pixel 8a.

## Completed Slice: Live Phase ROI Plan Contract

- Added `LivePhaseRoiPlan` for the first live phase renderer path.
- The plan computes source ROI pixel size, caps the processing texture to a
  bounded dimension, estimates RGBA16F render-target memory, and reports whether
  the plan fits the live phase budget.
- Added `LivePhaseRoiStatePlan` for the per-pixel phase state targets needed by
  the recorded phase update model: previous phase, unwrapped phase, lowpass,
  highpass, and bandpass.
- Added `LivePhaseWarmupStatus` labels for future live UI/debug diagnostics.
- Added JVM coverage for ROI sizing, aspect preservation, target counts, memory
  budget checks, invalid inputs, and warmup labels.
- This is the bounded live state contract for porting recorded phase state
  updates; the runtime GL renderer allocation and shader invocation remain the
  next AR slice.
- Installed the debug build on the Pixel 8a after focused and full
  JVM/build verification.
