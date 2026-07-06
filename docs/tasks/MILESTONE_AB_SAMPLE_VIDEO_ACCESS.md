# Milestone AB - Sample Video Access

Importance: Medium-high. A known-good sample input makes debugging and validation repeatable.

Goal: make a stable sample video easy to process without requiring users to find their own file.

## Tasks

- [ ] Decide whether to bundle, download, or document a sample clip based on app size and licensing.
- [ ] If bundled, add the sample to app assets and expose it in the Process Video flow.
- [ ] If downloaded, add a documented retrieval path and checksum/source notes.
- [ ] Ensure the sample exercises at least one visible or measurable mode.
- [ ] Add tests for sample discovery or documented fallback.
- [ ] Update README/sample-video docs.

## Done When

- Users have a repeatable sample-video path with clear licensing/source handling.
- The sample can be processed through the same evidence-export path as user videos.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
