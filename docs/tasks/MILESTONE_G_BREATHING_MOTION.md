# Milestone G: Breathing Motion

Goal: add a low-frequency motion mode for torso/chest breathing visualization.

## Tasks

- [x] Add manual rectangular ROI selector.
- [x] Add breathing preset around 0.1-0.6 Hz.
- [x] Add low-frequency temporal motion filter.
- [x] Add amplified motion, heatmap, or waveform display.
- [x] Record output video and metadata.
- [x] Add device verification notes.
- [x] Commit and push to `main`.

## Completed Slice: Pixel 8a Breathing Device Verification

- Installed and launched the debug app on a connected Pixel 8a.
- Pulled the generated camera capability report into `docs/experiments/pixel8a_camera_capabilities.json`.
- Verified live Breathing mode on-device with the 0.1-0.6 Hz band, breathing-motion waveform/value, and camera preview running.
- Captured a short Breathing recording in Amplified view.
- Pulled recording metadata into `docs/experiments/pixel8a_latest_breathing_metadata.json`.
- Confirmed recording metadata includes breathing settings, ROI, dx/dy translation, zero dropped-frame estimate, and monotonic presentation timestamps.
- Added Pixel 8a preview, recording, encoder, camera capability, performance, and thermal notes.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`
- `adb install -r app\build\outputs\apk\debug\app-debug.apk`
- Pixel 8a live preview screenshots for Pulse and Breathing modes
- Pixel 8a short Breathing recording metadata pull

## Completed Slice: Breathing Recording Metadata

- Existing recording controls and debug MP4 output now cover Breathing mode, including mode, band, amplification, and manual ROI.
- Recording metadata now includes per-sample translation dx/dy so breathing-motion captures preserve the motion signal source.
- Debug processed MP4 labels now include translation dx/dy.
- Added unit coverage for translation metadata serialization.

This satisfies breathing-mode debug recording and metadata. The final preview-matching encoded output remains tracked in Milestone C/E.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.recording.ProcessedRecordingSessionTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

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
