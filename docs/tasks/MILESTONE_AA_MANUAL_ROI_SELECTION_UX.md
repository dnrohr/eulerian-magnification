# Milestone AA - Manual ROI Selection UX

Importance: Medium-high. Better ROI controls make validation feel deliberate instead of fiddly.

Goal: make manual ROI selection easier to set, adjust, confirm, and clear.

## Tasks

- [x] Add a dedicated ROI edit mode or clear selection affordance.
- [x] Show handles, confirm/cancel, or equivalent feedback while editing.
- [x] Keep the final viewing state uncluttered after selection.
- [x] Prevent accidental ROI changes during normal preview interaction.
- [x] Add tests for ROI selection state transitions.
- [x] Update README/UI reference.

## Completed Slice

- Added an explicit `Edit ROI` / `Done ROI` control in expanded controls.
- Manual ROI drags are accepted only while ROI edit mode is active.
- Existing manual ROI outlines remain visible while viewing, but corner handles appear only during editing.
- `Clear ROI` exits edit mode and returns to automatic/center ROI.
- Added `ManualRoiEditStateTest` for edit-mode transitions.

## Done When

- Users can confidently place one ROI and know when it is active.
- ROI editing does not create duplicate-looking boxes or accidental drags.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
