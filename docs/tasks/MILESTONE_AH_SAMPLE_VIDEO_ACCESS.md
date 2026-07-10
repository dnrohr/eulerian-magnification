# Milestone AH - Sample Video Access

Status: Complete

Importance: Medium-high. A known-good sample input makes debugging and validation repeatable.

Goal: make a stable sample video easy to process without requiring users to find their own file.

## Tasks

- [x] Decide whether to bundle, download, or document a sample clip based on app size and licensing.
- [x] If bundled, add the sample to app assets and expose it in the Process Video flow. Not applicable: media is not bundled.
- [x] If downloaded, add a documented retrieval path and checksum/source notes.
- [x] Ensure the sample exercises at least one visible or measurable mode.
- [x] Add tests for sample discovery or documented fallback.
- [x] Update README/sample-video docs.

## Notes

- Decision: do not bundle media in Git or app assets yet. Sample files stay in
  ignored `sample-videos/`, with source, checksum, and recommended mode captured
  in `SampleVideoCatalog`.
- `mit-baby` is the public MIT reference clip for slow motion sanity checks.
- `local-euler` is a user-provided local-only qualitative regression sample.
- Phone picker validation was not repeated for this task because the device is
  unavailable; the same `Process Video` evidence-export path remains the intended
  phone flow.

## Done When

- Users have a repeatable sample-video path with clear licensing/source handling.
- The sample can be processed through the same evidence-export path as user videos.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
