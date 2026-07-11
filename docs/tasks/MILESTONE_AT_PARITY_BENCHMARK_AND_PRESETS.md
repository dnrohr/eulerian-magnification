# Milestone AT - Parity Benchmark And Presets

Status: Planned

Importance: Medium-high. Once the real renderer paths exist, users need reliable presets and the project needs benchmark gates before claiming parity.

Goal: turn parity validation into supported app presets with Pixel 8a performance, thermal, and setup guidance.

## Tasks

- [ ] Define locked presets for pulse color, breathing/slow motion, object vibration, and fast tremor with known frequency bands and amplification limits.
- [ ] Add preset-specific setup guidance that tells users what target, lighting, support, and distance are needed to see the effect.
- [ ] Benchmark each preset on Pixel 8a for preview FPS, dropped frames, latency, thermal state, and recording stability.
- [ ] Add warnings when selected frequencies are too close to each other, too high for the measured FPS, or likely to overlap with camera motion/heartbeat artifacts.
- [ ] Document why tremor and object-vibration bands overlap and when the setup, target, and renderer choice matter more than the label.
- [ ] Update README and parity docs with the presets that are actually validated.

## Done When

- Users can choose a preset and get setup instructions specific enough to reproduce the visible effect.
- Each parity preset has a Pixel 8a benchmark note and a known-good sample/evidence artifact.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.
