# Milestone F: Stabilization And Artifact Control

Goal: reduce false amplification caused by camera motion, exposure shifts, and poor lighting.

## Tasks

- [x] Smooth face ROI and reject sudden detector jumps.
- [x] Track ROI between detections.
- [ ] Add simple global translation estimate.
- [ ] Add AE/AWB lock attempts after convergence.
- [x] Add lighting flicker and low-light warnings.
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

## Completed Slice: Lighting Flicker Warning

- Added `LightingFlickerDetector` with a rolling brightness-delta alternation heuristic.
- Quality overlay now includes `Lighting flicker` when recent ROI brightness alternates strongly.
- Added tests for alternating brightness, smooth drift, and tiny below-threshold alternations.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: ROI Tracking Between Detections

- Added `RoiTracker` to predict smoothed face-box movement between sparse ML Kit detections.
- Tracker damps motion over repeated predictions and clamps predicted boxes inside frame bounds.
- `PulseRoiAnalyzer` now uses predicted face bounds between detector results.
- Added unit tests for prediction, damping, and clamping.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Face ROI Smoothing

- `RoiSmoother` smooths small detector movements and resets after large jumps.
- `PulseRoiAnalyzer` applies the smoother to detected face bounds before deriving the skin ROI.
- `RoiSmootherTest` covers small movement smoothing and large-jump reset behavior.

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
