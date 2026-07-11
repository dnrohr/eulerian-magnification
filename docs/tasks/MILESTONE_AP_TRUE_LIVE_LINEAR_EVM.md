# Milestone AP - True Live Linear EVM

Status: In progress

Importance: Critical. The live app should visibly magnify reconstructed motion/color across the frame, not merely change the boxed ROI color.

Goal: promote the live GL path to a truthful MIT-style linear EVM renderer with spatial pyramid decomposition, temporal filtering, amplification, and reconstruction.

## Tasks

- [ ] Verify the existing live reconstruction path against a controlled target and identify whether it is using enough spatial structure to produce visible motion magnification.
- [x] Add explicit Gaussian/Laplacian pyramid levels for live color and slow-motion presets.
- [x] Apply temporal bandpass state per pyramid level instead of only applying an ROI-derived scalar signal.
- [x] Reconstruct a full processed frame for Raw, Amplified, Difference, and Split views.
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

## Completed Slice: Breathing Linear EVM Policy

- The live full-frame reconstruction policy now allows Breathing mode in
  Amplified, Difference, and Split views when GL preview timing is healthy.
- The policy label now calls this path `Full-frame linear EVM preview` instead
  of color-only preview.
- High-frequency Tremor/Object modes remain on ROI signal preview until device
  performance and artifacts are validated.
- Added JVM coverage proving Breathing can request the GL reconstruction path
  while high-frequency motion modes remain gated.
- Phone validation was not run for this slice because the phone is currently
  unavailable.

## Completed Slice: Truthful Live Renderer Label

- `VisualizationModel.live` now names the active GL full-frame path `Live
  linear EVM reconstruction` instead of the older `Live GL full-frame color
  bridge`.
- The explicit `GL color bridge` render path remains available for fallback
  diagnostics when reconstruction is requested but unavailable.
- Updated the signal/renderer/visualization architecture table for Pulse and
  Breathing live reconstruction.
- Added JVM coverage for the renamed renderer contract.
- Phone validation was not run for this slice because the phone is currently
  unavailable.

## Completed Slice: Reconstruction Policy Diagnostics

- Live reconstruction diagnostics now include the active per-level pyramid gains
  and amplified-delta clamp uploaded by `CameraOesRenderer`.
- The expanded GL debug line reports the current policy, for example
  `start L0 / gains 0.35/0.75/1.00 / clamp +/-0.18`, alongside pyramid level
  count, internal size, and warmup state.
- Fallback diagnostics remain concise and continue to show the fallback reason
  without policy details.
- Added JVM coverage for reconstruction policy summary formatting.
- Phone validation was not run for this slice because the phone is currently
  unavailable.

## Completed Slice: Laplacian Reconstruction Delta

- The live reconstruction shader now derives Laplacian-style spatial deltas from
  the temporally filtered Gaussian bandpass levels.
- Fine and mid pyramid deltas subtract the next coarser Gaussian bandpass level;
  the coarsest level is preserved as the residual.
- Existing per-level gains and the max delta clamp now apply to those
  Laplacian-style reconstruction deltas instead of a plain sum of blurred
  Gaussian bandpass textures.
- Updated the live level-policy experiment note with the reconstruction formula.
- Added JVM coverage for the shader source contract.
- Phone validation was not run for this slice because the phone is currently
  unavailable.

## Completed Slice: Mode-Specific Reconstruction Profiles

- Added explicit live reconstruction profiles for Pulse color and Breathing /
  slow motion.
- Pulse starts at pyramid level `0` with the fine-level attenuation policy used
  for color magnification.
- Breathing starts at pyramid level `1`, skips fine texture shimmer, uses
  mid/coarse gains, and applies a slightly tighter amplified-delta clamp.
- `ColorMagnificationUniforms` now carries the selected reconstruction profile
  into `CameraOesRenderer`, which uploads the profile gains, clamp, and start
  level instead of using one fixed renderer policy for every mode.
- GL reconstruction diagnostics now report the active start level along with
  gains and clamp, making Pixel validation easier to interpret.
- Added JVM coverage for profile selection, uniform mapping, diagnostics, and
  shader profile contracts.
- Phone validation was not run for this slice because the phone is currently
  unavailable.

## Completed Slice: Raw Reconstruction Passthrough

- Added `reconstructionAmplification` to the GL uniform contract so the
  reconstruction shader can use zero amplification for Raw while preserving the
  selected amplification for Amplified, Difference, and Split.
- Raw Pulse/Breathing live preview can now request the same full-frame GL
  reconstruction pass graph as the other live EVM views.
- Raw remains labeled and exported as raw passthrough; the reconstruction graph
  simply renders the base frame with zero amplified delta.
- Updated the live preview policy reason and architecture mapping to cover
  Raw/Amplified/Difference/Split reconstruction behavior.
- Added JVM coverage for Raw full-frame policy eligibility and the zero
  reconstruction-amplification uniform.
- Phone validation was not run for this slice because the phone is currently
  unavailable.

## Completed Slice: Per-Level Temporal Bandpass Diagnostics

- Live reconstruction diagnostics now report the temporal state level count and
  active frequency band, for example `temporal 3L / band 0.70-3.00Hz`.
- `CameraOesRenderer` fills those diagnostics from the actual GL temporal state
  and current mode band after a successful reconstruction pass.
- Added shader-source coverage showing the reconstruction shader samples
  per-level bandpass textures and does not depend on the ROI scalar
  `uAmplifiedSignal`.
- Added JVM coverage for temporal diagnostic summary formatting.
- Phone validation was not run for this slice because the phone is currently
  unavailable.

## Completed Slice: Controlled Reconstruction Contract

- Added `LiveReconstructionContract`, a local structural validator for the live
  GL reconstruction path.
- The contract checks for at least three pyramid levels, per-level temporal
  targets, Pulse/Breathing reconstruction start levels, per-level bandpass
  texture use, no dependency on the old ROI scalar `uAmplifiedSignal`, and
  Laplacian-style reconstruction deltas.
- Added JVM coverage for the passing current contract and a failing
  under-structured contract.
- Documented the local result in
  `docs/experiments/live_reconstruction_controlled_contract.md`.
- This proves the live path has enough structural machinery to be more than an
  ROI tint/color bridge. It does not prove controlled-target visual output or
  phone-visible magnification quality.
- Phone validation was not run for this slice because the phone is currently
  unavailable.

## Done When

- Live amplified preview shows visible reconstructed output for at least one color sample and one slow-motion sample.
- The app can explain when it is showing true reconstructed EVM versus a fallback bridge or ROI signal visualization.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.
