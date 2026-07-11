# Live Phase Motion Scope

Date: 2026-07-11

Milestone: AR - Live Phase Motion EVM

## Decision

The first live phase/Riesz path should be a manually selected, reduced-resolution
ROI, not full-frame phase processing.

## Why ROI First

- Phase motion magnification is most useful for a known moving edge or object,
  and the app already has manual ROI selection for controlled targets.
- ROI processing gives the renderer a smaller texture budget, which is safer for
  Pixel 8a latency and thermal behavior than full-frame phase state.
- A manual ROI lets quality diagnostics reject low-amplitude or flat regions
  before the renderer spends work on phase updates.
- Difference and Split views can compare raw full-frame context against a
  phase-reconstructed ROI, which is easier to validate than a whole-frame phase
  pass that may amplify background noise.

## Initial Constraints

- Use GL preview only.
- Start with Motion mode, then consider Breathing only if the phase path proves
  stable.
- Process a reduced ROI texture, targeting roughly `320x240` or smaller after
  aspect-preserving downsample.
- Keep full-frame raw camera context visible; composite reconstructed phase
  output only inside the selected ROI for Amplified, Difference, and Split.
- Maintain per-pixel phase temporal state in ROI texture space, with warmup that
  emits raw/zero-difference output until history is initialized.
- Gate amplification by amplitude/confidence so flat regions do not turn into
  noise.
- Fall back to the existing ROI signal preview when GL phase resources are
  unavailable, timing is unhealthy, or no manual ROI exists.

## Validation Order

1. Synthetic moving-edge JVM/contract tests for phase wrapping, amplitude gating,
   warmup, and ROI texture sizing.
2. Recorded `local-euler` / moving-edge samples through an offline or harnessed
   phase path before live camera claims.
3. Pixel 8a live controlled object-motion setup after the app can report phase
   warmup, ROI texture size, amplitude gate status, and fallback reason.

## Deferred

- Full-frame live phase reconstruction.
- High-speed camera modes.
- Automatic feature/edge ROI discovery.
- Phase denoising beyond amplitude gating.
