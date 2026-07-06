# Milestone X - Interpretable Difference View

Importance: High. Difference view should reveal useful changes without looking like meaningless flicker.

Goal: make Difference view easier to interpret for live and recorded validation.

## Tasks

- [x] Audit current Difference output for pulse, breathing, and fast-motion clips.
- [x] Choose a clear visual encoding for positive/negative or magnitude-only signal.
- [x] Add scaling/clamping that avoids all-black and all-saturated output.
- [x] Keep the ROI and raw context understandable.
- [x] Add tests for difference-frame rendering behavior.
- [x] Update README with how to read Difference view.

## Completed Slice

- Difference view now uses signed color: warm/red-orange for positive signal and blue for negative signal.
- Live GL Difference view uses the same signed-color semantics inside the ROI.
- Recorded Difference exports dim the raw frame outside the ROI instead of rendering everything black.
- Added/updated tests for shader semantics and recorded difference-frame colors.

## Done When

- Difference view clearly communicates where/when the app is adding signal.
- Recorded exports and live preview use consistent semantics where possible.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
