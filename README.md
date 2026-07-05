# Eulerian Magnification

Native Android prototype for real-time Eulerian video and motion magnification on Pixel 8a-class hardware.

This is a visualization and research prototype. It must not make medical-grade
heart-rate, breathing-rate, tremor, or diagnostic claims without separate
validation.

## Current App

- Kotlin Android app with Jetpack Compose, CameraX, ML Kit face detection, and
  optional OpenGL ES preview.
- Live ROI analysis for pulse-color and breathing/motion experiments.
- Modes: Pulse (`0.7-3.0 Hz`), Breathing (`0.1-0.6 Hz`), Tremor
  (`4.0-12.0 Hz`), and Object Vibration (`3.0-12.0 Hz`).
- Views: Raw, Amplified, Difference, and Split.
- Manual ROI selection, face ROI smoothing/tracking, basic translation estimate,
  quality warnings, and amplification/noise guardrails.
- Debug processed MP4 recording with sidecar metadata JSON.
- Offline recorded-frame validation scaffolding and a Python Riesz reference
  pyramid for phase-mode work.

## Requirements

- Windows with PowerShell.
- Android Studio or the Android SDK installed.
- JDK compatible with the checked-in Gradle/Android plugin configuration.
- Pixel 8a-class Android device for camera, GL, encoder, and thermal validation.

The repo ignores local media under `sample-videos/`. Keep downloaded public clips
and phone recordings there rather than committing them.

## Build

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Open the repo in Android Studio, connect a Pixel 8a, and run the `app`
configuration when device testing is needed.

## Offline Validation First

Use recorded or synthetic inputs before relying on live phone behavior:

- Recorded-video validation flow: `docs/testing/RECORDED_VIDEO_VALIDATION.md`
- Public sample plan: `docs/testing/SAMPLE_VIDEO_SOURCES.md`
- Riesz / phase-mode reference: `docs/architecture/RIESZ_MODE.md`

The intended order is:

1. Run JVM/unit tests for deterministic math and timestamp behavior.
2. Validate recorded or public sample clips through the recorded-video path.
3. Move to the phone for CameraX, GLES, encoder, exposure, and thermal checks.

## Device Demo Flow

1. Install and launch the debug app on the Pixel.
2. Grant camera permission.
3. Start in Pulse mode with Amplified view and wait for the preview timing to
   settle near 30 FPS.
4. Switch between Raw, Amplified, Difference, and Split to inspect the ROI
   visualization.
5. Try Breathing mode with a stable manual ROI around visible torso/shoulder
   motion.
6. Use `Start Recording` for a short run, then stop and share metadata if needed.
7. Toggle `Use GL Preview` for the GPU display path and compare framing, FPS, and
   quality warnings against CameraX preview.

## Device Notes

- The `ROI motion` quality warning can be caused by phone movement, subject
  movement, visible heartbeat/face movement, or tracker drift.
- The debug MP4 is an app-owned processed visualization, not the final
  preview-matching camera/GPU recording path.
- GL preview currently uses CPU analysis for ROI and signal uniforms while GPU
  color processing/display work continues.
- Public or synthetic recorded-video validation should come before asking for a
  new phone recording unless the path under test is inherently device-only.

## Useful Docs

- `docs/tasks/README.md` - roadmap task files.
- `docs/demo/DEMO_LINKS.md` - public demo references and local demo flow.
- `docs/architecture/CPU_PULSE_COLOR_MVP.md` - CPU analysis and ROI pipeline.
- `docs/architecture/GPU_COLOR_MAGNIFICATION.md` - GL color path status.
- `docs/architecture/RECORDING_PROTOTYPE.md` - recording metadata and debug MP4.
- `docs/architecture/QUALITY_STATUS.md` - quality warning meanings.
- `docs/experiments/pixel8a_camera_notes.md` - Pixel 8a observations.
