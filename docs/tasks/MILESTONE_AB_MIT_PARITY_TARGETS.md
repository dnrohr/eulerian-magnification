# Milestone AB - MIT Parity Targets

Importance: Very high. We need objective targets before claiming Eulerian Video Magnification parity.

Goal: define the MIT EVM behaviors, sample clips, settings, and acceptance criteria the app must match.

## Tasks

- [ ] Identify the canonical MIT EVM outputs we want to match first: color pulse, breathing/large low-frequency motion, and small edge/object motion.
- [ ] Choose local sample clips and document source, license, checksum, and expected effect.
- [ ] Define per-sample settings: temporal band, amplification, chrominance/luminance mode, pyramid levels, and attenuation limits.
- [ ] Define measurable acceptance criteria: visual inspection notes, frame-difference energy, signal plots, and artifact limits.
- [ ] Add a parity validation doc linking samples to app commands and expected artifacts.
- [ ] Update README to distinguish current app behavior from MIT-parity target behavior.

## Done When

- A developer can run a named sample and know what MIT-parity output should look like.
- Sample provenance and validation criteria are documented.
- Relevant doc checks or tests pass, and the task is committed and pushed to `main`.
