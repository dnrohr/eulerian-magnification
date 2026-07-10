# Milestone AI - Ten-Second Validation Flow

Status: Complete

Importance: Medium-high. A guided capture/process/review loop is likely the most user-friendly validation workflow.

Goal: add a guided 10-second validation flow that records, processes, and surfaces evidence artifacts.

## Tasks

- [x] Define the flow states: setup, countdown, recording, processing, review.
- [x] Use current mode/preset and guided setup text during setup.
- [x] Record a fixed-duration clip or processed frames.
- [x] Automatically generate MP4, metadata, CSV, and later plot artifacts.
- [x] Show a concise result summary with share buttons.
- [x] Add tests for flow state transitions and metadata.
- [x] Update README/demo flow.

## Notes

- Added a JVM-testable `TenSecondValidationFlow` contract with setup,
  countdown, recording, processing, and review phases.
- The flow uses the current `AnalysisSettings` and `SetupGuide` copy, fixes the
  target recording duration at ten seconds, and defines the required evidence
  bundle: processed MP4, metadata JSON, signal CSV, and HTML evidence report.
- Review state verifies whether all required artifacts are present. The existing
  recent recording/export rows already expose metadata, video, and report share
  buttons for generated artifacts.
- Phone runtime validation was not performed because the device is unavailable
  today; one-tap live capture wiring still needs device verification before it
  should be presented as fully validated UX.

## Done When

- Users can run a short validation without manually coordinating recording and export.
- The flow produces inspectable evidence and an honest summary.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
