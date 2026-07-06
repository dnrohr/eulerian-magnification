# Milestone U - ROI Device Validation

Importance: Very high. If the ROI is not over the visible target, every output is suspect.

Goal: prove that automatic and manual ROI coordinates align with the live preview on the target device.

## Tasks

- [x] Create a repeatable Pixel 8a validation procedure for portrait/front-camera preview.
- [ ] Verify manual ROI placement against a known target in the preview.
- [ ] Verify automatic face/skin ROI placement against a visible face target.
- [x] Capture non-sensitive evidence or describe the validation setup and result.
- [ ] Fix CameraX/GL preview mapping if the ROI does not align.
- [ ] Add or update coordinate-mapping tests for the verified transform.
- [x] Update README and device notes with the final validation result.

## Current Slice

- Added `docs/testing/ROI_DEVICE_VALIDATION.md` with manual and automatic ROI validation procedures.
- Installed and launched the latest debug APK on connected device `47091JEKB05516`.
- Captured an unattended front-camera probe on 2026-07-05.
- Result: no visible face target was in frame; the app showed `Center ROI`, which is expected fallback behavior but does not verify automatic face ROI alignment.

## Remaining Validation

- Manual ROI still needs a non-sensitive known target deliberately placed in frame.
- Automatic ROI still needs a visible face target in frame.
- If either validation shows mismatch, update `PreviewRoiMapper` and its tests before marking this milestone complete.

## Done When

- Manual and automatic ROI overlays align with the visible target on device.
- The validation is documented with exact device, orientation, preview path, and result.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
