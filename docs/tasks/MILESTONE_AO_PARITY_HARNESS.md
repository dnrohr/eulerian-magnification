# Milestone AO - Parity Harness

Status: Planned

Importance: Critical. We need a repeatable comparison harness before tuning the app against MIT-style output, otherwise visual claims stay subjective.

Goal: create a local validation workflow that runs known samples through the app renderer, compares against reference behavior, and exports reviewable evidence.

## Tasks

- [x] Add a deterministic offline runner for recorded renderer inputs that does not require manual Android picker interaction.
- [x] Generate raw, amplified, difference, and split-view outputs for synthetic pulse and synthetic moving-edge samples.
- [x] Save a per-run manifest with source checksum, mode, frequency band, amplification, renderer path, frame count, and metrics.
- [x] Add objective comparison metrics: in-band signal energy, localized frame-difference energy, clipping percentage, and background pumping score.
- [x] Add a compact HTML evidence report summary that places settings and metrics on one page.
- [x] Write artifact paths, timeline CSVs, HTML reports, manifests, and preview image snapshots for synthetic pulse and synthetic moving-edge samples.
- [x] Document the exact command sequence for synthetic harness artifacts in `docs/testing/MIT_PARITY_TARGETS.md`.
- [ ] Extend the harness to decode `mit-baby` and `local-euler`, then write the same artifact bundle for those local videos.

## Completed Slice: Deterministic Harness Core

- Added `ParityHarness`, which accepts decoded `RgbFrame` sequences and runs
  Raw, Amplified, Difference, and Split views through the app's
  `RecordedVideoProcessor`.
- Added manifest JSON serialization with sample identity, optional source path
  and checksum, mode, band, amplification, renderer path, visualization style,
  frame count, output dimensions, and metrics.
- Added objective metrics for mean/max frame delta, changed-pixel fraction,
  clipped-pixel fraction, in-band signal energy, localized ROI difference
  energy, and background pumping score.
- Added `ParitySyntheticSamples` for deterministic color-pulse and moving-edge
  validation without relying on local/private videos.
- Added JVM coverage for all review views, split-view metric comparison,
  renderer routing, timelines, manifest serialization, and HTML escaping.

## Completed Slice: Synthetic Artifact Writer

- Added `ParityHarnessArtifactWriter` to write ignored artifact bundles under a
  configured output directory.
- Each sample bundle contains `manifest.json`, `artifact_index.json`,
  `evidence_report.html`, per-view `signal_timeline.csv`, and first/middle/last
  PPM frame snapshots.
- Added a Gradle test property so the durable command writes to repo-root
  `parity-output/` instead of a transient test folder.
- Added JVM coverage for artifact file creation, PPM preview content, artifact
  index paths, and synthetic pulse/moving-edge durable output.
- Verified the command creates output for `synthetic-color-pulse` and
  `synthetic-moving-edge`.

## Remaining Work

- Add an Android/device or desktop decode wrapper for `mit-baby` and
  `local-euler` so real-video frames feed the same harness and artifact writer.
- Add playable processed video outputs once the harness can run decoded
  real-video samples outside manual picker interaction.

## Done When

- A developer can run one command or a short documented sequence and get comparable evidence for all parity samples.
- The report makes it obvious whether the output shows actual frame reconstruction or only tint/signal changes.
- Relevant tests/build checks pass, documentation is updated, and the task is committed and pushed to `main`.
