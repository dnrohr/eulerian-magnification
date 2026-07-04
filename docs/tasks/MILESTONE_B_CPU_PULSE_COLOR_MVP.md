# Milestone B: CPU Pulse-Color MVP

Goal: prove real-time color magnification using CameraX `ImageAnalysis` and a face/skin ROI.

## Tasks

- [x] Add CameraX `ImageAnalysis` stream at 480x360 or 640x480.
- [x] Run face detection every 5-15 frames.
- [ ] Choose forehead/cheek ROI and stabilize the box.
- [x] Extract average green/chrominance signal from YUV ROI.
- [ ] Add temporal bandpass filters for pulse and breathing bands.
- [ ] Add amplified ROI preview, difference view, debug waveform, and FPS/latency overlay.
- [ ] Add unit tests for temporal filters, timestamp monotonicity, and ROI smoothing.
- [x] Document algorithm choices and limits.
- [ ] Commit and push to `main`.

## Completed Slice: CPU ROI Signal Probe

- Added 640x480 CameraX `ImageAnalysis`.
- Added ML Kit face detection every 10 frames.
- Added forehead/upper-cheek ROI sampling with centered fallback ROI.
- Added approximate green-channel YUV sampling and 0.7-3.0 Hz bandpass filter.
- Added live ROI rectangle plus analysis FPS, average green, and bandpassed value overlay.
- Added unit tests for FPS and bandpass filter behavior.

## Verification

- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

## Success Criteria

- 30 fps preview at MVP analysis resolution on Pixel 8a under good lighting.
- Face ROI remains stable for normal handheld use.
- Skin tone variation is visibly amplified without medical claims.
