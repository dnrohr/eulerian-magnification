# Milestone AE - Live Full-Frame EVM Preview

Importance: High. The live app needs true reconstructed output before it can feel like motion magnification.

Goal: integrate the full-frame EVM renderer into the live preview path.

## Tasks

- [ ] Choose the first live implementation path: GPU, native, reduced-resolution CPU, or hybrid.
- [ ] Maintain temporal state across live frames for each pyramid level.
- [ ] Render reconstructed output into GL preview.
- [ ] Keep Raw, Amp, Diff, and Split views truthful for full-frame EVM.
- [ ] Add frame-rate and latency safeguards that disable or degrade gracefully.
- [ ] Validate on Pixel in portrait orientation with a known target.
- [ ] Update UI labels so users can distinguish ROI signal visualization from full-frame EVM preview.

## Done When

- Live preview can show full-frame reconstructed EVM output for at least one mode.
- Split view compares raw vs reconstructed output, not raw vs ROI tint.
- Relevant tests/device checks pass, and the task is committed and pushed to `main`.
