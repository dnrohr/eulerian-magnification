# Milestone Q - Validation-First UI

Goal: make the live screen useful for judging whether magnification is visible, not just whether diagnostics are changing.

## Tasks

- [x] Add a true clean preview state that hides status text and diagnostic controls while keeping a way back to controls.
- [x] Keep Raw, Amplified, Difference, and Split available without opening the full diagnostics panel.
- [x] Preserve recording visibility when the preview is otherwise clean.
- [x] Update README and audit notes with the new validation flow.
- [x] Run local build/test checks, commit, and push.

## Completed Slice

- Added a `Clean` button to the compact overlay.
- Clean preview hides the compact quality/mode/ROI/FPS bar and leaves only a `Controls` button, plus recording time when recording is active.
- Added Raw/Amp/Diff/Split buttons to the compact overlay so validation can switch views without opening the full panel.

## Done When

- A user can select a view mode, hide most UI, and inspect the image with minimal visual obstruction.
- The expanded diagnostics still contain the detailed controls and remain reachable from clean preview.
- Tests/build pass and the task is committed and pushed to `main`.
