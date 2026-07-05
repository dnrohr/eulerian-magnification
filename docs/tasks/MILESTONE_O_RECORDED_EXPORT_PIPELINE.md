# Milestone O - Recorded Export Pipeline

Goal: process a selected video into a shareable output video so algorithm results can be inspected independent of live camera behavior.

## Tasks

- [ ] Define a recorded-video processing API that can produce frames or an MP4 output.
- [ ] Start with the CPU color magnification path so it matches current live capability.
- [ ] Include side-by-side or difference output options for diagnostics.
- [ ] Save output and metadata in app storage.
- [ ] Add validator coverage for generated files where possible.
- [ ] Document how the app-generated output differs from Python/offline diagnostic renders.

## Done When

- The app can process a selected video and save a user-inspectable output artifact.
- The output path has metadata and validation coverage.
- Docs are updated, tests pass, and the task is committed and pushed to `main`.
