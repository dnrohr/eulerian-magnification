# Milestone AF - Phase Motion Renderer Integration

Importance: High. Phase/Riesz magnification is the better route for subtle motion and fewer linear-EVM artifacts.

Goal: promote the existing Riesz/phase reference work into an app renderer path.

## Tasks

- [ ] Decide whether phase/Riesz starts as recorded-only or live-preview capable.
- [ ] Convert the existing shader-source foundation into an executable render pass.
- [ ] Track local phase/amplitude over time with temporal filtering.
- [ ] Amplify wrapped phase deltas with spatial/temporal smoothing.
- [ ] Reconstruct output frames or a clearly labeled phase-motion visualization.
- [ ] Validate against the existing Riesz reference and MIT/local motion samples.
- [ ] Add tests for phase state, wrapping, amplitude gating, and reconstruction.
- [ ] Document when to use phase motion versus linear EVM.

## Done When

- The app has a real phase/Riesz motion output path beyond shader source scaffolding.
- Validation shows it improves at least one subtle-motion sample over linear EVM.
- Relevant tests/build/device checks pass, and the task is committed and pushed to `main`.
