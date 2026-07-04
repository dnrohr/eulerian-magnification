# Milestone B: CPU Pulse-Color MVP

Goal: prove real-time color magnification using CameraX `ImageAnalysis` and a face/skin ROI.

## Tasks

- [x] Add CameraX `ImageAnalysis` stream at 480x360 or 640x480.
- [x] Run face detection every 5-15 frames.
- [x] Choose forehead/cheek ROI and stabilize the box.
- [x] Extract average green/chrominance signal from YUV ROI.
- [x] Add temporal bandpass filters for pulse and breathing bands.
- [x] Add amplified ROI preview, difference view, debug waveform, and FPS/latency overlay.
- [x] Add unit tests for temporal filters, timestamp monotonicity, and ROI smoothing.
- [x] Document algorithm choices and limits.
- [x] Commit and push to `main`.

## Completed Slice: CPU ROI Signal Probe

- Added 640x480 CameraX `ImageAnalysis`.
- Added ML Kit face detection every 10 frames.
- Added forehead/upper-cheek ROI sampling with centered fallback ROI.
- Added approximate green-channel YUV sampling and 0.7-3.0 Hz bandpass filter.
- Added live ROI rectangle plus analysis FPS, average green, and bandpassed value overlay.
- Added unit tests for FPS and bandpass filter behavior.

## Completed Slice: ROI Smoothing And Signal Visualization

- Added ROI smoothing with large-jump reset behavior.
- Added unit tests for ROI smoothing.
- Added a bandpassed-signal waveform overlay.
- Added a debug ROI tint driven by the bandpassed signal.
- Routed analyzer callbacks back to the main executor before mutating Compose state.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Mode Controls And Timing Tests

- Added Pulse and Breathing presets with separate frequency bands.
- Added Raw, Amplified, and Difference display modes.
- Added amplification slider.
- Added latency and timestamp-monotonicity status to the overlay.
- Added tests for preset bands, breathing-band response, and timestamp monotonicity.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Success Criteria

- 30 fps preview at MVP analysis resolution on Pixel 8a under good lighting.
- Face ROI remains stable for normal handheld use.
- Skin tone variation is visibly amplified without medical claims.
