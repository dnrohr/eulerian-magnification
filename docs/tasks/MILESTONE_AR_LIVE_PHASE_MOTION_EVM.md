# Milestone AR - Live Phase Motion EVM

Status: In progress

Importance: High. Linear EVM is useful, but phase/Riesz magnification is the likely path to convincing subtle motion with fewer blur artifacts.

Goal: bring the recorded Riesz/phase motion renderer into a live-preview path for controlled motion targets.

## Tasks

- [x] Decide the first live phase scope: reduced-resolution full frame or manually selected ROI.
- [x] Port the recorded phase state update to a live-capable renderer path with bounded memory and latency.
- [x] Add amplitude gating, phase wrapping, and temporal warmup diagnostics to the live UI/debug metadata.
- [x] Add live Difference and Split views that compare raw video against phase-reconstructed output.
- [x] Validate on synthetic moving-edge and `local-euler` samples before phone-camera validation.
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

## Completed Slice: Live Phase Riesz Component Pass

- `CameraOesRenderer` now compiles the Riesz component shader for the live phase
  path.
- After ROI extraction, the renderer invokes the Riesz pass into
  `LivePhaseRoiState.rieszComponents`, using the reduced ROI texture size for
  central-difference texel spacing.
- Phase projection, temporal update, reconstruction, and visible phase compose
  invocation remain open.
- Installed the debug build on the Pixel 8a after focused shader coverage and
  full JVM/build verification.

## Completed Slice: Live Phase Projection Pass

- Added `LIVE_PHASE_PROJECT_FRAGMENT`, a live shader variant that derives local
  orientation from each ROI pixel's Riesz x/y response instead of requiring a
  CPU-side dominant-orientation reduction.
- `CameraOesRenderer` now compiles and invokes the phase projection pass after
  ROI extraction and Riesz component generation.
- The live renderer now produces `LivePhaseRoiState.projectedPhase` for the
  upcoming temporal state update.
- Temporal update, reconstruction, and visible phase compose invocation remain
  open.
- Installed the debug build on the Pixel 8a after focused shader coverage and
  full JVM/build verification.

## Completed Slice: Live Phase Temporal State Update Pass

- `CameraOesRenderer` now compiles and invokes the live phase temporal shader
  after ROI extraction, Riesz component generation, and phase projection.
- The live phase path now ping-pongs wrapped phase, unwrapped phase, lowpass,
  and highpass textures in `LivePhaseRoiState`.
- Phase diagnostics now advance from warmup to ready after the ROI temporal
  state has a previous phase frame.
- Reconstruction, amplitude-gated amplification, and visible phase compose
  invocation remain open.
- Installed the debug build on the Pixel 8a after focused shader/graph coverage
  and full JVM/build verification.

## Completed Slice: Live Phase Amplify And Reconstruct Passes

- `CameraOesRenderer` now compiles and invokes the live phase amplification
  shader after temporal state update.
- The amplification pass applies the recorded phase renderer's high-minus-low
  temporal phase band and amplitude threshold before writing
  `LivePhaseRoiState.amplifiedPhase`.
- The renderer now reconstructs amplified phase into
  `LivePhaseRoiState.reconstructedRoi`, ready for the upcoming full-frame
  compose pass.
- Visible phase compose invocation remains open.
- Installed the debug build on the Pixel 8a after focused shader/graph coverage
  and full JVM/build verification.

## Completed Slice: Live Phase Full-Frame Compose Output

- `CameraOesRenderer` now compiles and invokes the live phase compose shader
  after ROI reconstruction.
- The reconstructed phase ROI is composited into the existing full-frame
  `processedRenderTarget`, so the preview and processed recording path can use
  the live phase output without a new surface.
- Difference view maps to the phase difference heatmap; Split view uses the
  existing raw-left/processed-right display with the phase-composited target on
  the processed side.
- Synthetic, recorded-sample, and Pixel validation remain open before declaring
  visible live phase magnification complete.
- Installed the debug build on the Pixel 8a after focused shader/graph coverage
  and full JVM/build verification.

