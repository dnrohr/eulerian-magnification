# Milestone I: Riesz / Phase Mode

Goal: add a higher-quality phase-based motion magnification path.

## Tasks

- [x] Implement offline/reference Riesz pyramid in Python or C++.
- [ ] Validate against known sample videos.
- [ ] Port core filters to C++ or GPU shaders.
- [x] Add dominant-orientation phase manipulation.
- [x] Add phase denoising and smoothing.
- [ ] Compare quality and performance against simple EVM.
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
