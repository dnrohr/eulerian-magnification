# Milestone K - Truthful UI Labels

Goal: make the live UI accurately describe what the current app is doing so users do not expect visible motion magnification from a color/tint preview.

## Tasks

- [ ] Rename or label the current pulse pipeline as color amplification.
- [ ] Add an always-visible compact status phrase that states the active output type, such as `Color amplification`.
- [ ] Mark motion-related modes as experimental when they do not yet drive a motion-warped preview.
- [ ] Keep labels short enough for the compact portrait UI.
- [ ] Update README/app docs to match the exact UI text.
- [ ] Add or update JVM tests for mode labels and output-type descriptions.

## Done When

- The live UI tells the user whether the displayed output is color amplification, breathing signal analysis, or experimental motion analysis.
- Existing unit tests pass.
- Documentation is updated, committed, and pushed to `main`.
