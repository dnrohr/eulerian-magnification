# Milestone AQ - Chrominance Color EVM

Status: Planned

Importance: High. MIT-style pulse/color results rely on color handling that amplifies subtle chrominance changes without making the whole frame flash.

Goal: improve pulse/color magnification quality by separating luminance from chrominance, stabilizing exposure/white balance assumptions, and reducing background color pumping.

## Tasks

- [ ] Add an RGB-to-luminance/chrominance processing path for recorded output first, then live GL if the recorded result is better.
- [ ] Compare green-only, RGB, and chrominance amplification on synthetic pulse and face/skin samples.
- [ ] Add skin/ROI weighting or background suppression so non-skin regions are not amplified as strongly as the target region.
- [ ] Gate color amplification when exposure, white balance, saturation, or lighting flicker diagnostics indicate unreliable input.
- [ ] Add output clamps that avoid posterization, skin-color inversion, and full-frame color pulses.
- [ ] Document recommended pulse setup and why the app may reject or dampen unstable color conditions.

## Done When

- Pulse/color samples show stronger target color variation with less background pumping than the current full-frame color path.
- Quality warnings and metadata explain when color magnification is being attenuated.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.
