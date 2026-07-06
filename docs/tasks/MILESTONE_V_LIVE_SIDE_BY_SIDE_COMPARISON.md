# Milestone V - Live Side-by-Side Comparison

Importance: Very high. Raw-vs-processed comparison is the clearest way to judge whether the live effect is real.

Goal: add an easy live comparison view that shows raw input beside processed output.

## Tasks

- [ ] Decide whether the first implementation belongs in CameraX overlay, GL preview, or both.
- [ ] Add a live side-by-side view mode that keeps framing stable and readable.
- [ ] Ensure Raw, Amplified, Difference, and Split semantics remain truthful.
- [ ] Verify the comparison view on phone in portrait orientation.
- [ ] Update README/UI reference with the comparison workflow.
- [ ] Add tests for any layout or renderer state logic.

## Done When

- Users can compare raw and processed live output without exporting a video.
- The comparison does not obscure ROI selection or quality labels.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
