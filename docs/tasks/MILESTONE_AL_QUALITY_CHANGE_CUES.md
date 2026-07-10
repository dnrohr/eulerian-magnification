# Milestone AL - Quality Change Cues

Importance: Low-medium. Nonvisual cues can help during setup, but must not become annoying.

Goal: optionally notify the user when quality meaningfully changes.

## Tasks

- [ ] Decide which quality transitions deserve a cue.
- [ ] Add an opt-in toggle for haptic or audio feedback.
- [ ] Rate-limit cues to avoid distracting repeated alerts.
- [ ] Respect device silent/accessibility expectations.
- [ ] Add tests for cue decision logic.
- [ ] Update README/UI reference.

## Done When

- Users can opt into useful quality-change feedback without noisy default behavior.
- Cue logic is bounded and testable.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
