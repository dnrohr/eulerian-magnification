# Milestone V - Live Side-by-Side Comparison

Importance: Very high. Raw-vs-processed comparison is the clearest way to judge whether the live effect is real.

Goal: add an easy live comparison view that shows raw input beside processed output.

## Tasks

- [x] Decide whether the first implementation belongs in CameraX overlay, GL preview, or both.
- [x] Add a live side-by-side view mode that keeps framing stable and readable.
- [x] Ensure Raw, Amplified, Difference, and Split semantics remain truthful.
- [x] Verify the comparison view on phone in portrait orientation.
- [x] Update README/UI reference with the comparison workflow.
- [x] Add tests for any layout or renderer state logic.

## Completed Slice

- The first implementation uses the GL preview path because it already renders raw and processed frames as separate render targets.
- Selecting `Split` now automatically uses GL preview when GL is available.
- CameraX remains available for Raw, Amp, and Diff; Split keeps truthful live raw-vs-processed semantics by using GL.
- Added `PreviewPathPolicyTest` for the preview-path selection logic.
- Installed the debug APK on connected device `47091JEKB05516` and verified Split renders a portrait live side-by-side view with a non-sensitive room target.

## Done When

- Users can compare raw and processed live output without exporting a video.
- The comparison does not obscure ROI selection or quality labels.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
