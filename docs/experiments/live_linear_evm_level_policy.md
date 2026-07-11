# Live Linear EVM Level Policy

Date: 2026-07-11

Milestone: AP - True Live Linear EVM

## Summary

The live reconstruction shader now derives Laplacian-style spatial deltas from
the temporally filtered Gaussian bandpass pyramid, applies per-level gain
attenuation, and clamps the amplified reconstruction delta before final RGB
clamping.

## Policy

| Level | Gain | Intent |
| --- | ---: | --- |
| 0 | 0.35 | Suppress fine-level shimmer and edge halos. |
| 1 | 0.75 | Keep mid-level motion/color contribution visible. |
| 2 | 1.00 | Preserve coarser low-frequency reconstruction. |

The amplified delta is clamped to `+/-0.18` in normalized RGB space before
being added to the base frame.

## Reconstruction Delta

The live GL path stores temporally filtered Gaussian bandpass levels. The
reconstruction shader now computes the displayed delta as:

```text
level 0 delta = gaussian bandpass 0 - gaussian bandpass 1
level 1 delta = gaussian bandpass 1 - gaussian bandpass 2
level 2 delta = gaussian bandpass 2
```

This keeps the live path closer to a linear EVM Laplacian reconstruction than a
plain sum of blurred color changes. Each delta level is sampled in normalized
texture coordinates, so the coarser levels are upsampled by the texture sampler
before they are subtracted or added.

## Validation

Local JVM coverage verifies policy defaults, invalid policy rejection,
Laplacian reconstruction source contracts, and shader uniform/source contracts.

Phone validation was not run for this slice because the phone is currently
unavailable. The next Pixel run should verify:

- GL debug reports `Live reconstruction`.
- Pyramid diagnostics show active levels and warm temporal state.
- Amplified view does not flash or clip aggressively under the same target used
  before the clamp.
- Difference view shows full-frame reconstructed-delta heatmap rather than an
  ROI signal tint.
- Split view still shows visible reconstructed change rather than an ROI tint.
