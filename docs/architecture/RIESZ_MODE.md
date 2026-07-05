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
- Dominant-orientation projection for a pyramid level.
- Local phase/amplitude extraction along the dominant orientation.
- Wrapped phase delta amplification.
- Circular 3x3 phase smoothing for denoising without averaging across the
  `-pi`/`pi` boundary.

This is not yet the production Riesz transform. The 3x3 derivative kernels are a
small deterministic reference that captures expected behavior for flat fields,
horizontal ramps, vertical ramps, wrapped phase deltas, circular phase smoothing,
and pyramid sizing. The production path can use FFT, separable filters, C++, or
GPU shaders once the phase-mode pipeline is ready to move beyond offline
validation.

## Expected Pipeline

1. Convert input frames or ROIs to a luminance plane.
2. Build a multi-scale Gaussian pyramid.
3. Compute Riesz components for each level.
4. Track local phase and amplitude over time.
5. Apply temporal filtering to phase deltas in the target motion band.
6. Denoise and smooth phase changes across space, scale, and time.
7. Reconstruct or approximate the magnified output for display and recording.

The reference implementation now covers steps 3-6 at the small-image math-helper
level. It does not yet reconstruct a full magnified frame sequence or validate
against real sample videos.

## EVM Baseline Comparison

`tools/riesz_reference/compare_evm.py` runs a deterministic synthetic one-pixel
edge-motion comparison between simple linear EVM and the offline Riesz phase
reference. The first baseline uses a 24x16 grayscale edge pair and 4.0x
amplification:

- Linear mean absolute delta: `0.116667`
- Phase mean absolute delta: `0.097524`
- Phase-to-linear delta ratio: `0.835919`
- Linear roughness: `0.107692`
- Phase roughness: `0.037700`

This is not a quality win claim for real video. It is a repeatable baseline that
confirms the phase reference produces a comparable motion response and a smoother
synthetic output than simple linear intensity amplification on this fixture.
Public sample-video validation remains the next evidence gate.

## Validation Strategy

The current tests cover small synthetic images because they are deterministic and
fast. `tools/riesz_reference/validate_sample_sequences.py` extends that into
known frame-sequence validation:

- stationary flat content must produce near-zero linear and phase deltas
- a vertical edge translating sideways must produce motion response with stable
  horizontal orientation
- a horizontal edge translating downward must produce motion response with stable
  vertical orientation

The next validation step is recorded-video testing with public or locally
recorded samples:

- Stationary flat content should produce near-zero phase response.
- Translating edges should produce stable orientation-specific responses.
- Pulse or breathing samples should be compared against the linear EVM output for
  visible artifacts, latency, and usable frame rate.

## Porting Notes

The Python reference should stay simple and readable. Native or GPU ports should
match its synthetic fixtures first, then add recorded-video acceptance tests.
Real-time mode can initially run on reduced-resolution ROIs before attempting a
full-frame 30 fps path.

`RieszPhaseShaderSource` is the first GPU port slice. It provides GLES 3.0 shader
source for:

- luminance conversion and central-difference Riesz x/y components
- phase projection along a dominant-orientation uniform
- wrapped phase-delta amplification
- grayscale reconstruction from amplified phase and amplitude

These shaders are source-tested and compile with the app, but they are not yet
wired into `CameraOesRenderer` or the encoder-surface path. Runtime wiring should
bind them behind reduced-resolution render targets and reuse the recorded-video
and synthetic fixtures before phone-only tuning.
