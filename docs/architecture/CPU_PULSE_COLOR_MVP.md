# CPU Pulse Color MVP

## Current Slice

The app now binds CameraX `Preview` and `ImageAnalysis` together. Analysis targets a 640x480 stream through CameraX's `ResolutionSelector` and keeps only the latest frame to avoid backlog.

Every tenth analyzed frame is sent to ML Kit Face Detection in fast mode. The largest detected face becomes the source for a forehead/upper-cheek ROI. Until a face is available, the analyzer samples a centered fallback ROI so the signal path can still be debugged.

For each frame, the analyzer samples a 16x16 grid inside the ROI, converts YUV values to an approximate green channel, and feeds the average into a 0.7-3.0 Hz bandpass filter. This band is appropriate for early pulse-color experiments, but the current UI shows only the measured signal and bandpassed value. It does not yet amplify pixels.

## Why This Shape

- `ImageAnalysis` is slower than the final GPU path, but it makes the math and timing visible early.
- Sparse face detection keeps ML work from blocking every frame.
- A grid sample is enough for signal debugging while avoiding full ROI conversion cost.
- The first filter is unit-tested separately from camera plumbing.

## Next Work

- Stabilize the ROI over time.
- Add a waveform plot.
- Add raw/amplified/difference views.
- Apply the bandpassed signal back onto the preview ROI.
- Add timestamp monotonicity and ROI smoothing tests.
