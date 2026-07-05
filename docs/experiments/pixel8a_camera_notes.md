# Pixel 8a Camera Notes

Status: device run captured on 2026-07-05.

## How To Capture

1. Install the debug app on the Pixel 8a.
2. Grant camera permission.
3. The app writes the latest report to its app-specific external files directory under `capabilities/pixel8a_camera_capabilities.json`.
4. Open Logcat and filter for `CapabilityReportStore` to see the absolute device path.
5. Pull that file from the device and replace `docs/experiments/pixel8a_camera_capabilities.json`.
6. Add notes below for stable preview, analysis, recording, thermal, and front/rear camera behavior.

## Observations

- Capability report pulled from `/storage/emulated/0/Android/data/com.dnrohr.eulerianmagnification/files/capabilities/pixel8a_camera_capabilities.json` into `docs/experiments/pixel8a_camera_capabilities.json`.
- Device reported as Google Pixel 8a (`akita`), Android 16 / SDK 36.
- Back and front cameras both expose 32 preview sizes, including `1280x720` and `1920x1080`.
- Back and front cameras both expose FPS ranges `15-15`, `15-24`, `24-24`, `15-30`, `24-30`, `30-30`, `15-60`, and `60-60`.
- Back camera reports `1920x1080` high-speed video size; front camera did not report high-speed video sizes in this run.
- Both cameras report stabilization modes `0`, `1`, and `2`.
- Encoder list includes H.264 (`video/avc`) and H.265/HEVC (`video/hevc`) hardware and Android software encoders.
- Live Pulse preview launched with camera permission granted and showed timing OK around 27.8 FPS in the captured screenshot.
- Live Breathing preview showed the `0.1-0.6 Hz` band, breathing-motion waveform/value, and timing OK; one captured screenshot showed low-FPS quality at 23.6 FPS, while the recording screenshot showed good quality at 28.7 FPS.
- Short Breathing recording completed in Amplified view. Metadata was pulled into `docs/experiments/pixel8a_latest_breathing_metadata.json`.
- Breathing recording metadata captured mode `Breathing`, band `0.1-0.6 Hz`, 190 samples over about 7.2 seconds, zero dropped-frame estimate, ROI coordinates, dx/dy translation, and monotonic presentation timestamps from `0` to `6975461057` ns.
- The debug processed MP4 was written on-device at `/storage/emulated/0/Android/data/com.dnrohr.eulerianmagnification/files/recordings/processed-2026-07-05t10-52-22-936z/debug_processed.mp4`.
- Thermal status after the preview/recording run was `0`; sampled HAL values included battery around 28.5 C, virtual skin around 31.8 C, GPU around 49 C, and CPU clusters around 53-56 C.
- `dumpsys gfxinfo` during preview reported 854 rendered frames, 15 ms median frame time, 19 ms p90, 29 ms p99, and 2-3 ms GPU percentiles. The UI thread showed jank under live camera load, so longer tripod breathing tests should still watch quality warnings and dropped frames.
