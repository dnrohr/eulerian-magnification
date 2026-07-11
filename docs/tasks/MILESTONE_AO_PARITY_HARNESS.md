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
- [ ] Extend the harness to write image/video artifacts and output paths for `mit-baby`, `local-euler`, synthetic pulse, and synthetic moving-edge samples.
- [ ] Document the exact command sequence in `docs/testing/MIT_PARITY_TARGETS.md` once artifact writing is available outside unit tests.

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

## Remaining Work

- Add a file-writing wrapper that decodes `mit-baby` and `local-euler`, runs the
  harness, and writes manifest JSON, timeline CSV, HTML report, and reviewable
  image/video artifacts to an ignored output directory.
- Update `docs/testing/MIT_PARITY_TARGETS.md` with that wrapper's exact command
  sequence once it exists.

## Done When

- A developer can run one command or a short documented sequence and get comparable evidence for all parity samples.
- The report makes it obvious whether the output shows actual frame reconstruction or only tint/signal changes.
- Relevant tests/build checks pass, documentation is updated, and the task is committed and pushed to `main`.
