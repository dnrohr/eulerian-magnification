# Milestone J: Polish

Goal: turn the prototype into a coherent demo app with clean UX and release-ready guardrails.

## Tasks

- [ ] Refine controls, settings, presets, and recording gallery.
- [ ] Add app icon and polished permission copy.
- [ ] Add thermal behavior notes and long-run benchmarks.
- [x] Add README with setup, device testing, and demo flow.
- [ ] Add demo videos or links when available.
- [ ] Hide unavailable modes based on capability reports.
- [ ] Commit and push to `main`.

## Completed Slice: ROI Motion Quality Label

- Renamed the live quality warning label from `Camera motion` to `ROI motion`.
- Kept the existing ROI translation threshold behavior, but documented that the warning can come from phone motion, subject motion, heartbeat-visible face motion, or tracker drift.
- Added unit coverage for the user-facing quality label.
- Captured the Pixel 8a observation that front-facing heartbeat/face movement can oscillate the motion warning.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.quality.QualityEvaluatorTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Operator README

- Replaced the scaffold README with current app scope, requirements, build
  commands, offline-first validation order, Pixel device demo flow, and known
  device notes.
- Linked the existing recorded-video validation, public sample, Riesz, GPU,
  recording, quality, and Pixel notes docs from the top-level README.
- Kept the medical/diagnostic limitation visible near the top of the project.

## Verification

- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

## Success Criteria

- Minimum viable demo flow is smooth on Pixel 8a.
- App does not make medical-grade claims.
- README accurately reflects supported modes and known limits.
