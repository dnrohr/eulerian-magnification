# Milestone AO - Parity Harness

Status: Planned

Importance: Critical. We need a repeatable comparison harness before tuning the app against MIT-style output, otherwise visual claims stay subjective.

Goal: create a local validation workflow that runs known samples through the app renderer, compares against reference behavior, and exports reviewable evidence.

## Tasks

- [ ] Add a deterministic offline runner for recorded renderer inputs that does not require manual Android picker interaction.
- [ ] Generate raw, amplified, difference, and split-view outputs for `mit-baby`, `local-euler`, synthetic pulse, and synthetic moving-edge samples.
- [ ] Save a per-run manifest with source checksum, mode, frequency band, amplification, pyramid settings, renderer path, frame count, and output paths.
- [ ] Add objective comparison metrics: in-band signal energy, localized frame-difference energy, clipping percentage, and background pumping score.
- [ ] Add a compact HTML evidence report that places reference notes, settings, plots, and videos/images on one page.
- [ ] Document the exact command sequence in `docs/testing/MIT_PARITY_TARGETS.md`.

## Done When

- A developer can run one command or a short documented sequence and get comparable evidence for all parity samples.
- The report makes it obvious whether the output shows actual frame reconstruction or only tint/signal changes.
- Relevant tests/build checks pass, documentation is updated, and the task is committed and pushed to `main`.
