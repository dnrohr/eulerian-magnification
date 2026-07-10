# Milestone AD - MIT Sample Validation

Status: Complete

Importance: Very high. We need known samples to prove the renderer behaves like EVM.

Goal: validate the recorded full-frame EVM renderer against MIT-style sample videos and artifacts.

## Tasks

- [x] Run the recorded full-frame EVM renderer on deterministic local parity samples.
- [x] Save non-sensitive output artifacts outside git or document their local paths.
- [x] Compare output against expected MIT-style behavior for pulse/color and small edge motion.
- [x] Tune validation amplification downward based on clipping metrics.
- [x] Add automated parity checks for recorded full-frame renderer behavior.
- [x] Document results, failures, and remaining gaps in `docs/experiments/`.

## Implementation Notes

- Added `RecordedEvmParityValidator`, which runs timestamped frame sequences
  through `FullFrameLinearEvmRenderer` and reports frame delta, changed-pixel,
  and clipping metrics.
- Added JVM tests for stationary, synthetic color pulse, synthetic translating
  edge, and failed-expectation reporting.
- Validation showed that high AC-era gains create obvious clipping on controlled
  samples, so sample-validation gains are now lower: `4.0` for pulse color and
  `0.5` for the high-contrast translating-edge motion sample.
- Re-ran the existing Riesz decoded MIT baby checks against
  `sample-videos/exports/mit-evm-baby-riesz-frames.json`.
- Fresh MP4 rendering from `sample-videos/euler.mp4` and
  `sample-videos/mit-evm-baby.mp4` still requires Android media decode/export or
  workstation media tooling. Phone testing was intentionally skipped because the
  device is unavailable today.

## Evidence

- `docs/experiments/recorded_full_frame_evm_parity_validation.md`

## Done When

- At least one color sample and one motion sample produce plausible MIT-style output.
- Remaining gaps are documented with concrete next steps.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