## Completed Slice: Synthetic And Local Euler Validation

- Re-ran the JVM parity gate for synthetic samples:
  `ParityHarnessTest`, `RecordedEvmParityValidatorTest`, and
  `ParityHarnessArtifactWriterTest`.
- The synthetic moving-edge sample still routes Motion amplified/split views
  through the recorded Riesz phase renderer with nonzero changed-pixel metrics.
- Verified the local `euler.mp4` hash:
  `BF549FEAA994104817A6AFCC39037FB80A013D4074E0AC00EC167F4471B0ACBF`.
- Ran the connected Pixel 8a parity harness for `local-euler` using the Android
  media decoder and generated artifacts under
  `/sdcard/Download/eulerian-parity-output/local-euler/`.
- The generated `local-euler` manifest reported `36` decoded frames at `51x90`.
  Amplified and Split outputs had `meanAbsDelta=12.500363`,
  `changedPixelFraction=0.710899`, and `clippedPixelFraction=0.000000`.
- Detailed metrics are recorded in
  `docs/experiments/recorded_full_frame_evm_parity_validation.md`.
- Live Pixel camera validation with a controlled object-motion setup remains
  open.

## Completed Slice: Pixel Live Phase Validation Protocol

- Added `docs/experiments/pixel8a_live_phase_validation.md`.
- The protocol defines the controlled object-motion setup, app settings,
  expected live phase diagnostics, pass/fail visual criteria, acceptable
  artifacts, failure artifacts, and capture commands for screen recording and
  logcat evidence.
- This does not complete the Pixel validation task; a physical moving target in
  front of the camera is still required.
- Ran a Pixel 8a launch smoke check after granting camera permission. The camera
  stream opened and recent logcat contained no fatal exception or GL error.

## Supporting Slice: Live Evidence Capture Tool

- Added `tools/capture_live_validation_evidence.ps1` and linked it from the
  Pixel live phase validation protocol.
- The tool captures a screenshot, optional screen recording, logcat, gfxinfo,
  thermal state, battery state, focused window, and manifest into ignored
  `sample-videos/exports/live-validation/` bundles.
- This standardizes the artifact capture path for the controlled object-motion
  run, but the AR visual validation checkbox remains open until a moving target
  is visible and inspected.

## Supporting Slice: Scripted Phase Launch

- Added validation launch extras and capture-script parameters for mode, view,
  ROI source, GL preview, controls visibility, and amplification.
- The live phase protocol now launches directly into Fast Motion/Split/Manual/GL
  setup before recording evidence.
- Overrides are not persisted unless explicitly requested, keeping scripted
  validation isolated from normal app settings.

## Supporting Slice: Full-Frame Phase Guard

- Full-frame ROI no longer enables live phase motion preview. It now reports
  `phase fallback: full-frame phase not yet supported` until a validated
  reduced/full-frame phase renderer exists.
- GL frame diagnostics now label the active live phase render path when bounded
  ROI phase rendering is actually running.
- Pixel 8a evidence showed the camera HAL still delivering about 30 FPS in
  full-frame mode, while expanded controls had poor UI frame pacing: 91.67%
  janky frames with a 48 ms median frame time.
- Hiding controls improved the same full-frame launch to 59.57% janky frames
  with a 22 ms median frame time, so overlay cost remains a separate follow-up
  from live phase eligibility.
- Evidence bundles:
  `sample-videos/exports/live-validation/20260712-155744-full-frame-phase-disabled`
  and
  `sample-videos/exports/live-validation/20260712-155819-full-frame-hidden-controls-fps`.

## Next Gate For Manual ROI As Non-Default

- Run the controlled Pixel object-motion setup from
  `docs/experiments/pixel8a_live_phase_validation.md`.
- If live phase visibly magnifies the controlled target with acceptable
  diagnostics, proceed to Milestone AU and make Manual ROI a selectable option
  rather than the default motion setup.
- If the result only works with a carefully hand-picked box or has confusing
  artifacts, add a reduced/full-frame phase renderer slice before changing the
  default ROI source.
