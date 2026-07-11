# Milestone AP - True Live Linear EVM

Status: In progress

Importance: Critical. The live app should visibly magnify reconstructed motion/color across the frame, not merely change the boxed ROI color.

Goal: promote the live GL path to a truthful MIT-style linear EVM renderer with spatial pyramid decomposition, temporal filtering, amplification, and reconstruction.

## Tasks

- [ ] Verify the existing live reconstruction path against a controlled target and identify whether it is using enough spatial structure to produce visible motion magnification.
- [ ] Add explicit Gaussian/Laplacian pyramid levels for live color and slow-motion presets.
- [ ] Apply temporal bandpass state per pyramid level instead of only applying an ROI-derived scalar signal.
- [ ] Reconstruct a full processed frame for Raw, Amplified, Difference, and Split views.
- [x] Add per-level attenuation and gain clamps to reduce halos, clipping, and full-frame flashing.
- [x] Add renderer diagnostics that report active pyramid levels, internal resolution, temporal warmup state, fallback reason, and display FPS.
- [ ] Validate on Pixel 8a with a known motion/color target and store screenshots or exported evidence notes.

## Completed Slice: Live Reconstruction Diagnostics

- Added `GlReconstructionDiagnostics` to `GlFrameStats`.
- `CameraOesRenderer` now reports whether live reconstruction is not requested,
  missing render targets, blocked by half-float support, incomplete, warming,
  ready, or disabled after a GL error.
- Successful live reconstruction reports active pyramid level count and first
  internal pyramid size, for example `3 levels / 540x1200 / ready` on a
  portrait surface.
- Expanded GL debug UI now shows the pyramid diagnostic line directly below
  `GL renderer`.
- Added JVM coverage for diagnostic propagation and summary formatting.

This slice does not prove visible motion magnification yet; it makes the next
Pixel validation run interpretable enough to tell whether the live preview is
showing true pyramid reconstruction or a fallback bridge.

## Completed Slice: Level Attenuation And Delta Clamp

- Added `LivePyramidLevelPolicy` for live reconstruction level gains and a max
  amplified-delta clamp.
- The live reconstruction shader now weights each bandpass level before summing
  the reconstruction delta.
- The shader clamps the amplified delta before final RGB clamping. This reduces
  full-frame flashing and clipping risk while still preserving reconstructed
  spatial changes.
- `CameraOesRenderer` now uploads level gains and max delta uniforms for the
  live reconstruction path.
- Added JVM coverage for policy defaults, invalid policy rejection, and shader
  uniform/source contracts.
- Phone validation was not run for this slice because the phone is currently
  unavailable.

## Completed Slice: Full-Frame Difference Contract

- Pulse Difference mode now requests the live full-frame GL path when GL preview
  is active and timing is healthy.
- The live reconstruction shader accepts `uDifferenceMode` and emits a
  full-frame reconstructed-delta heatmap based on the clamped amplified pyramid
  delta.
- `CameraOesRenderer` no longer blocks live reconstruction solely because the
  view is Difference.
- `VisualizationModel.live` now labels active GL Pulse Difference as
  full-frame difference instead of ROI signal diagnostic.
- Added JVM coverage for the live preview policy, visualization model, and
  shader source contracts.
- Phone validation was not run for this slice because the phone is currently
  unavailable.

## Done When

- Live amplified preview shows visible reconstructed output for at least one color sample and one slow-motion sample.
- The app can explain when it is showing true reconstructed EVM versus a fallback bridge or ROI signal visualization.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.
