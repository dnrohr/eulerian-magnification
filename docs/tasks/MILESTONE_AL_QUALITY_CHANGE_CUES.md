# Milestone AL - Quality Change Cues

Status: Complete

Importance: Low-medium. Nonvisual cues can help during setup, but must not become annoying.

Goal: optionally notify the user when quality meaningfully changes.

## Tasks

- [x] Decide which quality transitions deserve a cue.
- [x] Add an opt-in toggle for haptic or audio feedback.
- [x] Rate-limit cues to avoid distracting repeated alerts.
- [x] Respect device silent/accessibility expectations.
- [x] Add tests for cue decision logic.
- [x] Update README/UI reference.

## Notes

- Added `QualityCuePolicy` for testable haptic cue decisions.
- Cues are haptic-only, off by default, and controlled by the `Quality Cues`
  toggle in expanded controls.
- Cues trigger for meaningful transitions: good-to-problem, problem-to-good, or
  primary issue changes among high-signal statuses. Weak signal and static
  settings-risk warnings do not cue.
- Cues are rate-limited to at least 10 seconds apart and skipped when system
  haptic feedback is disabled.
- The opt-in preference is persisted with other durable app settings.

## Done When

- Users can opt into useful quality-change feedback without noisy default behavior.
- Cue logic is bounded and testable.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
