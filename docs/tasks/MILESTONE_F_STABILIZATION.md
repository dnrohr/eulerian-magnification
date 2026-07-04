# Milestone F: Stabilization And Artifact Control

Goal: reduce false amplification caused by camera motion, exposure shifts, and poor lighting.

## Tasks

- [ ] Smooth face ROI and reject sudden detector jumps.
- [ ] Track ROI between detections.
- [ ] Add simple global translation estimate.
- [ ] Add AE/AWB lock attempts after convergence.
- [ ] Add lighting flicker and low-light warnings.
- [ ] Add saturation/noise suppression and amplification caps.
- [ ] Add quality/status overlay.
- [ ] Document artifact controls.
- [ ] Commit and push to `main`.

## Success Criteria

- Less motion-induced color flicker.
- App gives actionable warnings for poor capture conditions.
