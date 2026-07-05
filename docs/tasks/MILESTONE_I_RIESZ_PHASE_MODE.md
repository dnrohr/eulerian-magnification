# Milestone I: Riesz / Phase Mode

Goal: add a higher-quality phase-based motion magnification path.

## Tasks

- [x] Implement offline/reference Riesz pyramid in Python or C++.
- [ ] Validate against known sample videos.
- [x] Port core filters to C++ or GPU shaders.
- [x] Add dominant-orientation phase manipulation.
- [x] Add phase denoising and smoothing.
- [x] Compare quality and performance against simple EVM.
- [x] Document architecture in `docs/architecture/RIESZ_MODE.md`.
- [ ] Commit and push to `main`.

## Success Criteria

- Motion results improve over linear EVM.
- Reduced-resolution ROI or downsampled full-frame mode reaches 30 fps.

## Completed Slice: Offline Riesz Pyramid Reference

- Added `tools/riesz_reference/riesz_reference.py` as a dependency-free Python
  reference for pyramid construction and first-order Riesz-like x/y responses.
- Added unit tests for pyramid sizing, flat-field zero response, horizontal and
  vertical ramp orientation behavior, and ragged input validation.
- Documented the phase-mode architecture and validation plan in
  `docs/architecture/RIESZ_MODE.md`.

Verification:

- `python -m unittest discover -s tools\riesz_reference\tests`

## Completed Slice: Synthetic Riesz vs EVM Comparison

- Added a simple linear EVM baseline frame helper.
- Added phase reconstruction and synthetic Riesz-vs-linear comparison metrics.
- Added `tools/riesz_reference/compare_evm.py` for a deterministic one-pixel
  edge-motion baseline.
- Expanded Python coverage for reconstruction, linear EVM amplification,
  phase-magnified edge response, and comparison metrics.
- Documented the first baseline metrics in `docs/architecture/RIESZ_MODE.md`.

Verification:

- `python -m unittest discover -s tools\riesz_reference\tests`
- `python tools\riesz_reference\compare_evm.py`

## Completed Slice: Riesz GPU Shader Source Foundation

- Added `RieszPhaseShaderSource` with GLES 3.0 source for luminance/Riesz
  component extraction, phase projection, wrapped phase amplification, and phase
  reconstruction.
- Added shader-source tests for version placement, central-difference taps,
  orientation uniform use, wrapped delta math, and amplitude/cosine
  reconstruction.
- Documented the GPU port boundary in `docs/architecture/RIESZ_MODE.md`.

This ports the core reference filters to shader source. Runtime GL renderer and
encoder-surface integration remain separate work.

Verification:

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.gl.RieszPhaseShaderSourceTest"`

## Completed Slice: Known Frame-Sequence Validation Harness

- Added `tools/riesz_reference/validate_sample_sequences.py` to validate the
  Riesz reference against deterministic frame sequences.
- The harness checks stationary flat content, a vertical edge translating
  sideways, and a horizontal edge translating downward.
- Added coverage for passing known sequences, false-motion rejection, and
  wrong-orientation rejection.
- Documented this as the pre-decoded-frame validation layer in
  `docs/architecture/RIESZ_MODE.md`.

This does not close the public sample-video validation task. Real MIT/UBFC/local
MP4 validation still needs decoded frames from a public clip or the Android
`MediaMetadataRetriever` path.

Verification:

- `python -m unittest discover -s tools\riesz_reference\tests`
- `python tools\riesz_reference\validate_sample_sequences.py`

## Completed Slice: Offline Phase Projection And Smoothing

- Added dominant-orientation selection for a Riesz level.
- Added oriented Riesz projection, local phase/amplitude extraction, wrapped
  phase deltas, and phase amplification helpers.
- Added circular 3x3 phase smoothing so denoising preserves values around the
  `-pi`/`pi` wrap boundary.
- Expanded Python tests for horizontal/vertical orientation, phase projection,
  wrapped deltas, amplification, smoothing, wrapping, and size validation.

Verification:

- `python -m unittest discover -s tools\riesz_reference\tests`
