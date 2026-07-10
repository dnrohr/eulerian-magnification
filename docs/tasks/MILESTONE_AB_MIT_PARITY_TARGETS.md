# Milestone AB - MIT Parity Targets

Importance: Very high. We need objective targets before claiming Eulerian Video Magnification parity.

Goal: define the MIT EVM behaviors, sample clips, settings, and acceptance criteria the app must match.

## Tasks

- [x] Identify the canonical MIT EVM outputs we want to match first: color pulse, breathing/large low-frequency motion, and small edge/object motion.
- [x] Choose local sample clips and document source, license, checksum, and expected effect.
- [x] Define per-sample settings: temporal band, amplification, chrominance/luminance mode, pyramid levels, and attenuation limits.
- [x] Define measurable acceptance criteria: visual inspection notes, frame-difference energy, signal plots, and artifact limits.
- [x] Add a parity validation doc linking samples to app commands and expected artifacts.
- [x] Update README to distinguish current app behavior from MIT-parity target behavior.

## Completed Slice

- Added `docs/testing/MIT_PARITY_TARGETS.md`.
- Defined initial parity target classes: color pulse, slow motion/breathing, and small edge/object motion.
- Documented local sample files, source/provenance handling, and SHA-256 hashes for `mit-evm-baby.mp4` and `euler.mp4`.
- Added baseline settings and acceptance criteria for full-frame EVM output.
- Listed current local commands and non-goals so ROI tint or signal-only views cannot be mistaken for parity.

## Done When

- A developer can run a named sample and know what MIT-parity output should look like.
- Sample provenance and validation criteria are documented.
- Relevant doc checks or tests pass, and the task is committed and pushed to `main`.
