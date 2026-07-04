# Milestone E: GPU Color Magnification

Goal: move color magnification from CPU analysis toward texture processing.

## Tasks

- [ ] Render camera OES texture to internal RGB texture.
- [ ] Add downsample pyramid textures.
- [ ] Add temporal filter state textures.
- [ ] Implement color amplification shader pass.
- [ ] Add ROI-limited processing when possible.
- [ ] Render processed output to display and encoder surfaces.
- [ ] Add side-by-side raw/amplified view.
- [ ] Benchmark against CPU MVP.
- [ ] Commit and push to `main`.

## Success Criteria

- Sustained 30 fps at 720p display with reduced internal processing size.
- CPU load is meaningfully lower than the CPU MVP.
- Processed recording still works.
