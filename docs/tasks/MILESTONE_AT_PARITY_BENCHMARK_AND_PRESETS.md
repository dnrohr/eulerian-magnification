# Milestone AT - Parity Benchmark And Presets

Status: In Progress

Importance: Medium-high. Once the real renderer paths exist, users need reliable presets and the project needs benchmark gates before claiming parity.

Goal: turn parity validation into supported app presets with Pixel 8a performance, thermal, and setup guidance.

## Tasks

- [x] Define locked presets for pulse color, breathing/slow motion, object vibration, and fast tremor with known frequency bands and amplification limits.
- [x] Add preset-specific setup guidance that tells users what target, lighting, support, and distance are needed to see the effect.
- [x] Benchmark each preset on Pixel 8a for preview FPS, dropped frames, latency, thermal state, and recording stability.
- [x] Add warnings when selected frequencies are too close to each other, too high for the measured FPS, or likely to overlap with camera motion/heartbeat artifacts.
- [x] Document why tremor and object-vibration bands overlap and when the setup, target, and renderer choice matter more than the label.
- [x] Update README and parity docs with the presets that are actually validated.

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
- Extended the benchmark with a per-preset processed-recording metadata probe:
  30 monotonic samples, zero dropped-frame estimate, and `metadata ok` for all
  four presets on the 2026-07-12 Pixel run.
- Extended the benchmark with a per-preset encoded MP4 probe. All four presets
  produced valid MP4 exports with required atoms on the 2026-07-12 Pixel run.
- Ran the benchmark on Pixel 8a and wrote artifacts under
  `/sdcard/Download/eulerian-preset-benchmark`.
- Documented the 2026-07-12 short-run results in
  `docs/experiments/pixel8a_parity_preset_benchmark.md`.

## Completed Slice: Validated Preset Docs

- Updated README and MIT parity docs with a validation-status table for each
  locked preset.
- Documented that all four presets have Pixel 8a short-run performance,
  recording metadata, and encoded MP4 validation.
- Kept visual parity marked unvalidated for all presets until each has a
  watched target setup and known-good visual artifact.

## Remaining

- Extend Pixel 8a evidence with a known-good visual artifact and watched target
  setup for each preset. The current automated benchmark covers rendered frame
  jank, thermal status, metadata recording stability, and encoded MP4 validity,
  but not visual parity.
- Mark the README/parity docs as validated only after those benchmark notes and artifacts exist.

## Done When

- Users can choose a preset and get setup instructions specific enough to reproduce the visible effect.
- Each parity preset has a Pixel 8a benchmark note and a known-good sample/evidence artifact.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.
