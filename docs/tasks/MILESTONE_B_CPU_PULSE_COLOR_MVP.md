# Milestone B: CPU Pulse-Color MVP

Goal: prove real-time color magnification using CameraX `ImageAnalysis` and a face/skin ROI.

## Tasks

- [ ] Add CameraX `ImageAnalysis` stream at 480x360 or 640x480.
- [ ] Run face detection every 5-15 frames.
- [ ] Choose forehead/cheek ROI and stabilize the box.
- [ ] Extract average green/chrominance signal from YUV ROI.
- [ ] Add temporal bandpass filters for pulse and breathing bands.
- [ ] Add amplified ROI preview, difference view, debug waveform, and FPS/latency overlay.
- [ ] Add unit tests for temporal filters, timestamp monotonicity, and ROI smoothing.
- [ ] Document algorithm choices and limits.
- [ ] Commit and push to `main`.

## Success Criteria

- 30 fps preview at MVP analysis resolution on Pixel 8a under good lighting.
- Face ROI remains stable for normal handheld use.
- Skin tone variation is visibly amplified without medical claims.
