# Live Linear EVM Level Policy

Date: 2026-07-11

Milestone: AP - True Live Linear EVM

## Summary

The live reconstruction shader now applies per-level gain attenuation and clamps
the amplified reconstruction delta before final RGB clamping.

## Policy

| Level | Gain | Intent |
| --- | ---: | --- |
| 0 | 0.35 | Suppress fine-level shimmer and edge halos. |
| 1 | 0.75 | Keep mid-level motion/color contribution visible. |
| 2 | 1.00 | Preserve coarser low-frequency reconstruction. |

The amplified delta is clamped to `+/-0.18` in normalized RGB space before
being added to the base frame.

## Validation

Local JVM coverage verifies policy defaults, invalid policy rejection, and shader
uniform/source contracts.

Phone validation was not run for this slice because the phone is currently
unavailable. The next Pixel run should verify:

- GL debug reports `Live reconstruction`.
- Pyramid diagnostics show active levels and warm temporal state.
- Amplified view does not flash or clip aggressively under the same target used
  before the clamp.
- Split view still shows visible reconstructed change rather than an ROI tint.
