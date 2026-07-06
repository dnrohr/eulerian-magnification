# Milestone AF - Persist Last Settings

Importance: Medium. Remembering settings improves repeated testing.

Goal: persist the last useful settings across app launches.

## Tasks

- [ ] Choose a small persistence mechanism such as DataStore or SharedPreferences.
- [ ] Persist mode, view mode, amplification, preview path, and camera lock preference where appropriate.
- [ ] Avoid persisting transient ROI unless there is a clear UX reason.
- [ ] Add reset/default behavior.
- [ ] Add tests for settings serialization/defaults.
- [ ] Update README/UI reference if needed.

## Done When

- Reopening the app restores the user's last testing setup without surprising transient state.
- Defaults remain sensible for first launch.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
