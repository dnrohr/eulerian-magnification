# Milestone A: Research And Capability Probe

Goal: create the native Android base, capture the first research decisions, and make the app report what the Pixel 8a camera and encoders can do.

## Tasks

- [x] Create Android project scaffold with Kotlin, Jetpack Compose, CameraX, and ML Kit dependencies.
- [x] Add app permission flow and a booting camera preview shell.
- [x] Add device, camera, encoder, battery, and thermal capability logging utilities.
- [x] Add `docs/research/VIDEO_MAGNIFICATION_SURVEY.md`.
- [x] Add `docs/research/ANDROID_PIPELINE_SURVEY.md`.
- [x] Add `docs/experiments/pixel8a_camera_notes.md`.
- [x] Add placeholder `docs/experiments/pixel8a_camera_capabilities.json` format.
- [x] Verify with a local Gradle build/test check.
- [x] Commit and push to `main`.

## Verification

- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

## Success Criteria

- App can build and launch from Android Studio.
- App requests camera permission and shows a preview when granted.
- Capability probe code can generate a structured JSON report from a device run.
- Research docs explain MVP, beta, and performance paths.
