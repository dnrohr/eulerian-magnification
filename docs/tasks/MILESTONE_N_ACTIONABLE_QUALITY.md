# Milestone N - Actionable Quality Warnings

Goal: replace terse quality labels with guidance that tells the user what to change.

## Tasks

- [x] Map each quality status to a short user action.
- [x] Keep the compact overlay concise while showing richer guidance in expanded controls.
- [x] Distinguish phone motion, ROI/tracker motion, lighting flicker, low FPS, timestamp issues, and risky high-frequency settings.
- [x] Add tests for user-facing quality guidance.
- [x] Update README quality documentation.

## Completed Slice

- Added an `action` string to every `QualityStatus`.
- Expanded controls now show actionable guidance for non-good statuses.
- Compact overlay remains concise and still shows only the status labels.
- README and quality architecture docs now explain the action behavior.
- `QualityEvaluatorTest` verifies the user-facing guidance strings.

## Done When

- Quality messages answer "what should I do next?"
- Compact and expanded UI remain readable on the Pixel portrait viewport.
- Tests pass, docs are updated, and the task is committed and pushed to `main`.
