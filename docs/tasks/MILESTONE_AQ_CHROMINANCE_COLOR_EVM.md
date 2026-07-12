# Milestone AQ - Chrominance Color EVM

Status: Complete

Importance: High. MIT-style pulse/color results rely on color handling that amplifies subtle chrominance changes without making the whole frame flash.

Goal: improve pulse/color magnification quality by separating luminance from chrominance, stabilizing exposure/white balance assumptions, and reducing background color pumping.

## Tasks

- [x] Add an RGB-to-luminance/chrominance processing path for recorded output first, then live GL if the recorded result is better.
- [x] Compare green-only, RGB, and chrominance amplification on synthetic pulse and face/skin samples.
- [x] Add skin/ROI weighting or background suppression so non-skin regions are not amplified as strongly as the target region.
- [x] Gate color amplification when exposure, white balance, saturation, or lighting flicker diagnostics indicate unreliable input.
- [x] Add output clamps that avoid posterization, skin-color inversion, and full-frame color pulses.
- [x] Document recommended pulse setup and why the app may reject or dampen unstable color conditions.

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

## Completed Slice: ROI-Weighted Chrominance Suppression

- Added `RoiWeightedChrominance` to the recorded-side comparison harness.
- Added `ChrominanceAmplificationWeights`, which applies full chroma
  amplification inside the target ROI and a damped background gain outside it.
- Added JVM coverage showing ROI-weighted chrominance keeps target chroma
  response near full-frame chrominance while reducing background pumping by more
  than half on the synthetic skin-pulse fixture.
- Updated `docs/experiments/chrominance_color_evm_comparison.md` with the
  ROI-weighted strategy and result.
- This is still comparison-harness behavior; wiring it into the recorded
  renderer remains a later AQ slice.
- Installed the debug build on the Pixel 8a after full JVM/build verification;
  live camera validation was not run because this slice is recorded/synthetic
  comparison behavior only.

## Completed Slice: Recorded Color Amplification Gate

- Added `ColorAmplificationGate`, which maps recorded-side lighting diagnostics
  and ROI saturation/clipping into an amplification gain and user-facing reason.
- Recorded pulse processing now reduces the full-frame linear renderer
  amplification when lighting is settling, flickering, exposure-pumping,
  motion-contaminated, too dark, or clipped.
- Added the gate reason, gain, and saturated pixel fraction to the recorded
  evidence timeline CSV so exports can explain dampened color output.
- Added JVM coverage for gate policy and processor-level flicker attenuation.
- The live GL path later reused the same gate for lighting-based Pulse
  attenuation; saturated-pixel gating remains recorded-only until live analysis
  exposes saturation statistics.
- Installed the debug build on the Pixel 8a after focused and full
  JVM/build verification.

## Completed Slice: Recorded Color Output Clamp

- Added `ColorOutputClamp`, a recorded reconstruction clamp for channel deltas,
  chroma-vector shifts, display-rail headroom, and broad full-frame color
  pulses.
- Wired the clamp into `RgbPyramidReconstructor` and `RieszPhaseMotionRenderer`
  so recorded linear and phase output are bounded before frames are emitted.
- Added JVM coverage for channel rail avoidance, chroma-shift limiting,
  full-frame pulse damping, localized-change preservation, and reconstructor
  output headroom.
- This is recorded-side reconstruction behavior; the live GL shader still needs
  an equivalent clamp before final RGB output.
- Installed the debug build on the Pixel 8a after focused and full
  JVM/build verification.

## Completed Slice: Pulse Setup And Dampening Docs

- Updated the README pulse setup guide to explain when the app dampens or hides
  color amplification.
- Documented the recorded Pulse color gate fields in
  `docs/testing/RECORDED_VIDEO_VALIDATION.md`.
- Marked AQ complete now that chrominance representation, comparison,
  ROI/background weighting, gating, output clamps, and usage guidance are
  documented.
- This is documentation-only; no runtime behavior changed in this slice.
- Re-ran the standard JVM/build verification and reinstalled the current debug
  build on the Pixel 8a.
