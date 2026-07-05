# Riesz / Phase Mode

This mode is intended to become the higher-quality motion magnification path for
subtle spatial motion. Linear EVM remains the real-time baseline; Riesz mode adds
a phase-based path that can separate local orientation and amplitude before
magnifying phase changes.

## Reference Implementation

The first reference slice lives in `tools/riesz_reference/riesz_reference.py`.
It is dependency-free Python so the math can be tested without requiring a phone,
GPU driver, or native build pipeline.

The reference currently provides:

- Gaussian pyramid construction with clamped-edge 3x3 smoothing.
- First-order Riesz-like x/y derivative responses for each pyramid level.
- Component energy helpers used by tests to verify orientation behavior.

This is not yet the production Riesz transform. The 3x3 derivative kernels are a
small deterministic reference that captures expected behavior for flat fields,
horizontal ramps, vertical ramps, and pyramid sizing. The production path can use
FFT, separable filters, C++, or GPU shaders once the phase-mode pipeline is ready
to move beyond offline validation.

## Expected Pipeline

1. Convert input frames or ROIs to a luminance plane.
2. Build a multi-scale Gaussian pyramid.
3. Compute Riesz components for each level.
4. Track local phase and amplitude over time.
5. Apply temporal filtering to phase deltas in the target motion band.
6. Denoise and smooth phase changes across space, scale, and time.
7. Reconstruct or approximate the magnified output for display and recording.

## Validation Strategy

The current tests cover small synthetic images because they are deterministic and
fast. The next validation step is recorded-video testing with known samples:

- Stationary flat content should produce near-zero phase response.
- Translating edges should produce stable orientation-specific responses.
- Pulse or breathing samples should be compared against the linear EVM output for
  visible artifacts, latency, and usable frame rate.

## Porting Notes

The Python reference should stay simple and readable. Native or GPU ports should
match its synthetic fixtures first, then add recorded-video acceptance tests.
Real-time mode can initially run on reduced-resolution ROIs before attempting a
full-frame 30 fps path.
