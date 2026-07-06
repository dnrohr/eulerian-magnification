# Milestone U - ROI Device Validation

Importance: Very high. If the ROI is not over the visible target, every output is suspect.

Goal: prove that automatic and manual ROI coordinates align with the live preview on the target device.

## Tasks

- [ ] Create a repeatable Pixel 8a validation procedure for portrait/front-camera preview.
- [ ] Verify manual ROI placement against a known target in the preview.
- [ ] Verify automatic face/skin ROI placement against a visible face target.
- [ ] Capture non-sensitive evidence or describe the validation setup and result.
- [ ] Fix CameraX/GL preview mapping if the ROI does not align.
- [ ] Add or update coordinate-mapping tests for the verified transform.
- [ ] Update README and device notes with the final validation result.

## Done When

- Manual and automatic ROI overlays align with the visible target on device.
- The validation is documented with exact device, orientation, preview path, and result.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
