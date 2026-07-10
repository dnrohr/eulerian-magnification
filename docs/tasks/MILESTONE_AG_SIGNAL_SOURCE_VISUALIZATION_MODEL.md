# Milestone AG - Signal Source And Visualization Model

Importance: High. Once real EVM renderers exist, the UI must distinguish analysis band, signal source, and visualization.

Goal: clarify app settings so signal source and visual output are separate concepts.

## Tasks

- [ ] Audit current `MagnificationMode`, `ViewMode`, demo presets, and export metadata responsibilities.
- [ ] Define a model such as analysis signal/band, renderer, and visualization style.
- [ ] Migrate UI labels and settings without breaking existing metadata.
- [ ] Preserve backward compatibility in exported JSON where practical.
- [ ] Add tests for setting defaults, labels, and serialization.
- [ ] Update README/mode docs so motion modes only imply real motion rendering when active.

## Done When

- The UI no longer implies that every mode has a distinct mature visual renderer.
- Exports clearly identify analyzed signal band, renderer, and visualization style.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
