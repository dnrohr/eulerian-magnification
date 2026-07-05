# Milestone L - Sample Video Mode

Goal: add a repeatable in-app sample path so development and user validation can start from known videos instead of live camera conditions.

## Tasks

- [ ] Add a UI entry point for sample or recorded-video processing.
- [ ] Allow picking a video and showing a clear validation/processing summary.
- [ ] Prefer bundled or local sample clips for deterministic development, while keeping large media out of git unless explicitly approved.
- [ ] Show the selected source name, mode, band, frame count, and signal metrics.
- [ ] Document how to run the `euler.mp4` sample.
- [ ] Add tests around the sample/recorded-video state and summary formatting.

## Done When

- A user can run a video sample through the app without guessing whether live camera setup is the issue.
- The README describes the flow.
- Relevant tests pass, and the task is committed and pushed to `main`.
