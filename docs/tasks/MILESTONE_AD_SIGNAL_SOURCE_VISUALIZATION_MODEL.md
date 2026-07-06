# Milestone AD - Signal Source And Visualization Model

Importance: Medium. Separating measurement source from visualization style would make the UI more honest and flexible.

Goal: clarify app settings so signal source and visual output are separate concepts.

## Tasks

- [ ] Audit current `MagnificationMode` and `ViewMode` responsibilities.
- [ ] Define a model such as signal source/preset plus visualization style.
- [ ] Migrate UI labels and settings without breaking exports.
- [ ] Preserve backward compatibility in metadata where practical.
- [ ] Add tests for setting defaults, labels, and serialization.
- [ ] Update README/mode docs.

## Done When

- The UI no longer implies that every mode has a distinct mature visual renderer.
- Exports clearly identify both the analyzed signal band and visualization style.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
