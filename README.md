# Eulerian Magnification

Native Android prototype for real-time Eulerian video and motion magnification on Pixel 8a-class hardware.

This is a visualization and research prototype. It must not make medical-grade
heart-rate, breathing-rate, tremor, or diagnostic claims without separate
validation.

## Current App

- Kotlin Android app with Jetpack Compose, CameraX, ML Kit face detection, and
  optional OpenGL ES preview.
- Live ROI analysis for pulse-color and early breathing/motion experiments.
- Modes: Pulse (`0.7-3.0 Hz`), Breathing (`0.1-0.6 Hz`), Tremor
  (`4.0-12.0 Hz`), and Object Vibration (`3.0-12.0 Hz`).
- Views: Raw, Amplified, Difference, and Split.
- Manual ROI selection, face ROI smoothing/tracking, basic translation estimate,
  quality warnings, and amplification/noise guardrails.
- Debug processed MP4 recording with sidecar metadata JSON.
- Offline recorded-frame validation scaffolding and a Python Riesz reference
  pyramid for phase-mode work.

## What To Expect

The current live app is best understood as a research viewer, not a finished
motion-magnification camera.

- Pulse mode currently shows color/tint changes inside the ROI. A good result is
  a subtle rhythmic color change in a skin patch, not visible movement of the
  face.
- Breathing mode estimates low-frequency vertical translation and shows the
  breathing signal in the expanded controls. It does not yet warp the preview to
  make chest motion visually larger.
- Tremor and Object modes expose higher-frequency bands and quality warnings,
  but the live preview is still driven mostly by ROI color/tint visualization.
- The Riesz/phase motion work exists as reference/scaffolding in the repo, but
  it is not yet the primary live preview renderer.

If the only obvious effect you see is ROI flicker or color flicker, that is a
limitation of the current implementation rather than a sign that you are using
it wrong.

## Live Usage Guide

1. Mount the phone or hold it as still as possible.
2. Launch the app and grant camera permission.
3. Start with `Pulse`, `Amplified`, and the compact preview.
4. Let exposure settle for a few seconds, then tap `Controls` and use
   `Lock AE/AWB`.
5. Drag a manual ROI over the area you want to measure. For pulse, use forehead
   or cheek skin. For breathing, use the torso/shoulder area. For object tests,
   use a high-contrast moving edge.
6. Tap `Hide` so the image is mostly unobstructed.
7. Use `Raw`, `Amplified`, `Difference`, or `Split` to compare whether the
   processed view is adding useful signal or only noise.
8. Use short recordings and sample videos for processing checks before trusting live
   handheld behavior.

### Best Pulse Setup

- Put the phone on a stand, front camera facing you.
- Use bright, steady, diffuse light. Avoid flickering LEDs and sunlight patches.
- Fill much of the frame with your face.
- Select a stable forehead or cheek ROI.
- Stay still and avoid talking, smiling, or moving your head.
- Expect a subtle color pulse, not motion.

### Best Breathing Setup

- Put the phone on a stand several feet away.
- Frame the upper torso or shoulder area.
- Select a manual ROI on clothing or a visible chest/shoulder edge.
- Breathe normally and keep the phone still.
- Watch the breathing value/waveform in expanded controls. The preview itself is
  not yet a full motion-warped result.

### Best Motion/Object Setup

- Use a stable phone mount and a high-contrast object.
- Good test targets are a black stripe on white paper, a ruler edge, or a small
  tag attached to a speaker, fan, or vibrating surface.
- Use `Object` for general mechanical vibration and `Tremor` for hand-like
  biological motion experiments.
- Keep the camera and background still. Small camera motion can dominate these
  bands.

## UI Reference

The default screen is intentionally compact so the preview remains visible.

- `Controls`: opens the full controls and diagnostics panel.
- `Hide`: closes the full panel and returns to the compact preview.
- `Color amp`, `Breath sig`, `Motion exp`, `Object exp`: compact output labels
  that state what kind of processing is active.
- `Pulse`, `Breath`, `Tremor`, `Object`: select the temporal band and analysis
  preset.
- `Raw`: shows the camera preview without the app's amplified tint.
- `Amp`: shows the current amplified/tinted visualization.
- `Diff`: shows the magnitude of the added signal, which is useful for spotting
  noise and flicker.
- `Split`: shows raw and processed views side by side in the GL preview path.
- `Amplification`: scales the measured signal. Higher values are easier to see
  but amplify noise and camera motion.
- `Band`: displays the active frequency range in Hz.
- `Lock AE/AWB`: locks auto-exposure and auto-white-balance after the camera has
  settled. This helps reduce brightness/color pumping.
- `Unlock AE/AWB`: returns exposure and white balance to camera auto mode.
- `Clear ROI`: removes the manual ROI and returns to automatic/center ROI.
- `Use GL Preview`: switches to the OpenGL preview path when available.
- `Use CameraX Preview`: returns to the standard CameraX preview.
- `Start Recording` / `Stop Recording`: records a processed debug MP4 plus
  metadata JSON in app storage.
- `Process Video`: selects a recorded/sample video and runs the offline
  processing summary path. Use this for `sample-videos/euler.mp4` after copying
  it to the phone or another picker-visible location.
- `Share Metadata`: shares the JSON metadata for the latest recording.
- `Quality`: summarizes whether the current run looks usable. Warnings can come
  from low FPS, camera/ROI motion, lighting flicker, timestamp jumps, or risky
  high-frequency/amplification combinations.
- `Output`: expanded-controls label that spells out the active pipeline:
  `Color amplification`, `Breathing signal`, `Experimental motion analysis`, or
  `Experimental object vibration`.
- `Auto ROI` / `Manual ROI`: compact status label showing whether the app is
  using automatic face/center ROI or your selected region.

Only one ROI outline should be visible when a manual ROI is set. If the automatic
ROI appears far from the visible face, that likely indicates a preview-to-analysis
coordinate mapping issue rather than a good target choice.

## Modes And Frequency Bands

The presets are broad first-pass bands:

- `Pulse`: `0.7-3.0 Hz`, roughly 42-180 beats per minute.
- `Breath`: `0.1-0.6 Hz`, roughly 6-36 breaths per minute.
- `Tremor`: `4.0-12.0 Hz`, intended for hand/biological tremor-style motion.
- `Object`: `3.0-12.0 Hz`, intended for general small mechanical vibrations.

`Tremor` and `Object` overlap heavily because they are currently coarse
high-frequency presets running through similar analysis/quality plumbing. The
lower `3.0 Hz` edge for `Object` admits slower mechanical vibration that would be
filtered out by `Tremor`; otherwise both modes deliberately cover the range where
30 FPS phone video can still represent small high-frequency motion with some
margin below Nyquist. They should become more distinct as the motion pipeline
matures, for example by adding different ROI defaults, stabilization assumptions,
quality thresholds, and narrower preset bands.

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
