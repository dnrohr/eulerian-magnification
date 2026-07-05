# Milestone P - Mode Rationalization

Goal: make modes distinct, understandable, and aligned with actual implementation behavior.

## Tasks

- [ ] Decide whether `Tremor` and `Object` should remain separate before true motion rendering is active.
- [ ] If kept separate, give them distinct labels, defaults, quality thresholds, and setup guidance.
- [ ] If merged, expose a single `Fast Motion` or `Experimental Motion` preset until the pipeline matures.
- [ ] Revisit frequency bands with tests and documentation.
- [ ] Update README and task docs with the final mode semantics.

## Done When

- Users can understand why each mode exists and when to use it.
- The mode list does not imply capabilities the app does not yet have.
- Tests and docs reflect the final mode behavior, and the task is committed and pushed to `main`.
