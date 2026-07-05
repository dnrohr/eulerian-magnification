# Milestone P - Mode Rationalization

Goal: make modes distinct, understandable, and aligned with actual implementation behavior.

## Tasks

- [x] Decide whether `Tremor` and `Object` should remain separate before true motion rendering is active.
- [x] If kept separate, give them distinct labels, defaults, quality thresholds, and setup guidance. Not applicable: modes were merged.
- [x] If merged, expose a single `Fast Motion` or `Experimental Motion` preset until the pipeline matures.
- [x] Revisit frequency bands with tests and documentation.
- [x] Update README and task docs with the final mode semantics.

## Completed Slice

- Collapsed the public high-frequency UI to one `Motion` button backed by the existing `MagnificationMode.Tremor` preset.
- Renamed the user-facing label to `Fast Motion`.
- Kept the internal `ObjectVibration` enum for compatibility, but stopped exposing it in `FeatureAvailability`.
- Updated capability and settings tests for the new public mode list.
- Updated README and architecture docs to explain why the old overlapping Tremor/Object split is no longer shown.

## Done When

- Users can understand why each mode exists and when to use it.
- The mode list does not imply capabilities the app does not yet have.
- Tests and docs reflect the final mode behavior, and the task is committed and pushed to `main`.
