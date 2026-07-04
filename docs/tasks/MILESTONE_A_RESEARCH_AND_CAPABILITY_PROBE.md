# Milestone A: Research And Capability Probe

Goal: create the native Android base, capture the first research decisions, and make the app report what the Pixel 8a camera and encoders can do.

## Tasks

- [ ] Create Android project scaffold with Kotlin, Jetpack Compose, CameraX, and ML Kit dependencies.
- [ ] Add app permission flow and a booting camera preview shell.
- [ ] Add device, camera, encoder, battery, and thermal capability logging utilities.
- [ ] Add `docs/research/VIDEO_MAGNIFICATION_SURVEY.md`.
- [ ] Add `docs/research/ANDROID_PIPELINE_SURVEY.md`.
- [ ] Add `docs/experiments/pixel8a_camera_notes.md`.
- [ ] Add placeholder `docs/experiments/pixel8a_camera_capabilities.json` format.
- [ ] Verify with a local Gradle build/test check.
- [ ] Commit and push to `main`.

## Success Criteria

- App can build and launch from Android Studio.
- App requests camera permission and shows a preview when granted.
- Capability probe code can generate a structured JSON report from a device run.
- Research docs explain MVP, beta, and performance paths.
