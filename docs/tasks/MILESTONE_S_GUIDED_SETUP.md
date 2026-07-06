# Milestone S - Guided Setup

Goal: give users a concrete, repeatable way to see a useful result instead of guessing at setup.

## Tasks

- [x] Add a short in-app setup flow or setup panel for Pulse, Breathing, and Fast Motion.
- [x] Include mode-specific target guidance: cheek/forehead for pulse, torso/shoulder for breathing, high-contrast edge for fast motion.
- [x] Surface stabilization steps: mount phone, let exposure settle, lock AE/AWB, select ROI, then hide controls.
- [x] Add README screenshots or text examples for ideal setups.
- [x] Add tests for any new setup-state logic.

## Completed Slice

- Added an expanded-controls setup panel driven by the active mode.
- Pulse guide targets forehead/cheek skin and sets the expectation of subtle color pulse, not face motion.
- Breathing guide targets torso/shoulder/clothing edges and explicitly says the preview is not motion-warped yet.
- Fast Motion guide targets high-contrast moving/vibrating edges and warns that camera shake can dominate.
- Added JVM tests for mode-specific setup text.

## Done When

- A first-time user has an app-native recipe for producing a visible or measurable result.
- The guide matches the actual current capabilities and does not imply unsupported motion warping.
- Tests and docs are committed and pushed to `main`.
