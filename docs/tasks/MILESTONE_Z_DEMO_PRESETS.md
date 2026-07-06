# Milestone Z - Demo Presets

Importance: High. One-tap presets reduce setup errors and make demos repeatable.

Goal: add mode-specific demo presets that configure mode, view, amplification, and guidance together.

## Tasks

- [x] Define Pulse, Breathing, and Fast Motion demo defaults.
- [x] Add preset buttons or a preset selector in the expanded controls.
- [x] Apply settings atomically so the UI does not pass through confusing intermediate states.
- [x] Pair presets with the guided setup text.
- [x] Add tests for preset-to-settings mapping.
- [x] Update README/demo flow.

## Completed Slice

- Added `Pulse demo`, `Breathing demo`, and `Motion demo` buttons in expanded controls.
- Presets apply mode, view mode, and amplification together.
- The setup guide updates immediately because it is driven by the selected mode.
- Pulse demo uses Pulse/Amp/12x.
- Breathing demo uses Breathing/Diff/16x.
- Motion demo uses Fast Motion/Split/20x.
- Added `DemoPresetTest` for preset-to-settings mapping.

## Done When

- Users can choose a demo preset and immediately get sensible settings plus setup guidance.
- Presets do not imply unsupported measurement or diagnostic claims.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
