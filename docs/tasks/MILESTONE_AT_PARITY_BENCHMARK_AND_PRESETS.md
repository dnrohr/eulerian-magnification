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

## Supporting Slice: Live Evidence Thermal Summary

- Live validation summaries now parse `thermalservice.txt` and report Android
  thermal status, maximum sensor status, maximum temperature, and hottest
  sensor name.
- Summaries warn when Android thermal status or any sensor status reaches
  `2` (`moderate`) or higher. These warnings keep runtime smoke separate from
  thermal qualification: a run can remain crash-free while still being too hot
  to treat as clean visual/performance evidence.
- This strengthens the watched preset evidence flow, but does not replace the
  remaining known-target visual artifacts.

## Supporting Slice: Camera HAL FPS Summary

- Live validation summaries now parse Pixel camera HAL `FPS:` lines from scoped
  logcat and report camera cadence sample count, average FPS, min FPS, and max
  FPS.
- Summaries warn when the minimum observed HAL cadence drops below `23.5 FPS`,
  matching the app's low-camera-cadence quality threshold.
- This fills the previously missing live camera dropped-frame diagnosis field
  for watched preset evidence, while keeping visual parity marked unvalidated
  until known-target artifacts exist.

## Supporting Slice: Battery Context Summary

- Live validation summaries now parse `battery.txt` and report battery level,
  battery temperature, charging state, and external power source.
- Summaries warn when the device is externally powered or battery temperature is
  at least `40 C`. This makes USB charging and warm-battery conditions explicit
  when reviewing Pixel performance and visual-validation artifacts.
- These warnings are advisory and do not replace known-target visual parity
  artifacts.

## Supporting Slice: Live Thermal Quality Warning

- The live quality evaluator now consumes Android thermal state and shows
  `Thermal high` when `PowerManager.currentThermalStatus` is `moderate` or
  worse.
- The warning uses the same threshold as validation-summary thermal warnings,
  so users see the issue before trusting a hot-device capture.
- Quality cues treat thermal warnings as user-visible regressions, while
  evidence summaries continue to record detailed thermal sensor state after
  capture.

## Supporting Slice: Thermal Preflight Evidence

- Live validation captures now record `thermalservice_preflight.txt` before
  clearing logcat or launching the app.
- Capture manifests and summaries expose `thermalPreflight`, so a low-FPS or
  frozen-preview run can be rejected when the phone was already throttled before
  the app started.
- Preflight thermal warnings are advisory for runtime smoke, but `critical` or
  worse preflight state blocks app launch by default and produces a
  `thermal_preflight_aborted` summary verdict unless `-AllowThermalLaunch` is
  passed.
- Aborted bundles are useful evidence that the phone was not in a valid state
  for preset benchmarking, full-frame FPS, apparent camera freeze, or visual
  parity.
- Added `tools/test_live_validation_summary.ps1` to verify that thermal-aborted
  bundles receive `thermal_preflight_aborted` with exit code `4`, while
  incomplete non-aborted runtime bundles still fail as `runtime_failed`.
- Added `tools/wait_for_device_thermal_ready.ps1` so watched preset and
  full-frame validation runs can wait until both Android thermal status and max
  sensor status are below the chosen threshold before launching the app. The
  helper requires consecutive ready samples by default so one transient thermal
  dip does not start a watched validation run too early.
- `tools/capture_live_validation_evidence.ps1` can now run that wait directly
  with `-WaitForThermalReady`, attach `thermal_ready_wait.json`, and abort
  before launch if readiness is not reached.
- `evidence_summary.json` now embeds the parsed thermal wait result as
  `thermalReadyWait`, so reviewers can audit readiness without opening a
  separate JSON artifact.

## Supporting Slice: Visual Review Metadata

- Live validation summaries now carry watched-run visual-review metadata:
  target description, visual claim, target visibility, operator validation
  result, operator notes, and `countsAsVisualValidation`.
- Known-good preset artifacts should set `TargetVisible=true` and
  `VisualValidated=true` only after the target and expected output are inspected.
- Summaries now include `evidenceVerdict`, so preset evidence can be separated
  into runtime smoke, target-visible-but-unvalidated, and visually validated
  bundles.
- This keeps preset benchmark/runtime smoke evidence separate from visual parity
  claims until a watched target run explicitly validates the result.

## Supporting Slice: Dirty Source Evidence Warning

- Live validation summaries now warn when the capture manifest reports a dirty
  source worktree.
- Dirty captures remain useful for validating in-progress scripts or fixes, but
  should not be treated as release-quality preset or visual-parity evidence
  until repeated from a clean commit.
- The summary self-test now covers the dirty-source warning alongside thermal
  aborts, incomplete runtime bundles, and required UI-text failures.

## Supporting Slice: Strict Visual Evidence Gates

- Live validation summaries now accept `-RequireCleanSource` and
  `-RequireVisualValidation`.
- `-RequireCleanSource` exits `6` when source metadata is missing or the capture
  came from a dirty worktree.
- `-RequireVisualValidation` exits `5` when a runtime-smoke bundle has not been
  explicitly accepted as visual validation.
- `tools/capture_live_validation_evidence.ps1` forwards both gates when
  `-Summarize` is used, so watched preset commands can fail loudly unless they
  produce clean, visually accepted evidence.
- The summary self-test covers both gates with synthetic bundles. This improves
  the remaining preset/ROI/live-renderer validation workflow without requiring
  the physical Pixel during this tooling slice.

## Supporting Slice: Aborted Evidence Gate Consistency

- The capture script now uses one shared summary invocation path for normal and
  prelaunch-aborted bundles.
- Thermal/preflight-aborted summaries record requested strict gates under
  `requiredGates`, while preserving exit code `4` for the abort itself.
- The summary self-test now covers gated aborted bundles so future Pixel
  validation scripts cannot lose gate metadata when the phone is too hot to
  launch.

## Remaining

- Extend Pixel 8a evidence with a known-good visual artifact and watched target
  setup for each preset. The current automated benchmark covers rendered frame
  jank, thermal status, metadata recording stability, and encoded MP4 validity,
  but not visual parity.
- Use `tools/capture_live_validation_evidence.ps1` for watched preset runs so
  each visual claim has a screenshot or recording plus logcat, gfxinfo, thermal,
  battery, focused-window, and manifest context.
- Mark the README/parity docs as visually validated only after those watched
  target notes and artifacts exist.

## Done When

- Users can choose a preset and get setup instructions specific enough to reproduce the visible effect.
- Each parity preset has a Pixel 8a benchmark note and a known-good sample/evidence artifact.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.
