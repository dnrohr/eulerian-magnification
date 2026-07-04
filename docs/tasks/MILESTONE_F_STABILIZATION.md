# Milestone F: Stabilization And Artifact Control

Goal: reduce false amplification caused by camera motion, exposure shifts, and poor lighting.

## Tasks

- [ ] Smooth face ROI and reject sudden detector jumps.
- [ ] Track ROI between detections.
- [ ] Add simple global translation estimate.
- [ ] Add AE/AWB lock attempts after convergence.
- [ ] Add lighting flicker and low-light warnings.
- [x] Add saturation/noise suppression and amplification caps.
- [x] Add quality/status overlay.
- [x] Document artifact controls.
- [ ] Commit and push to `main`.

## Completed Slice: Basic Quality Status Overlay

- Added `QualityEvaluator` for missing ROI, low light, low FPS, timestamp instability, and weak signal.
- Added live quality row to the overlay.
- Added unit tests for stable, unstable, and weak-signal cases.
- Documented the quality/status behavior in `docs/architecture/QUALITY_STATUS.md`.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Amplification Caps And Noise Suppression

- Added `ArtifactSuppressor` to centralize low-signal suppression and amplified-signal clamping.
- Live ROI tint and debug MP4 rendering now share the same suppression/capping behavior.
- Added unit tests for low-signal suppression, clamping, and normal signal pass-through.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Success Criteria

- Less motion-induced color flicker.
- App gives actionable warnings for poor capture conditions.
