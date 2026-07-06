# UI Usefulness Audit

Date: 2026-07-05

## Findings

- The previous live screen overloaded the preview with diagnostics, controls, recording history, validation text, and waveforms. That made it hard to judge whether magnification was visibly useful.
- Manual ROI and analyzed ROI were both drawn at the same time. When a manual ROI was active, the app could appear to have a second drifting square even though the analyzer was using the manual region.
- The current phone screenshot after cleanup shows a quieter preview, but also reveals that the automatic ROI can be displayed away from the visible face in portrait/front-camera use. That points to a coordinate mapping mismatch between the analysis frame and the preview surface.
- The compact quality label is useful, but quality can still oscillate with small body motion. It should remain visible in compact mode, while detailed metrics should stay hidden until requested.

## Changes Made

- Defaulted the live UI to a compact viewing mode with a single `Controls` button.
- Moved detailed metrics, sliders, mode controls, validation, recording history, and waveform charts behind the expanded controls panel.
- Added a `Hide` button to return from the expanded panel to the compact preview.
- Suppressed the analyzed ROI outline whenever a manual ROI is active, leaving only one visible ROI square.
- Added a `Clean` preview state that hides the compact status/control bar and leaves only a way back to `Controls`.
- Added compact Raw/Amp/Diff/Split controls so view validation does not require opening the full diagnostics panel.
- Changed live auto ROI to freeze the last good detection across missed detection passes instead of extrapolating movement.
- Added compact ROI-state labels: `Manual ROI`, `Tracking`, `Frozen ROI`, and `Center ROI`.
- Added `signal_timeline.csv` and richer metadata to app-native selected-video exports.
- Added mode-specific setup recipes in expanded controls for Pulse, Breathing, and Fast Motion.
- Selecting `Split` now automatically uses GL preview for live raw-left/processed-right comparison; verified on Pixel device `47091JEKB05516` in portrait with a non-sensitive room target.
- Added a compact sparkline for active signal history while keeping Clean preview unobstructed.
- Difference view now uses signed warm/blue colors and keeps dim context in recorded exports.
- Selected-video exports now include `evidence_report.html` with an inline signal plot and shareable recent-export `Report` action.
- Added demo presets that apply mode, view, and amplification together before showing the matching setup guide.
- Added explicit ROI edit mode so normal preview drags do not accidentally move the manual ROI; corner handles appear only while editing.

## Euler Sample Output

- Generated `docs/experiments/euler_audit_color_magnification.mp4`.
- The video is a diagnostic side-by-side render of `sample-videos/euler.mp4`: original plus ROI on the left, CPU color magnification on the right.
- Settings for the diagnostic render: 0.7-3.0 Hz temporal band, 16x amplification, first 7.99 seconds, 29.78 fps.

## Remaining Work

- Fix analysis-to-preview ROI coordinate mapping for portrait/front-camera mode. The compact UI makes this problem much easier to see.
- Consider a dedicated ROI mode where tapping/dragging temporarily reveals selection handles, then returns to uncluttered viewing.
