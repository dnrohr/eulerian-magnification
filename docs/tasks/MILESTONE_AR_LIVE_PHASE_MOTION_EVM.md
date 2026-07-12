# Milestone AR - Live Phase Motion EVM

Status: In progress

Importance: High. Linear EVM is useful, but phase/Riesz magnification is the likely path to convincing subtle motion with fewer blur artifacts.

Goal: bring the recorded Riesz/phase motion renderer into a live-preview path for controlled motion targets.

## Tasks

- [x] Decide the first live phase scope: reduced-resolution full frame or manually selected ROI.
- [ ] Port the recorded phase state update to a live-capable renderer path with bounded memory and latency.
- [ ] Add amplitude gating, phase wrapping, and temporal warmup diagnostics to the live UI/debug metadata.
- [ ] Add live Difference and Split views that compare raw video against phase-reconstructed output.
- [ ] Validate on synthetic moving-edge and `local-euler` samples before phone-camera validation.
- [ ] Validate on Pixel 8a with a controlled object-motion setup and document expected artifacts.

## Done When

- Live Motion mode visibly magnifies a controlled subtle movement target beyond what linear color/tint modes can show.
- The app can fall back gracefully when phase confidence is too low or the device cannot sustain the path.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.

## Completed Slice: Live Phase Scope Decision

- Chose a manually selected, reduced-resolution ROI as the first live phase
  path.
- Documented the decision, constraints, fallback expectations, and validation
  order in `docs/architecture/LIVE_PHASE_SCOPE.md`.
- Deferred full-frame live phase processing until the ROI path proves quality,
  latency, and thermal behavior on Pixel 8a.
- Re-ran the standard JVM/build verification and reinstalled the current debug
  build on the Pixel 8a.

## Completed Slice: Live Phase ROI Plan Contract

- Added `LivePhaseRoiPlan` for the first live phase renderer path.
- The plan computes source ROI pixel size, caps the processing texture to a
  bounded dimension, estimates RGBA16F render-target memory, and reports whether
  the plan fits the live phase budget.
- Added `LivePhaseRoiStatePlan` for the per-pixel phase state targets needed by
  the recorded phase update model: previous phase, unwrapped phase, lowpass,
  highpass, and bandpass.
- Added `LivePhaseWarmupStatus` labels for future live UI/debug diagnostics.
- Added JVM coverage for ROI sizing, aspect preservation, target counts, memory
  budget checks, invalid inputs, and warmup labels.
- This is the bounded live state contract for porting recorded phase state
  updates; the runtime GL renderer allocation and shader invocation remain the
  next AR slice.
- Installed the debug build on the Pixel 8a after focused and full
  JVM/build verification.

## Completed Slice: Live Phase Temporal Shader Contract

- Added `LIVE_PHASE_TEMPORAL_FRAGMENT` to `RieszPhaseShaderSource`.
- The shader contract ports the recorded phase-state update model into a
  live-capable GLSL pass: wrapped phase delta, unwrapped phase accumulation,
  temporal low/high state, high-minus-low bandpass phase, warmup seeding, and
  amplitude-gated amplification.
- Added shader-source coverage for the live temporal state inputs, warmup path,
  wrapped phase update, temporal bandpass, and amplitude gate.
- This is still a contract slice; runtime render-target allocation and
  invocation remain open.
- Installed the debug build on the Pixel 8a after focused shader coverage and
  full JVM/build verification.

## Completed Slice: Live Phase Diagnostics Contract

- Added `LivePhaseDiagnostics` for the future live phase UI/debug metadata.
- Diagnostics summarize whether phase rendering was requested, whether it is
  active, warmup status, ROI processing size, amplitude gate status, and fallback
  reason.
- Added explicit fallback reasons for missing manual ROI, unsupported GL phase
  resources, memory-budget overflow, unhealthy timing, low amplitude, and
  renderer errors.
- Added JVM coverage for inactive, fallback, warming, ready, and low-amplitude
  summaries.
- This is a diagnostics contract; wiring it into `CameraOesRenderer` and the UI
  remains open.
- Installed the debug build on the Pixel 8a after focused diagnostics coverage
  and full JVM/build verification.

## Completed Slice: Live Phase Preview Eligibility Policy

- Added `LivePhasePreviewPolicy` to decide when Motion/Object modes may request
  the future live phase ROI renderer.
- The policy requires GL preview, live phase resources, a manual ROI, a valid
  ROI phase plan, and healthy settled GL timing before enabling the phase path.
- The policy reports `LivePhaseDiagnostics` fallback reasons when phase is not
  requested or cannot run.
- Added JVM coverage for enabled Motion phase, non-motion no-op, missing GL,
  missing manual ROI, unhealthy timing, timing warmup, and unavailable resources.
- Runtime renderer wiring remains open.
- Installed the debug build on the Pixel 8a after focused policy coverage and
  full JVM/build verification.

## Completed Slice: Live Phase ROI Output Shader Contract

- Added `LIVE_PHASE_COMPOSE_FRAGMENT` to `RieszPhaseShaderSource`.
- The shader contract composites a phase-reconstructed ROI into raw full-frame
  context and keeps pixels outside the manual ROI raw.
