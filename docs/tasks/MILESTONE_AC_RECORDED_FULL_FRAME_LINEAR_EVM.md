# Milestone AC - Recorded Full-Frame Linear EVM

Importance: Very high. MIT-style EVM requires full-frame pyramid/filter/amplify/reconstruct output, not ROI tint.

Goal: implement a recorded-video full-frame linear EVM renderer for color and simple motion samples.

## Tasks

- [ ] Build or reuse a full-frame Laplacian/Gaussian pyramid for decoded frames.
- [ ] Apply temporal filtering per pixel/per pyramid level over the selected band.
- [ ] Amplify filtered spatial bands with scale-aware limits.
- [ ] Reconstruct processed RGB frames from the amplified pyramid.
- [ ] Support color-focused and luminance/motion-focused settings without changing the live UI yet.
- [ ] Export MP4, metadata, timeline, and evidence report through the existing selected-video path.
- [ ] Add synthetic full-frame tests for stationary frames, color pulse, and translating edge behavior.
- [ ] Document performance limits and artifact modes.

## Done When

- A recorded sample produces an actual reconstructed EVM video, not just an ROI tint or signal plot.
- Synthetic tests prove no-motion input stays stable and in-band signal is amplified.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
