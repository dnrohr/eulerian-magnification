# Milestone AN - History Browser

Status: Complete

Importance: Low for now. A polished history view is useful later, after core outputs are trustworthy.

Goal: improve browsing and comparing previous recordings/exports.

## Tasks

- [x] Design a simple list/detail browser for recordings and processed videos.
- [x] Show metadata summary, artifact links, and share actions.
- [x] Add delete/cleanup behavior for app-owned artifacts.
- [x] Avoid turning the main preview into a dashboard.
- [x] Add tests for gallery ordering and cleanup logic.
- [x] Update README/UI reference.

## Notes

- Expanded `RecordingGallery` into a small history model with artifact inventory,
  detail summaries, and newest-first ordering.
- Recent rows stay inside expanded controls so the live preview does not become a
  dashboard.
- Added `Delete` for app-owned `processed-*` session directories, guarded so it
  refuses paths outside the recordings root.
- Tests cover ordering, limits, artifact summaries, invalid metadata skipping,
  successful cleanup, and refusal to delete outside the app-owned root.

## Done When

- Users can find, inspect, share, and clean up previous evidence artifacts.
- The browser supports validation work without cluttering the live preview.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
