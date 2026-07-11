# Milestone AQ - Chrominance Color EVM

Status: In progress

Importance: High. MIT-style pulse/color results rely on color handling that amplifies subtle chrominance changes without making the whole frame flash.

Goal: improve pulse/color magnification quality by separating luminance from chrominance, stabilizing exposure/white balance assumptions, and reducing background color pumping.

## Tasks

- [x] Add an RGB-to-luminance/chrominance processing path for recorded output first, then live GL if the recorded result is better.
- [x] Compare green-only, RGB, and chrominance amplification on synthetic pulse and face/skin samples.
- [ ] Add skin/ROI weighting or background suppression so non-skin regions are not amplified as strongly as the target region.
- [ ] Gate color amplification when exposure, white balance, saturation, or lighting flicker diagnostics indicate unreliable input.
- [ ] Add output clamps that avoid posterization, skin-color inversion, and full-frame color pulses.
- [ ] Document recommended pulse setup and why the app may reject or dampen unstable color conditions.

## Done When

- Pulse/color samples show stronger target color variation with less background pumping than the current full-frame color path.
- Quality warnings and metadata explain when color magnification is being attenuated.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.

## Completed Slice: Recorded Chrominance Frame Foundation

- Added `ChrominanceFrame`, a recorded-frame luminance/chrominance
  representation based on YIQ-style `Y`, `I`, and `Q` channels.
- Added RGB-to-chrominance and chrominance-to-RGB conversion utilities that
  preserve frame dimensions and timestamps.
- Added JVM coverage for RGB round-trip behavior, luminance-only output,
  chroma-only shifts, and invalid plane sizes.
- This is a foundation for recorded chrominance EVM comparison; it does not yet
  replace the existing recorded linear EVM renderer or prove better pulse
  output.
- Phone validation was not run for this slice because the phone is currently
  unavailable.

## Completed Slice: Synthetic Chrominance Strategy Comparison

- Added `ChrominanceAmplificationComparison`, a deterministic recorded-side
  comparison for green-only, RGB, and chrominance-only amplification.
- The synthetic sample uses a skin-toned pulse target with mild background
  exposure flicker so the metrics can distinguish target chroma response from
  background pumping.
- Added metrics for target chroma response, background pumping, background
  luminance shift, and target-response-to-pumping ratio.
- Added JVM coverage showing chrominance amplification improves the
  response-to-pumping ratio versus RGB and lowers background luminance pumping
  versus both RGB and green-only on the synthetic skin pulse.
- Documented the comparison in
  `docs/experiments/chrominance_color_evm_comparison.md`.
- This does not yet replace the recorded renderer or tune production clamps.
- Phone validation was not run for this slice because the comparison is
  recorded/synthetic only.
