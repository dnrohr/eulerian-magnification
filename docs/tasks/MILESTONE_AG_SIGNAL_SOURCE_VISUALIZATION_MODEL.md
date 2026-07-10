# Milestone AG - Signal Source And Visualization Model

Status: Complete

Importance: High. Once real EVM renderers exist, the UI must distinguish analysis band, signal source, and visualization.

Goal: clarify app settings so signal source and visual output are separate concepts.

## Tasks

- [x] Audit current `MagnificationMode`, `ViewMode`, demo presets, and export metadata responsibilities.
- [x] Define a model such as analysis signal/band, renderer, and visualization style.
- [x] Migrate UI labels and settings without breaking existing metadata.
- [x] Preserve backward compatibility in exported JSON where practical.
- [x] Add tests for setting defaults, labels, and serialization.
- [x] Update README/mode docs so motion modes only imply real motion rendering when active.

## Implementation Notes

- Added `VisualizationModel`, `SignalSource`, `RendererKind`, and
  `VisualizationStyle`.
- Expanded live UI details to show separate signal source and renderer labels.
- Live recording metadata and recorded export metadata keep legacy `mode` and
  `viewMode` fields and add explicit signal/renderer/visualization fields.
- Evidence reports now include signal, renderer, and visualization labels.
- Tests cover recorded Pulse linear EVM, recorded motion Riesz phase rendering,
  diagnostic views, live full-frame bridge labels, and metadata serialization.

## Evidence

- `docs/architecture/SIGNAL_VISUALIZATION_MODEL.md`

## Done When

- The UI no longer implies that every mode has a distinct mature visual renderer.
- Exports clearly identify analyzed signal band, renderer, and visualization style.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
