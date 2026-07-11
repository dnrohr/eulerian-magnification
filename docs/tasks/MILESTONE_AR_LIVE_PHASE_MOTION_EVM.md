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
