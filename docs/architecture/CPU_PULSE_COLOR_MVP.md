# CPU Pulse Color MVP

## Current Slice

The app now binds CameraX `Preview` and `ImageAnalysis` together. Analysis targets a 640x480 stream through CameraX's `ResolutionSelector` and keeps only the latest frame to avoid backlog.

Every tenth analyzed frame is sent to ML Kit Face Detection in fast mode. The largest detected face becomes the source for a forehead/upper-cheek ROI. Until a face is available, the analyzer samples a centered fallback ROI so the signal path can still be debugged.

For each frame, the analyzer samples a 16x16 grid inside the ROI, converts YUV values to an approximate green channel, and feeds the average into a 0.7-3.0 Hz bandpass filter. This band is appropriate for early pulse-color experiments, but the current UI shows only the measured signal and bandpassed value. It does not yet amplify pixels.

The face box is smoothed before ROI extraction. Small detector movement is interpolated to reduce flicker; large jumps reset the smoother so a new face position can be adopted quickly.

This smoothing is intentionally limited to the detector box. The app now adds lightweight ROI tracking between sparse detector results: `RoiTracker` estimates short-term box motion from the most recent detections, damps that motion over time, and clamps predictions to the frame. It does not yet track facial landmarks or optical flow.

The preview now includes a debug tint over the ROI, driven by the bandpassed signal, plus a compact waveform history. Raw, amplified, and difference display modes are available. This is a visualization overlay, not true per-pixel color EVM yet.

The analyzer can be rebound with either the pulse band, 0.7-3.0 Hz, or the breathing band, 0.1-0.6 Hz. The UI exposes these presets along with an amplification slider. Latency is estimated from the frame timestamp and shown next to analysis FPS. A timestamp tracker flags repeated or decreasing timestamps.

## Why This Shape

- `ImageAnalysis` is slower than the final GPU path, but it makes the math and timing visible early.
- Sparse face detection keeps ML work from blocking every frame.
- A grid sample is enough for signal debugging while avoiding full ROI conversion cost.
- The first filter is unit-tested separately from camera plumbing.
- Compose state is updated on the main executor because frame analysis runs on a background executor.
- Pulse and breathing presets are explicit because later modes need different temporal bands and user expectations.

## Next Work

- Apply the bandpassed signal back onto the preview ROI.
- Add a true processed bitmap/texture output instead of an overlay tint.
