# Pixel 8a Camera Notes

Status: pending device run.

## How To Capture

1. Install the debug app on the Pixel 8a.
2. Grant camera permission.
3. The app writes the latest report to its app-specific external files directory under `capabilities/pixel8a_camera_capabilities.json`.
4. Open Logcat and filter for `CapabilityReportStore` to see the absolute device path.
5. Pull that file from the device and replace `docs/experiments/pixel8a_camera_capabilities.json`.
6. Add notes below for stable preview, analysis, recording, thermal, and front/rear camera behavior.

## Observations

- Pending: supported preview sizes.
- Pending: stable 720p and 1080p preview modes.
- Pending: high-speed FPS ranges exposed to the app.
- Pending: encoder support for H.264 and H.265.
- Pending: thermal behavior during 5-minute preview.
