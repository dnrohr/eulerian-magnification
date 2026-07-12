# Milestone AT - Parity Benchmark And Presets

Status: In Progress

Importance: Medium-high. Once the real renderer paths exist, users need reliable presets and the project needs benchmark gates before claiming parity.

Goal: turn parity validation into supported app presets with Pixel 8a performance, thermal, and setup guidance.

## Tasks

- [x] Define locked presets for pulse color, breathing/slow motion, object vibration, and fast tremor with known frequency bands and amplification limits.
- [x] Add preset-specific setup guidance that tells users what target, lighting, support, and distance are needed to see the effect.
- [ ] Benchmark each preset on Pixel 8a for preview FPS, dropped frames, latency, thermal state, and recording stability.
- [x] Add warnings when selected frequencies are too close to each other, too high for the measured FPS, or likely to overlap with camera motion/heartbeat artifacts.
- [x] Document why tremor and object-vibration bands overlap and when the setup, target, and renderer choice matter more than the label.
- [ ] Update README and parity docs with the presets that are actually validated.

## Completed Slice: Locked Presets And Warnings

- Added a tested `ParityPreset` model for Pulse color, Breathing, Object vibration, and Fast tremor.
- Connected demo buttons to the locked preset settings and added an Object demo preset.
- Added preset setup guidance for target, lighting, support, distance, and expected output.
- Added runtime warnings for low measured FPS relative to the selected band and for overlapping high-frequency motion bands.
- Updated README and MIT parity docs with locked preset definitions and the overlap explanation; validation-specific claims remain pending.

## Completed Slice: Pixel Preview Benchmark Artifact

- Added `ParityPresetBenchmarkReport` CSV/JSON artifact generation.
- Added `ParityPresetBenchmarkInstrumentedTest`, which launches each locked
  preset on device and records `dumpsys gfxinfo` frame/jank percentiles plus
  thermal status.
- Ran the benchmark on Pixel 8a and wrote artifacts under
  `/sdcard/Download/eulerian-preset-benchmark`.
- Documented the 2026-07-12 short-run results in
  `docs/experiments/pixel8a_parity_preset_benchmark.md`.

## Remaining

- Extend Pixel 8a benchmark evidence for each preset with dropped/unstable
  camera frames, live analysis latency, processed recording stability, and a
  known-good visual artifact.
- Mark the README/parity docs as validated only after those benchmark notes and artifacts exist.

## Done When

- Users can choose a preset and get setup instructions specific enough to reproduce the visible effect.
- Each parity preset has a Pixel 8a benchmark note and a known-good sample/evidence artifact.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.
