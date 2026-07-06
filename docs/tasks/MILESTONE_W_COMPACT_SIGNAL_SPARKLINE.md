# Milestone W - Compact Signal Sparkline

Importance: High. A tiny signal trace helps users see whether a stable rhythm exists when the visual effect is subtle.

Goal: show a low-clutter signal sparkline in compact or clean-adjacent UI.

## Tasks

- [x] Decide where the sparkline can live without blocking the preview.
- [x] Render the active mode signal history with stable sizing.
- [x] Make recording/quality states coexist with the sparkline.
- [x] Keep Clean preview mostly unobstructed, or provide a separate minimal-signal mode.
- [x] Add tests for signal-history formatting or rendering state where practical.
- [x] Update README/UI reference.

## Completed Slice

- Added a compact 24 dp signal sparkline below the compact view controls when enough samples exist.
- Pulse and Fast Motion use the bandpassed color history.
- Breathing uses the breathing-motion history.
- Clean preview remains unobstructed and does not show the sparkline.
- Added `SignalDisplayPolicyTest` for active-history selection.

## Done When

- Users can see whether the active signal is stable without opening full diagnostics.
- The sparkline does not resize or crowd the compact controls.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
