# Milestone AO - Parity Harness

Status: Complete

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
- [x] Add an instrumented device runner that decodes `mit-baby` or a supplied local video path, then writes the same artifact bundle.
- [x] Pull and inspect evidence notes for `mit-baby` and `local-euler` after running the device harness on both samples.

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

## Completed Slice: Decoded Video Device Runner

- Added `ParityHarnessInstrumentedTest`, which uses Android
  `MediaMetadataRetriever` through `RecordedVideoFrameDecoder`.
- The runner defaults to the bundled `mit-evm-baby.mp4` androidTest asset and
  also accepts a `sampleVideoPath` instrumentation argument for local-only
  samples such as `euler.mp4`.
- The runner writes the same parity bundle as the JVM synthetic writer:
  manifest, artifact index, HTML report, timelines, and PPM preview frames.
- It records source path and SHA-256 in the manifest so local video evidence can
  be tied back to the expected sample.
- The runner requests scaled frames from `RecordedVideoFrameDecoder` so large
  local videos do not exhaust the instrumented test process before downsampling.

## Device Evidence: Pixel 8a Decoded Samples

Date: 2026-07-11

- Connected device: Pixel 8a `47091JEKB05516`.
- `mit-baby`: decoded the bundled `mit-evm-baby.mp4` androidTest asset,
  verified SHA-256
  `2C5E744384AB88FCCD3AA4883959B33EB4CDB7384C3E46E788CEDE821B2478EE`, wrote
  pulled files under ignored
  `sample-videos/exports/eulerian-parity-output/mit-baby/`, and recorded 36
  frames at 160x90. Amplified view used `recorded_riesz_phase_motion` with
  mean absolute delta `0.314350`, changed-pixel fraction `0.142095`, clipped
  fraction `0.004720`, and in-band signal energy `3.567199`.
- `local-euler`: copied ignored local `sample-videos/euler.mp4` to ignored
  `app/src/androidTest/assets/euler.mp4`, decoded it as a test asset, verified
  SHA-256
  `BF549FEAA994104817A6AFCC39037FB80A013D4074E0AC00EC167F4471B0ACBF`, wrote
  pulled files under ignored
  `sample-videos/exports/eulerian-parity-output/local-euler/`, and recorded 36
  frames at 51x90. Amplified view used `recorded_linear_evm` with mean absolute
  delta `14.687229`, changed-pixel fraction `0.711002`, clipped fraction
  `0.059150`, and in-band signal energy `16.698975`.
- Both bundles include manifest JSON, artifact index JSON, HTML report,
  per-view signal timelines, and first/middle/last PPM snapshots for Raw,
  Amplified, Difference, and Split views.

## Future Work

- Add playable processed video outputs if visual inspection needs motion rather
  than still snapshots; the current AO evidence bundle is static snapshots plus
  metrics/timelines.

## Done When

- A developer can run one command or a short documented sequence and get comparable evidence for all parity samples.
- The report makes it obvious whether the output shows actual frame reconstruction or only tint/signal changes.
- Relevant tests/build checks pass, documentation is updated, and the task is committed and pushed to `main`.
