# Milestone AU - ROI Source Selector

Status: In Progress

Importance: High. Manual ROI should become an explicit expert/tooling choice, not the default way to make motion magnification work.

Goal: add a clear ROI source selector and make the normal motion experience start from automatic or full-frame coverage when the validated renderer path can support it.

## Prerequisites

- Complete Milestone U manual/automatic ROI device validation, or document any mapping fixes needed first.
- Complete the Pixel 8a controlled object-motion validation slice in Milestone AR, so the app has evidence that live phase motion is visibly useful before hiding the manual ROI path behind a selector.

## Tasks

- [x] Add an ROI source model with at least `Auto`, `Full frame`, and `Manual`.
- [x] Default motion modes to `Auto` or `Full frame`, depending on the validated live phase path.
- [x] Keep `Manual` selectable from Controls for difficult targets, debugging, and experiments.
- [x] Update live phase eligibility so missing manual ROI is not a fallback when the selected ROI source is Auto or Full frame.
- [x] Make the compact status label report the active source plainly, for example `Full frame`, `Auto ROI`, or `Manual ROI`.
- [x] Persist the selected ROI source, but do not persist transient manual rectangle coordinates unless a later task explicitly chooses that behavior.
- [x] Update README usage and troubleshooting so users know when to choose Manual ROI.
- [x] Add unit coverage for default ROI source, persisted source restoration, phase eligibility, and source-label behavior.

## Completed Slice: Selectable ROI Source

- Added `RoiSource` and `RoiSourcePolicy` with `Auto`, `Full frame`, and `Manual`.
- First launch and Reset Settings now default motion-capable modes to `Full frame`; Pulse-only availability defaults to `Auto`.
- Controls exposes an `ROI Source` selector. `Edit ROI` switches to Manual and enables drag placement.
- The live analyzer uses the selected source: automatic face/center ROI, whole-frame ROI, or manual ROI.
- Live phase planning now receives the selected active ROI and distinguishes missing manual ROI from waiting for automatic ROI.
- Compact status reports `Full frame` or `Manual ROI` plainly when those sources are selected.
- SharedPreferences persists the selected source, while manual rectangle coordinates remain transient.

## Done When

- First launch and Reset Settings do not require the user to draw a manual ROI for the default motion path.
- Manual ROI remains available as a deliberate option.
- The live UI explains which ROI source is active and why phase may fall back.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.
