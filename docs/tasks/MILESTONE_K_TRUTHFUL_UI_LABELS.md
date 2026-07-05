# Milestone K - Truthful UI Labels

Goal: make the live UI accurately describe what the current app is doing so users do not expect visible motion magnification from a color/tint preview.

## Tasks

- [x] Rename or label the current pulse pipeline as color amplification.
- [x] Add an always-visible compact status phrase that states the active output type, such as `Color amplification`.
- [x] Mark motion-related modes as experimental when they do not yet drive a motion-warped preview.
- [x] Keep labels short enough for the compact portrait UI.
- [x] Update README/app docs to match the exact UI text.
- [x] Add or update JVM tests for mode labels and output-type descriptions.

## Completed Slice

- Added `outputLabel` and `compactOutputLabel` to `MagnificationMode`.
- Compact live UI now shows `Color amp`, `Breath sig`, `Motion exp`, or `Object exp`.
- Expanded controls now show `Output: ...` with the full pipeline description.
- README UI reference documents the exact labels.
- `AnalysisSettingsTest` verifies the user-facing output labels.

## Done When

- The live UI tells the user whether the displayed output is color amplification, breathing signal analysis, or experimental motion analysis.
- Existing unit tests pass.
- Documentation is updated, committed, and pushed to `main`.
