# Milestone G: Breathing Motion

Goal: add a low-frequency motion mode for torso/chest breathing visualization.

## Tasks

- [x] Add manual rectangular ROI selector.
- [x] Add breathing preset around 0.1-0.6 Hz.
- [x] Add low-frequency temporal motion filter.
- [x] Add amplified motion, heatmap, or waveform display.
- [ ] Record output video and metadata.
- [ ] Add device verification notes.
- [ ] Commit and push to `main`.

## Completed Slice: Manual Rectangular ROI Selector

- Added `ManualRoiSelector` to normalize drag rectangles and reject tiny selections.
- Added a preview drag overlay for selecting a manual ROI.
- CameraX and GL preview analysis now receive the manual ROI and sample it directly.
- Face detection is skipped while a manual ROI is active.
- Added a `Clear ROI` control that appears when a manual ROI exists.
- Added unit tests for normalization, reverse dragging, clamping, and tiny-selection rejection.

Runtime ergonomics still need device verification, especially touch targeting over the preview.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.ManualRoiSelectorTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Breathing Motion Waveform

- Live sample handling now runs `BreathingMotionFilter` while Breathing mode is selected.
- The status overlay shows the amplified vertical breathing-motion value.
- The overlay adds a compact breathing-motion waveform below the existing color-signal waveform.
- Motion history resets when leaving Breathing mode.

This is a waveform display, not a torso-motion heatmap or motion-warped preview. Manual torso ROI selection remains pending.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.BreathingMotionFilterTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Breathing Motion Filter

- Added `BreathingMotionFilter` for low-frequency vertical translation signals.
- The filter uses the breathing band, 0.1-0.6 Hz, and emits bandpassed plus amplified vertical motion.
- It consumes the existing normalized `TranslationEstimate` values.
- Added tests for first-sample reset, amplification scaling, breathing-band preference, horizontal-motion rejection, and invalid amplification.

This creates the motion signal only. Manual torso ROI selection and visual motion display remain pending.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.BreathingMotionFilterTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Breathing Band Preset

- `MagnificationMode.Breathing` exposes a 0.1-0.6 Hz band.
- The existing mode controls allow switching between Pulse and Breathing.
- Recording metadata and debug MP4 overlays include the selected mode and band values.
- Unit coverage verifies the preset band and that the low-frequency band favors slow breathing-like signals.

This is a signal preset only. Manual torso ROI selection and visible motion amplification remain pending.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Success Criteria

- Chest/shoulder motion is visibly amplified in stable lighting.
- Guidance encourages tripod or stationary phone setup.
