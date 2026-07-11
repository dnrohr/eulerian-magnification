# Milestone AK - Persist Last Settings

Status: Complete

Importance: Medium. Remembering settings improves repeated testing.

Goal: persist the last useful settings across app launches.

## Tasks

- [x] Choose a small persistence mechanism such as DataStore or SharedPreferences.
- [x] Persist mode, view mode, amplification, preview path, and camera lock preference where appropriate.
- [x] Avoid persisting transient ROI unless there is a clear UX reason.
- [x] Add reset/default behavior.
- [x] Add tests for settings serialization/defaults.
- [x] Update README/UI reference if needed.

## Notes

- Added a small SharedPreferences-backed `AppSettingsStore`.
- Durable settings are mode, view mode, amplification, requested GL preview, and
  AE/AWB lock preference.
- Manual ROI, signal history, validation summaries, and recording state remain
  transient.
- Added `Reset Settings` in expanded controls; reset clears persisted settings,
  restores first-launch defaults, and clears any current manual ROI.
- JVM tests cover defaults, round-trip serialization, invalid stored values,
  unavailable modes, amplification clamping, and reset defaults.

## Done When

- Reopening the app restores the user's last testing setup without surprising transient state.
- Defaults remain sensible for first launch.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
