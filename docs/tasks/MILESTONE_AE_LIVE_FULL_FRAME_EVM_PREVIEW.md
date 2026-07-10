# Milestone AE - Live Full-Frame EVM Preview

Status: In progress

Importance: High. The live app needs true reconstructed output before it can feel like motion magnification.

Goal: integrate the full-frame EVM renderer into the live preview path.

## Tasks

- [x] Choose the first live implementation path: GL Pulse full-frame color preview bridge first, then GPU pyramid reconstruction.
- [ ] Maintain temporal state across live frames for each pyramid level.
- [ ] Render reconstructed output into GL preview.
- [x] Keep Raw, Amp, Diff, and Split views truthful while the bridge is limited to Pulse color.
- [x] Add frame-rate and latency safeguards that disable or degrade gracefully.
- [ ] Validate on Pixel in portrait orientation with a known target.
- [x] Update UI labels so users can distinguish ROI signal visualization from full-frame color preview.

## Implementation Notes

- Added `LiveEvmPreviewPolicy` to decide when live GL can use full-frame Pulse
  color output.
- Added a `fullFrameMode` uniform to the GL color pass. When enabled, the live
  Pulse color signal is applied across the frame instead of only inside the ROI.
- Suppressed the Compose ROI tint overlay during the full-frame GL bridge so the
  image is easier to inspect.
- Added an expanded-controls `Preview:` label.
- This is not yet live MIT-style pyramid reconstruction. The remaining AE work
  is to wire temporal pyramid state into GL reconstruction and validate on the
  Pixel in portrait orientation.

## Evidence

- `docs/experiments/live_full_frame_preview_bridge.md`

## Done When

- Live preview can show full-frame reconstructed EVM output for at least one mode.
- Split view compares raw vs reconstructed output, not raw vs ROI tint.
- Relevant tests/device checks pass, and the task is committed and pushed to `main`.
