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

## Completed Slice: Live Phase Temporal Shader Contract

- Added `LIVE_PHASE_TEMPORAL_FRAGMENT` to `RieszPhaseShaderSource`.
- The shader contract ports the recorded phase-state update model into a
  live-capable GLSL pass: wrapped phase delta, unwrapped phase accumulation,
  temporal low/high state, high-minus-low bandpass phase, warmup seeding, and
  amplitude-gated amplification.
- Added shader-source coverage for the live temporal state inputs, warmup path,
  wrapped phase update, temporal bandpass, and amplitude gate.
- This is still a contract slice; runtime render-target allocation and
  invocation remain open.
- Installed the debug build on the Pixel 8a after focused shader coverage and
  full JVM/build verification.

## Completed Slice: Live Phase Diagnostics Contract

- Added `LivePhaseDiagnostics` for the future live phase UI/debug metadata.
- Diagnostics summarize whether phase rendering was requested, whether it is
  active, warmup status, ROI processing size, amplitude gate status, and fallback
  reason.
- Added explicit fallback reasons for missing manual ROI, unsupported GL phase
  resources, memory-budget overflow, unhealthy timing, low amplitude, and
  renderer errors.
- Added JVM coverage for inactive, fallback, warming, ready, and low-amplitude
  summaries.
- This is a diagnostics contract; wiring it into `CameraOesRenderer` and the UI
  remains open.
- Installed the debug build on the Pixel 8a after focused diagnostics coverage
  and full JVM/build verification.
