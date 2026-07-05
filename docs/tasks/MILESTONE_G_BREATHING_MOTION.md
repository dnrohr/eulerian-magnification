# Milestone G: Breathing Motion

Goal: add a low-frequency motion mode for torso/chest breathing visualization.

## Tasks

- [ ] Add manual rectangular ROI selector.
- [x] Add breathing preset around 0.1-0.6 Hz.
- [x] Add low-frequency temporal motion filter.
- [ ] Add amplified motion, heatmap, or waveform display.
- [ ] Record output video and metadata.
- [ ] Add device verification notes.
- [ ] Commit and push to `main`.

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
