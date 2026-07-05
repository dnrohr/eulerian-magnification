# Demo Videos And Links

This repository does not commit sample videos. Keep downloaded clips in
`sample-videos/`, which is ignored by Git, or use the in-app picker with local
media on the device.

## Public EVM References

- MIT CSAIL Eulerian Video Magnification examples:
  <https://people.csail.mit.edu/mrub/evm/>
- MIT EVM overview video:
  <https://www.youtube.com/watch?v=ONZcjs1Pjmk>

Use these links for qualitative color and motion-magnification references. They
are useful for explaining the target effect and comparing visible artifacts, but
they should not be treated as quantitative app benchmarks unless the original
input clips and frame timing are available.

## Pulse / rPPG Samples

- UBFC-rPPG dataset:
  <https://sites.google.com/view/ybenezeth/ubfcrppg>

UBFC-rPPG is the preferred public source for pulse-color validation because it
contains face videos intended for remote PPG work. Download dataset clips outside
the repository and place short local samples under `sample-videos/`.

## Local Pixel Demo Artifacts

Current committed Pixel 8a evidence:

- `docs/experiments/pixel8a_camera_notes.md`
- `docs/experiments/pixel8a_gpu_benchmark.md`
- `docs/experiments/pixel8a_thermal_long_run.md`
- `docs/experiments/pixel8a_latest_breathing_metadata.json`

The local debug MP4 generated during the Pixel breathing run is intentionally not
committed. The on-device path from that run is recorded in
`docs/experiments/pixel8a_camera_notes.md`.

## Recommended Demo Flow

1. Start with the MIT EVM examples to show the intended visual class of effects.
2. Run the app on Pixel 8a in Pulse mode, Amplified view, and default 12x
   amplification.
3. Switch to Raw, Difference, and Split to explain what the overlay is doing.
4. Switch to Breathing mode and select a stable manual torso/shoulder ROI.
5. Record a short debug run and share the metadata JSON.
6. Toggle GL Preview and compare framing/FPS against CameraX preview.
7. Use `Validate Video` with a local UBFC-rPPG or other public sample before
   asking for a new phone recording.

## Limits

- Demo links are references, not bundled assets.
- Public clips may have license, access, or citation requirements; follow the
  source instructions before redistributing media.
- The app is a visualization prototype and does not make diagnostic claims.
