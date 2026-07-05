# Milestone L - Sample Video Mode

Goal: add a repeatable in-app sample path so development and user validation can start from known videos instead of live camera conditions.

## Tasks

- [x] Add a UI entry point for sample or recorded-video processing.
- [x] Allow picking a video and showing a clear validation/processing summary.
- [x] Prefer bundled or local sample clips for deterministic development, while keeping large media out of git unless explicitly approved.
- [x] Show the selected source name, mode, band, frame count, and signal metrics.
- [x] Document how to run the `euler.mp4` sample.
- [x] Add tests around the sample/recorded-video state and summary formatting.

## Completed Slice

- Renamed the in-app picker action from `Validate Video` to `Process Video`.
- Preserved the selected video's display name in the cache copy, so `euler.mp4` appears in the processing summary.
- Changed recorded-video summaries to begin with `Video processing`.
- Documented how to copy and process `sample-videos/euler.mp4` through the Android picker.
- Updated `RecordedVideoValidationTest` for the user-facing summary text.

## Done When

- A user can run a video sample through the app without guessing whether live camera setup is the issue.
- The README describes the flow.
- Relevant tests pass, and the task is committed and pushed to `main`.
