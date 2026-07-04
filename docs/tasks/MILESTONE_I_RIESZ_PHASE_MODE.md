# Milestone I: Riesz / Phase Mode

Goal: add a higher-quality phase-based motion magnification path.

## Tasks

- [ ] Implement offline/reference Riesz pyramid in Python or C++.
- [ ] Validate against known sample videos.
- [ ] Port core filters to C++ or GPU shaders.
- [ ] Add dominant-orientation phase manipulation.
- [ ] Add phase denoising and smoothing.
- [ ] Compare quality and performance against simple EVM.
- [ ] Document architecture in `docs/architecture/RIESZ_MODE.md`.
- [ ] Commit and push to `main`.

## Success Criteria

- Motion results improve over linear EVM.
- Reduced-resolution ROI or downsampled full-frame mode reaches 30 fps.
