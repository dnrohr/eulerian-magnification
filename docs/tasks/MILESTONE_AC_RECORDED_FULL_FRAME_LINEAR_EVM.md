# Milestone AC - Recorded Full-Frame Linear EVM

Status: Complete

Importance: Very high. MIT-style EVM requires full-frame pyramid/filter/amplify/reconstruct output, not ROI tint.

Goal: implement a recorded-video full-frame linear EVM renderer for color and simple motion samples.

## Tasks

- [x] Build or reuse a full-frame Gaussian pyramid for decoded frames.
- [x] Apply temporal filtering per pixel/per pyramid level over the selected band.
- [x] Amplify filtered spatial bands through the existing RGB reconstructor.
- [x] Reconstruct processed RGB frames from the amplified pyramid.
- [x] Support color-focused and simple motion-frequency settings without changing the live UI yet.
- [x] Export MP4, metadata, timeline, and evidence report through the existing selected-video path.
- [x] Add synthetic full-frame tests for stationary frames, in-band color pulse, and translating edge behavior.
- [x] Document performance limits and artifact modes.

## Implementation Notes

- `FullFrameLinearEvmRenderer` builds a four-level frame pyramid, applies the
  existing temporal bandpass to every pixel/channel at each level, and
  reconstructs a processed RGB frame with the selected amplification.
- `RecordedVideoProcessor` now uses that renderer for recorded-video
  `Amplified` and `Split` exports. `Difference` remains an ROI-centered signed
  diagnostic view for interpreting the measured signal.
- Pulse mode includes the finest level so subtle color changes can be visible.
  Breathing/Fast Motion skip the finest level to reduce pixel-scale shimmer
  while AD/AF mature the MIT sample and phase-motion validation paths.
- This is a CPU recorded-video path. Live preview parity remains separate in
  milestone AE, and phone/device validation was intentionally not run for this
  task because device testing is deferred.

## Done When

- A recorded sample produces an actual reconstructed EVM video, not just an ROI tint or signal plot.
- Synthetic tests prove no-motion input stays stable and in-band signal is amplified.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
