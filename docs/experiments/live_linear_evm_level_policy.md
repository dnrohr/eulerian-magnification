# Live Linear EVM Level Policy

Date: 2026-07-11

Milestone: AP - True Live Linear EVM

## Summary

The live reconstruction shader now derives Laplacian-style spatial deltas from
the temporally filtered Gaussian bandpass pyramid, applies per-level gain
attenuation, and clamps the amplified reconstruction delta before final RGB
clamping.

## Policy

### Pulse Color

| Level | Gain | Intent |
| --- | ---: | --- |
| 0 | 0.35 | Suppress fine-level shimmer and edge halos. |
| 1 | 0.75 | Keep mid-level motion/color contribution visible. |
| 2 | 1.00 | Preserve coarser low-frequency reconstruction. |

Pulse starts reconstruction at level `0` and clamps the amplified delta to
`+/-0.18` in normalized RGB space before adding it to the base frame.

### Breathing / Slow Motion

| Level | Gain | Intent |
| --- | ---: | --- |
| 0 | 0.00 | Skip fine texture shimmer for slow motion. |
| 1 | 0.85 | Preserve torso/shoulder-scale motion contribution. |
| 2 | 1.00 | Preserve coarse low-frequency reconstruction. |

Breathing starts reconstruction at level `1` and clamps the amplified delta to
`+/-0.16`. This is intentionally more conservative than Pulse until Pixel
validation shows how stable the live GL reconstruction is on a real target.

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

## Temporal Filtering

Each pyramid level owns independent lowpass/highpass ping-pong render targets
and a current bandpass texture. The reconstruction shader samples those
per-level bandpass textures directly; it does not consume the older ROI scalar
`uAmplifiedSignal`.

The expanded GL diagnostics should report both the temporal state level count
and active band, for example `temporal 3L / band 0.70-3.00Hz`.

## Validation

Local JVM coverage verifies policy defaults, invalid policy rejection,
Laplacian reconstruction source contracts, per-level temporal diagnostics, and
shader uniform/source contracts.

Phone validation was not run for this slice because the phone is currently
unavailable. The next Pixel run should verify:

- GL debug reports `Live reconstruction`.
- Pyramid diagnostics show active levels, warm temporal state, temporal level
  count, active band, start level, per-level gains, and clamp.
- Amplified view does not flash or clip aggressively under the same target used
  before the clamp.
- Difference view shows full-frame reconstructed-delta heatmap rather than an
  ROI signal tint.
- Split view still shows visible reconstructed change rather than an ROI tint.