- The contract supports live phase Amplified, Difference, and Split output modes:
  Amplified inserts reconstructed grayscale phase output inside the ROI,
  Difference emits a signed heatmap inside the ROI, and Split compares raw
  versus reconstructed phase output.
- Added shader-source coverage for ROI containment, ROI texture mapping, raw
  context preservation, signed Difference colors, and Split behavior.
- Runtime renderer wiring remains open.
- Installed the debug build on the Pixel 8a after focused shader coverage and
  full JVM/build verification.

## Completed Slice: Live Phase Diagnostics Stats Path

- Extended `ColorMagnificationUniforms` to carry `LivePhaseDiagnostics`.
- `ColorMagnificationParameters` can now accept a `LivePhasePreviewDecision`
  and preserve its diagnostics for the GL renderer.
- `GlFrameStats` now carries live phase diagnostics alongside live linear
  reconstruction diagnostics.
- `CameraOesRenderer` emits the current live phase diagnostics in frame stats,
  giving the future UI/debug metadata a real renderer stats path.
- Added JVM coverage for uniform mapping and frame-stat propagation.
- Runtime phase rendering and UI display remain open.
- Installed the debug build on the Pixel 8a after focused stats coverage and
  full JVM/build verification.

## Completed Slice: Live Phase Preview Runtime Decision Wiring

- `GlFrameStats` now carries the current GL surface size reported by
  `CameraOesRenderer`.
- `MainActivity` evaluates `LivePhasePreviewPolicy` from the live settings,
  GL preview state, renderer stats, surface size, and manual ROI.
- `CameraGlPreview` passes the current `LivePhasePreviewDecision` into
  `ColorMagnificationParameters`, so renderer stats receive real live phase
  diagnostics for Motion/Object modes instead of the default not-requested
  placeholder.
- Runtime phase rendering and UI display remain open.
- Installed the debug build on the Pixel 8a after focused policy/timer coverage
  and full JVM/build verification.

## Completed Slice: Live Phase Debug Status Display

- The expanded GL debug overlay now displays the live phase diagnostics summary
  from `GlFrameStats`.
- Motion/Object modes can now report phase not-requested, missing ROI,
  unsupported GL resources, timing fallback, warmup, and processing size in the
  same debug area as the live pyramid renderer.
- Runtime phase rendering remains open.
- Installed the debug build on the Pixel 8a after full JVM/build verification.

## Completed Slice: Live Phase Temporal Pass Compatibility

- Split the live phase temporal shader contract into a four-output state-update
  pass and a separate amplified-phase pass.
- This keeps the temporal update within the GLES 3.0 guaranteed color
  attachment count before runtime renderer wiring depends on it.
- The new amplified-phase pass applies the recorded renderer's high-minus-low
  phase bandpass and amplitude gate before reconstruction.
- Runtime phase rendering remains open.
- Installed the debug build on the Pixel 8a after focused shader coverage and
  full JVM/build verification.

## Completed Slice: Live Phase Render Graph Plan

- Added `LivePhaseRenderGraphPlan` to lock down the runtime pass order before
  renderer wiring: ROI extract, Riesz components, phase projection, temporal
  state update, amplified phase, ROI reconstruction, and full-frame compose.
- Corrected the ROI memory budget to include eight ping-pong temporal state
  targets plus five working targets.
- Added coverage that the temporal state pass is the only four-output pass and
  stays within the GLES 3.0 guaranteed color attachment count.
- Runtime phase rendering remains open.
- Installed the debug build on the Pixel 8a after focused graph/ROI/shader
  coverage and full JVM/build verification.

## Completed Slice: Live Phase ROI State Allocation Path

- `ColorMagnificationUniforms` now carries the selected `LivePhaseRoiPlan` into
  the GL renderer alongside live phase diagnostics.
- Added `LivePhaseRoiState`, which allocates the bounded ROI phase state:
  ping-pong wrapped phase, unwrapped phase, lowpass, and highpass targets plus
  the working ROI/Riesz/project/amplify/reconstruct textures.
- `CameraOesRenderer` now prepares and releases the live phase ROI state from
  the policy decision, avoids allocation during policy fallback, and reports
  renderer errors through phase diagnostics.
- Runtime phase shader invocation and visible phase output remain open.
- Installed the debug build on the Pixel 8a after focused uniform/ROI/graph/
  shader coverage and full JVM/build verification.

## Completed Slice: Live Phase ROI Extraction Pass

- Added `LIVE_PHASE_EXTRACT_ROI_FRAGMENT` to map the reduced processing texture
  back into the selected full-frame manual ROI.
- `CameraOesRenderer` now compiles the ROI extraction program and invokes it
  into `LivePhaseRoiState.extractedRoi` whenever live phase is requested and
  eligible.
- Renderer fallback diagnostics now report an error if the source full-frame GL
  texture is unavailable during the extraction path.
- Riesz component, phase projection, temporal update, reconstruction, and
  visible phase compose invocation remain open.
- Installed the debug build on the Pixel 8a after focused shader/uniform
  coverage and full JVM/build verification.
