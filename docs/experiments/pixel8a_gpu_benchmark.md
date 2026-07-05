# Pixel 8a GPU Benchmark

Status: short-run benchmark captured on 2026-07-05.

## Setup

- Device: Google Pixel 8a (`akita`), Android 16 / SDK 36.
- Build: debug APK installed from `app/build/outputs/apk/debug/app-debug.apk`.
- Camera permission granted through ADB.
- Workload: live front-facing preview with CPU ROI analysis and overlay controls active.
- Metrics source: overlay readouts and `adb shell dumpsys gfxinfo com.dnrohr.eulerianmagnification`.

## CameraX CPU Preview Sample

- Overlay readouts during Pulse/Breathing runs showed analysis around 27-29 FPS when timing was OK.
- `dumpsys gfxinfo` during the CameraX preview run reported 854 rendered frames.
- Median frame time was 15 ms, p90 was 19 ms, p95 was 21 ms, and p99 was 29 ms.
- GPU percentiles were 2 ms p50, 2 ms p90, 3 ms p95, and 3 ms p99.
- Janky frames were 278 / 854, or 32.55%, with most missed deadlines attributed to the UI thread.

## GL Preview Sample

- After switching to GL preview and resetting gfxinfo stats, the 15 second sample reported 350 rendered frames.
- Median frame time was 11 ms, p90 was 13 ms, p95 was 14 ms, and p99 was 16 ms.
- GPU percentiles were 3 ms p50, 3 ms p90, 3 ms p95, and 6 ms p99.
- Janky frames were 4 / 350, or 1.14%.
- The GL path met the 30 FPS display target in this short sample.

## Interpretation

- The short GL preview sample had lower UI frame times and far fewer janky frames than the CameraX CPU preview sample while CPU ROI analysis remained active.
- This validates the current GL display path as a better preview/rendering base for Milestone E.
- This does not yet prove lower total CPU load, because the CPU analysis path still runs beside GL preview and the sample was short.
- A longer tripod run should still measure thermal behavior, battery impact, and app CPU usage after GPU temporal filtering replaces more CPU work.

## Stability Finding

- A later clean GL screenshot attempt crashed the app rather than locking the phone.
- Crash buffer showed `GlException: Could not compile GLES shader` because the GLSL `#version` declaration was on line 2 of a Kotlin raw string.
- The shader source formatting was fixed and reinstalled.
- Follow-up Pixel 8a smoke test stayed alive after switching to GL preview. The overlay showed `Use CameraX Preview`, GL timing/benchmark text, and no new crash-buffer entry for the package after logcat was cleared.
- The GL preview image was visibly upside down in one follow-up screenshot, then a clean reboot showed GL upright but stretched/cropped differently from CameraX. This was traced to rendering a landscape camera buffer across the full portrait render target. The OES-to-RGB pass now orients the buffer aspect to the surface before applying aspect-fill viewport math, so GL preserves camera aspect and clips overflow like CameraX `FILL_CENTER`.
