# Milestone X - Interpretable Difference View

Importance: High. Difference view should reveal useful changes without looking like meaningless flicker.

Goal: make Difference view easier to interpret for live and recorded validation.

## Tasks

- [ ] Audit current Difference output for pulse, breathing, and fast-motion clips.
- [ ] Choose a clear visual encoding for positive/negative or magnitude-only signal.
- [ ] Add scaling/clamping that avoids all-black and all-saturated output.
- [ ] Keep the ROI and raw context understandable.
- [ ] Add tests for difference-frame rendering behavior.
- [ ] Update README with how to read Difference view.

## Done When

- Difference view clearly communicates where/when the app is adding signal.
- Recorded exports and live preview use consistent semantics where possible.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
