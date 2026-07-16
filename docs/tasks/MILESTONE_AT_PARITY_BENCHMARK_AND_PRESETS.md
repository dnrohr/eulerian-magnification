# Milestone AT - Parity Benchmark And Presets

Status: In Progress

Importance: Medium-high. Once the real renderer paths exist, users need reliable presets and the project needs benchmark gates before claiming parity.

Goal: turn parity validation into supported app presets with Pixel 8a performance, thermal, and setup guidance.

## Tasks

- [x] Define locked presets for pulse color, breathing/slow motion, object vibration, and fast tremor with known frequency bands and amplification limits.
- [x] Add preset-specific setup guidance that tells users what target, lighting, support, and distance are needed to see the effect.
- [x] Benchmark each preset on Pixel 8a for preview FPS, dropped frames, latency, thermal state, and recording stability.
- [x] Add warnings when selected frequencies are too close to each other, too high for the measured FPS, or likely to overlap with camera motion/heartbeat artifacts.
- [x] Document why tremor and object-vibration bands overlap and when the setup, target, and renderer choice matter more than the label.
- [x] Update README and parity docs with the presets that are actually validated.
- [ ] Capture watched Pulse color visual parity evidence with the live linear protocol.
- [ ] Capture watched Breathing visual parity evidence with the live linear protocol.
- [ ] Capture watched Object vibration visual parity evidence with the live phase protocol.
- [ ] Capture watched Fast tremor visual parity evidence with the live phase protocol.
- [ ] Update README and parity docs only after watched visual artifacts are accepted.

## Completed Slice: Locked Presets And Warnings

- Added a tested `ParityPreset` model for Pulse color, Breathing, Object vibration, and Fast tremor.
- Connected demo buttons to the locked preset settings and added an Object demo preset.
- Added preset setup guidance for target, lighting, support, distance, and expected output.
- Added runtime warnings for low measured FPS relative to the selected band and for overlapping high-frequency motion bands.
- Updated README and MIT parity docs with locked preset definitions and the overlap explanation; validation-specific claims remain pending.

## Completed Slice: Pixel Preview Benchmark Artifact

- Added `ParityPresetBenchmarkReport` CSV/JSON artifact generation.
- Added `ParityPresetBenchmarkInstrumentedTest`, which launches each locked
  preset on device and records `dumpsys gfxinfo` frame/jank percentiles plus
  thermal status.
- Extended the benchmark with a per-preset processed-recording metadata probe:
  30 monotonic samples, zero dropped-frame estimate, and `metadata ok` for all
  four presets on the 2026-07-12 Pixel run.
- Extended the benchmark with a per-preset encoded MP4 probe. All four presets
  produced valid MP4 exports with required atoms on the 2026-07-12 Pixel run.
- Ran the benchmark on Pixel 8a and wrote artifacts under
  `/sdcard/Download/eulerian-preset-benchmark`.
- Documented the 2026-07-12 short-run results in
  `docs/experiments/pixel8a_parity_preset_benchmark.md`.

## Completed Slice: Validated Preset Docs

- Updated README and MIT parity docs with a validation-status table for each
  locked preset.
- Documented that all four presets have Pixel 8a short-run performance,
  recording metadata, and encoded MP4 validation.
- Kept visual parity marked unvalidated for all presets until each has a
  watched target setup and known-good visual artifact.

## Supporting Slice: Live Evidence Thermal Summary

- Live validation summaries now parse `thermalservice.txt` and report Android
  thermal status, maximum sensor status, maximum temperature, and hottest
  sensor name.
- Summaries warn when Android thermal status or any sensor status reaches
  `2` (`moderate`) or higher. These warnings keep runtime smoke separate from
  thermal qualification: a run can remain crash-free while still being too hot
  to treat as clean visual/performance evidence.
- This strengthens the watched preset evidence flow, but does not replace the
  remaining known-target visual artifacts.

## Supporting Slice: Camera HAL FPS Summary

- Live validation summaries now parse Pixel camera HAL `FPS:` lines from scoped
  logcat and report camera cadence sample count, average FPS, min FPS, and max
  FPS.
- Summaries warn when the minimum observed HAL cadence drops below `23.5 FPS`,
  matching the app's low-camera-cadence quality threshold.
- This fills the previously missing live camera dropped-frame diagnosis field
  for watched preset evidence, while keeping visual parity marked unvalidated
  until known-target artifacts exist.

## Supporting Slice: Battery Context Summary

- Live validation summaries now parse `battery.txt` and report battery level,
  battery temperature, charging state, and external power source.
- Summaries warn when the device is externally powered or battery temperature is
  at least `40 C`. This makes USB charging and warm-battery conditions explicit
  when reviewing Pixel performance and visual-validation artifacts.
- These warnings are advisory and do not replace known-target visual parity
  artifacts.

## Supporting Slice: Live Thermal Quality Warning

- The live quality evaluator now consumes Android thermal state and shows
  `Thermal high` when `PowerManager.currentThermalStatus` is `moderate` or
  worse.
- The warning uses the same threshold as validation-summary thermal warnings,
  so users see the issue before trusting a hot-device capture.
- Quality cues treat thermal warnings as user-visible regressions, while
  evidence summaries continue to record detailed thermal sensor state after
  capture.

## Supporting Slice: Thermal Preflight Evidence

- Live validation captures now record `thermalservice_preflight.txt` before
  clearing logcat or launching the app.
- Capture manifests and summaries expose `thermalPreflight`, so a low-FPS or
  frozen-preview run can be rejected when the phone was already throttled before
  the app started.
- Preflight thermal warnings are advisory for runtime smoke, but `critical` or
  worse preflight state blocks app launch by default and produces a
  `thermal_preflight_aborted` summary verdict unless `-AllowThermalLaunch` is
  passed.
- Aborted bundles are useful evidence that the phone was not in a valid state
  for preset benchmarking, full-frame FPS, apparent camera freeze, or visual
  parity.
- Added `tools/test_live_validation_summary.ps1` to verify that thermal-aborted
  bundles receive `thermal_preflight_aborted` with exit code `4`, while
  incomplete non-aborted runtime bundles still fail as `runtime_failed`.
- Added `tools/wait_for_device_thermal_ready.ps1` so watched preset and
  full-frame validation runs can wait until both Android thermal status and max
  sensor status are below the chosen threshold before launching the app. The
  helper requires consecutive ready samples by default so one transient thermal
  dip does not start a watched validation run too early.
- `tools/capture_live_validation_evidence.ps1` can now run that wait directly
  with `-WaitForThermalReady`, attach `thermal_ready_wait.json`, and abort
  before launch if readiness is not reached.
- `evidence_summary.json` now embeds the parsed thermal wait result as
  `thermalReadyWait`, so reviewers can audit readiness without opening a
  separate JSON artifact.

## Supporting Slice: Visual Review Metadata

- Live validation summaries now carry watched-run visual-review metadata:
  target description, visual claim, target visibility, operator validation
  result, operator notes, and `countsAsVisualValidation`.
- Known-good preset artifacts should set `TargetVisible=true` and
  `VisualValidated=true` only after the target and expected output are inspected.
- Summaries now include `evidenceVerdict`, so preset evidence can be separated
  into runtime smoke, target-visible-but-unvalidated, and visually validated
  bundles.
- This keeps preset benchmark/runtime smoke evidence separate from visual parity
  claims until a watched target run explicitly validates the result.

## Supporting Slice: Dirty Source Evidence Warning

- Live validation summaries now warn when the capture manifest reports a dirty
  source worktree.
- Dirty captures remain useful for validating in-progress scripts or fixes, but
  should not be treated as release-quality preset or visual-parity evidence
  until repeated from a clean commit.
- The summary self-test now covers the dirty-source warning alongside thermal
  aborts, incomplete runtime bundles, and required UI-text failures.

## Supporting Slice: Strict Visual Evidence Gates

- Live validation summaries now accept `-RequireCleanSource` and
  `-RequireVisualValidation`.
- `-RequireCleanSource` exits `6` when source metadata is missing or the capture
  came from a dirty worktree.
- `-RequireVisualValidation` exits `5` when a runtime-smoke bundle has not been
  explicitly accepted as visual validation.
- `tools/capture_live_validation_evidence.ps1` forwards both gates when
  `-Summarize` is used, so watched preset commands can fail loudly unless they
  produce clean, visually accepted evidence.
- The summary self-test covers both gates with synthetic bundles. This improves
  the remaining preset/ROI/live-renderer validation workflow without requiring
  the physical Pixel during this tooling slice.

## Supporting Slice: Warning-Free Evidence Gate

- Live validation summaries now accept `-RequireNoWarnings`.
- The no-warnings gate exits `7` when advisory warnings are present before the
  gate adds its own failure warning.
- `tools/capture_live_validation_evidence.ps1` forwards this gate during
  `-Summarize`, so final known-good preset artifacts can require clean source,
  accepted visual validation, and zero warnings in one command.
- The summary self-test covers a visually accepted, clean-source synthetic
  bundle that still fails the no-warnings gate because it contains an explicit
  warning. This helps separate exploratory diagnostics from release-quality
  visual evidence without needing the physical Pixel for this slice.

## Supporting Slice: Complete Warning-Free Gate Coverage

- The no-warnings gate now evaluates after ROI measurement and screenshot
  content/orientation checks add their warnings.
- This prevents final known-good evidence from passing `-RequireNoWarnings`
  when a late artifact-derived warning, such as wrong screenshot orientation,
  was generated during summarization.
- The summary self-test now covers a landscape screenshot bundle to verify that
  late screenshot warnings are included in the no-warnings gate.

## Supporting Slice: Verdict-Based Visual Gate Warning

- `-RequireVisualValidation` now adds its warning based on the final
  `evidenceVerdict`, not only the operator-entered visual-review fields.
- This means an operator-accepted bundle still fails loudly when the summary
  verdict is `wrong_orientation`, `screenshot_blank`, `runtime_failed`, or any
  other non-visual-validation state.
- The summary self-test covers an accepted but landscape-oriented bundle to
  verify that the final verdict remains authoritative.

## Supporting Slice: Aborted Evidence Gate Consistency

- The capture script now uses one shared summary invocation path for normal and
  prelaunch-aborted bundles.
- Thermal/preflight-aborted summaries record requested strict gates under
  `requiredGates`, while preserving exit code `4` for the abort itself.
- The shared summary helper stores the child summary exit code separately from
  emitted summary text so normal captures still propagate a numeric result.
- The summary self-test now covers gated aborted bundles so future Pixel
  validation scripts cannot lose gate metadata when the phone is too hot to
  launch.

## Supporting Slice: Required Evidence Verdict Gate

- Live validation summaries now accept `-RequireEvidenceVerdict` for exact
  verdict assertions such as `target_visible_unvalidated` or
  `visual_validated`.
- `tools/capture_live_validation_evidence.ps1` forwards the verdict requirement
  when `-Summarize` is used, so future Pixel commands can distinguish setup
  review from final accepted visual evidence.
- A mismatch exits `9` and records the expected and actual verdict under
  `requiredGates.evidenceVerdict`.
- The summary self-test covers both a matching target-visible exploratory
  bundle and a mismatched final-visual requirement. This strengthens the
  remaining watched preset evidence flow without needing the physical Pixel for
  this slice.

## Supporting Slice: Required Diagnostic Category Gates

- Live validation summaries now accept `-RequireRendererDiagnostics` and
  `-RequirePhaseDiagnostics`.
- Renderer diagnostics require at least one parsed `uiDump.rendererLabels`
  entry, and phase diagnostics require at least one parsed
  `uiDump.phaseLabels` entry.
- `tools/capture_live_validation_evidence.ps1` forwards both requirements when
  `-Summarize` is used, so future AP/AE/AR Pixel evidence can require the
  relevant debug category without relying only on free-form UI text matches.
- Missing required diagnostic categories exit `10` and record label counts
  under `requiredGates`. The summary self-test covers passing renderer/phase
  gates and a missing phase-diagnostics failure.

## Supporting Slice: Required Screenrecord Gate

- Live validation summaries now accept `-RequireScreenrecord` for watched
  motion-validation runs where a still screenshot is not enough evidence.
- The summary records `screenrecord.mp4` presence, byte count, non-empty
  status, and MP4 signature detection under both `artifacts.screenrecord` and
  `requiredGates.screenrecord`.
- `tools/capture_live_validation_evidence.ps1` forwards the requirement when
  `-Summarize` is used, so future AP/AR/AT Pixel runs can require a video
  artifact in the same command that captures visual-review metadata.
- Missing, empty, or invalid required recordings exit `11`. The summary
  self-test covers missing and invalid screenrecord failures plus a synthetic
  MP4-signature pass without needing the physical Pixel for this slice.

## Supporting Slice: Required Thermal-Ready Gate

- Live validation summaries now accept `-RequireThermalReady` so final Pixel
  visual evidence can prove the thermal wait helper actually reported
  `ready=true` before launch.
- `tools/capture_live_validation_evidence.ps1` forwards the requirement when
  `-Summarize` is used. This pairs with `-WaitForThermalReady` in final AP/AR/AT
  capture commands.
- Missing or not-ready `thermal_ready_wait.json` artifacts exit `12` and record
  readiness state under `requiredGates.thermalReady`.
- The summary self-test covers missing, failed, and passing thermal-readiness
  artifacts without needing the physical Pixel for this slice.

## Supporting Slice: Required Camera-FPS Gate

- Live validation summaries now accept `-RequireCameraFps` so final Pixel
  evidence can require scoped camera HAL `FPS:` samples instead of treating
  missing cadence data as acceptable.
- The gate fails when no FPS samples are found or when the minimum parsed sample
  is below `-WarnCameraFps` (`23.5 FPS` by default), then records sample count,
  minimum FPS, threshold, and pass/fail state under `requiredGates.cameraFps`.
- `tools/capture_live_validation_evidence.ps1` forwards the requirement when
  `-Summarize` is used. This pairs with `-RequireNoWarnings` for final AP/AR/AT
  live-preview captures where frozen or low-FPS camera evidence should not
  pass.
- Missing or low camera-FPS evidence exits `13`. The summary self-test covers
  missing, low, and passing camera-FPS bundles without needing the physical
  Pixel for this slice.

## Supporting Slice: Required Focused-App Gate

- Live validation summaries now accept `-RequireFocusedApp` so final captured
  evidence can prove the app package was present in `window_focus.txt`.
- The gate fails when the focused-window artifact is missing or when the
  expected package is not found, then records artifact presence, expected
  package, package visibility, and pass/fail state under
  `requiredGates.focusedApp`.
- `tools/capture_live_validation_evidence.ps1` forwards the requirement when
  `-Summarize` is used. This helps future AP/AR/AT evidence fail if the capture
  accidentally records the launcher, permission UI, or another foreground app.
- Missing or wrong focused-app evidence exits `14`. The summary self-test covers
  missing, wrong-package, and passing focused-window bundles without needing the
  physical Pixel for this slice.

## Supporting Slice: Final Visual Evidence Profile

- Live validation summaries now accept `-RequireFinalVisualEvidence`, a
  composite gate for final watched visual evidence.
- The profile expands to the standard closing gates: clean source, accepted
  visual validation, warning-free summary, MP4 screenrecord, thermal-ready
  evidence, camera-FPS evidence, and focused-app evidence.
- `tools/capture_live_validation_evidence.ps1` forwards the profile when
  `-Summarize` is used, so future AP/AR/AT final capture commands do not need
  to repeat every strict gate by hand.
- The summary self-test covers both an incomplete bundle that records all
  required component gates and a complete synthetic final-evidence bundle that
  passes the composite profile without needing the physical Pixel for this
  slice.

## Supporting Slice: Scripted Launch Parameter Guards

- `tools/capture_live_validation_evidence.ps1` now validates scripted launch
  strings for mode, view, ROI source, and controls panel before ADB launch.
- This catches mistyped Pixel validation commands before they produce misleading
  evidence bundles.
- Added `tools/test_live_validation_capture_contract.ps1` so those accepted
  value sets and invalid-value failures are covered without a connected device.
- Added `tools/test_live_validation_tooling.ps1` as the aggregate offline
  validation-tooling suite for capture contract and evidence-summary checks.
- Added `tools/test_offline_project_tooling.ps1` as the broader phone-free
  project tooling suite, covering live-validation tooling and roadmap status
  auditing in one command.
- The broader suite now also runs the real roadmap status audit with
  `-FailOnMismatch`, so task-index drift fails the main phone-free check.
- Added a GitHub Actions workflow that runs the phone-free tooling suite on
  pushes to `main` and pull requests.
- Added `tools/test_github_workflows.ps1` so the offline CI workflow trigger,
  runner, shell, and suite target are covered by the phone-free test command.
- Added `tools/test_live_validation_protocol_docs.ps1` so the ROI, live linear,
  and live phase validation guides cannot quietly drop required final evidence
  gates before the next Pixel visual-validation pass.
- Added `tools/show_next_pixel_validation_plan.ps1` and
  `tools/test_pixel_validation_plan.ps1` so the next connected Pixel session has
  an ordered checklist that covers every remaining in-progress milestone.
- Extended the Pixel validation plan with command templates for setup and final
  evidence captures, keeping the same strict gates as the protocol docs.
- Strengthened the Pixel validation plan self-test so every generated capture
  command parses as PowerShell and only uses parameters exposed by
  `tools/capture_live_validation_evidence.ps1`.
- Added `tools/summarize_pixel_validation_closeout.ps1` and
  `tools/test_pixel_validation_closeout.ps1` so accepted `evidence_summary.json`
  bundles can be mapped back to the remaining ROI, live-linear, live-phase, and
  preset visual-validation slots before editing roadmap or parity docs.
- The closeout script now exits cleanly on success and supports a tested
  `-FailOnMissing` gate for future release or documentation closeout checks.
- The closeout script now reports accepted final evidence bundles that do not
  match any roadmap closeout slot, preventing accepted-but-unclassified Pixel
  artifacts from being missed during review.
- Added a tested `-FailOnUnmatched` closeout gate so unclassified accepted
  evidence can fail automation separately from missing closeout slots.
- Added a tested `-FailOnAmbiguous` closeout gate so a single accepted bundle
  cannot silently satisfy multiple roadmap closeout slots.
- Added a tested `-FailOnDuplicate` closeout gate so multiple accepted bundles
  for the same roadmap closeout slot require explicit review.
- Added a tested `-FailOnCloseoutNotReady` one-shot gate and
  `allCloseoutEvidenceClean` summary field for automation that wants one
  roadmap-closeout readiness check.
- Added a tested `-FailOnPresetDocsNotReady` closeout gate so README/parity doc
  updates can be blocked until all four preset visual slots are accepted.
- Tightened `-FailOnPresetDocsNotReady` so preset doc updates also fail on
  unmatched, ambiguous, or duplicate accepted evidence, even when all four
  preset slots are present.
- Split the closeout summary's preset readiness signal into
  `presetVisualSlotsPresent` and `presetDocsEvidenceClean`, and made
  `readyForPresetDocs` mean the stricter doc-update readiness condition.
- Added source branch and commit fields to satisfied closeout slots and
  unmatched/ambiguous/duplicate accepted-evidence reports, so final parity docs
  can cite the exact app build that produced each accepted artifact.
- Added non-`main` accepted-evidence reporting to the closeout summary and made
  the one-shot closeout and preset-doc readiness gates reject accepted evidence
  captured outside `main`.
- Added a tested `-FailOnNonMain` closeout gate so automation can reject
  off-`main` accepted evidence directly.
- Added `-FailOnNonMain` to the `preset-parity-closeout` planner command, with
  self-test coverage to keep the printed Pixel closeout checklist aligned.
- Updated the combined closeout self-test to include `-FailOnNonMain`, matching
  the explicit gate set documented for roadmap closeout.
- Fixed the roadmap status text summary to exit `0` on success and added a
  regression check so stale PowerShell exit codes do not fail CI after passing
  phone-free tests.
- Added unpushed-source reporting and `-FailOnUnpushedSource` to the Pixel
  closeout summary so accepted final evidence must come from a commit reachable
  from `origin/main`, not just a local `main` checkout.
- Extended the protocol-doc self-test to require the pushed-source closeout
  gate in operator-facing docs, preventing the stricter evidence requirement
  from drifting out of the next Pixel handoff instructions.
- Capture manifests now record whether the captured commit is reachable from
  `origin/main`; summaries warn when it is not, so final warning-free watched
  evidence cannot quietly come from a local-only commit.
- Added capture-contract coverage for the source reachability manifest field,
  so phone-free tests fail if future capture-script edits drop the pushed-source
  metadata needed by final evidence review.
- Updated the ROI, live linear, and live phase protocols to require
  `source.commitReachableFromOriginMain=true` in final accepted summaries, with
  protocol-doc self-test coverage to keep that acceptance criterion visible.
- Updated the generated Pixel validation plan to call out the same
  `origin/main` source reachability requirement in each final evidence group,
  with planner self-test coverage so the handoff checklist stays aligned.
- Live validation summaries now include SHA-256 hashes for present screenshots
  and screen recordings, giving final accepted evidence durable artifact
  identifiers for README/parity-doc closeout notes.
- Pixel validation closeout now carries those screenshot and screenrecord
  SHA-256 values into satisfied slots and flagged accepted-evidence reports, so
  the final roadmap/README parity update can cite artifacts directly from one
  closeout summary.
- Added `-FailOnMissingArtifactHashes` to the Pixel closeout summary and
  planner/docs gates, so older accepted summaries without screenshot and
  screenrecord SHA-256 values cannot close roadmap or parity documentation.
- Added `-FailOnNonFinalLabel` to the Pixel closeout summary and planner/docs
  gates, so accidentally accepted setup-labeled bundles cannot satisfy final
  roadmap or preset parity slots.
- Added `-FailOnWrongSlotLabel` to the Pixel closeout summary and planner/docs
  gates, so a final-labeled bundle can only satisfy the matching manual ROI,
  automatic ROI, Pulse, Breathing, Object phase, or Fast tremor closeout slot.
- Added final-evidence operator-note enforcement to live summaries and Pixel
  closeout, so accepted visual evidence must explain what the operator
  accepted before it can close roadmap or parity documentation.
- Added final-evidence visual-review text enforcement to live summaries and
  Pixel closeout, so accepted visual evidence must include the watched target
  description and visual claim before it can close roadmap or parity
  documentation.
- Documented the closeout gates in README, the task index, and MIT parity
  targets so visual-validation docs are only updated after accepted evidence.
- Extended the protocol-doc self-test to keep README and task-index closeout
  gate instructions from drifting.
- The closeout summary now includes protocol paths and next-command hints for
  missing slots, so the next Pixel session can continue directly from the
  failed closeout report.
- The closeout summary now exposes each slot's expected final label in JSON and
  text output, so label enforcement and operator handoff use the same slot
  metadata.
- Wrong-slot-label reports now include the expected final label for each
  mismatched slot, so a failed closeout identifies the exact capture label to
  rerun.
- The closeout summary now supports `-OutputPath`, and the Pixel planner writes
  `pixel_closeout_summary.json` so final README/parity updates can cite a saved
  closeout artifact.
- Satisfied closeout slots now include an `artifactNote` with bundle, source,
  screenshot hash, and screenrecord hash for final README/parity release notes.
- Tightened closeout classification so the shared `Tremor` runtime mode cannot
  make Object vibration evidence satisfy the separate Fast tremor slot.
- Added explicit Fast tremor setup/final capture commands to the Pixel
  validation planner and closeout hint instead of relying on adapting the
  Object vibration command during the phone session.
- Added explicit manual ROI, automatic ROI, and Breathing final capture
  commands to the planner and protocol docs so every remaining closeout slot has
  concrete setup/final commands.
- Added a Pixel validation self-test that cross-checks closeout `Next:` command
  hints against the planner command names, preventing stale handoff guidance.
- Added planner self-test coverage that keeps setup captures on
  `target_visible_unvalidated` and final captures on `-RequireFinalVisualEvidence`
  with accepted operator validation.
- Added the one-shot closeout and preset-doc gates directly to the
  `preset-parity-closeout` planner command, with self-test coverage that keeps
  doc updates after those readiness checks.
- Added protocol-doc self-test coverage that requires every planner capture
  command name to appear in its corresponding protocol doc.
- Added protocol-doc parsing coverage for documented Pixel capture command
  blocks, including validation against the live capture script parameter list.
- Updated the live visual validation guide to use `Tremor` for the public Fast
  Motion / Motion path. `ObjectVibration` remains accepted only for internal
  compatibility and parity experiments.

## Supporting Slice: Parity Evidence Protocol Links

- Updated README and `docs/testing/MIT_PARITY_TARGETS.md` so the preset visual
  parity table points at the strict watched-evidence protocols before any
  preset can be marked visually validated.
- Pulse color and Breathing point to the live linear Pixel protocol; Fast
  Motion/Object setups point to the live phase Pixel protocol; ROI alignment
  points to the ROI device validation protocol.
- The docs now call out the required final evidence profile plus renderer,
  phase, or ROI measurement gates for those domains.
- The closeout summary now includes `closeoutBlockers`, a compact JSON/text
  checklist of missing slots and accepted-evidence issues that still block
  roadmap or README/parity closeout.
- The Pixel validation planner now includes the current closeout blocker count
  and missing-slot next commands, with `-EvidenceRoot` support for reviewing a
  specific live-validation export folder.
- Added `-NextOnly` to the Pixel validation planner so a connected-device
  operator can print only the recommended setup/final captures needed by the
  current closeout blockers.
- Added `-Slot <slotId>` filtering to the Pixel validation planner so the
  recommended capture queue can focus on one missing closeout slot.
- The planner now reports available missing slots and invalid requested slots
  so mistyped slot filters do not silently produce an empty capture queue.
- Added `-CaptureStage Setup|Final` filtering to the Pixel validation planner
  so connected-device sessions can run pre-inspection captures separately from
  closing evidence captures.
- Added `-CommandsOnly` to the Pixel validation planner so a filtered queue can
  emit paste-ready capture command templates without checklist text.
- Added `-FailOnInvalidSlot` to the Pixel validation planner so scripted
  connected-device runs fail fast when a mistyped slot filter would otherwise
  emit an empty capture queue.
- Added `-OutputPath` to the Pixel validation planner so the exact
  machine-readable command queue can be saved as `pixel_validation_plan.json`
  for a connected-device session handoff.
- Added `-FailOnEmptyQueue` to the Pixel validation planner so automation can
  fail when filters produce no recommended captures.
- Added `tools/prepare_pixel_validation_handoff.ps1` so a phone-free command can
  write `pixel_validation_plan.json`, `pixel_closeout_summary.json`, and
  `pixel_validation_commands.txt` for the next connected-device session.
- The handoff command now also writes `pixel_validation_handoff.md`, a
  human-readable summary of recommended captures, closeout blockers, artifacts,
  and paste-ready commands.
- Pixel validation handoffs now include source branch, commit, clean-tree state,
  and origin/main reachability so later accepted evidence can be compared
  against the command bundle that produced it.
- The handoff command now writes `pixel_validation_handoff_manifest.json` with
  SHA-256 hashes for the plan, closeout summary, command list, and readable
  handoff so the next Pixel session can preserve artifact integrity.
- Added `tools/install_debug_on_pixel.ps1` so connected-device tasks can build,
  install, grant camera permission, and launch through the SDK `adb.exe` even
  when `adb` is not on `PATH`.
- Pixel validation planner commands now default to `-DeviceSerial
  47091JEKB05516`, and the ROI, live-linear, and live-phase watched protocol
  snippets include the same serial so remaining evidence captures target the
  connected Pixel 8a explicitly.
- Pixel validation handoff bundles now accept and record the target
  `-DeviceSerial`, pass it into the saved planner and command list, and include
  it in the Markdown handoff and manifest.
- Live validation summaries now accept `-RequireDeviceSerial`, generated Pixel
  planner commands include it beside `-DeviceSerial`, and closeout can fail
  accepted evidence from any serial other than the expected Pixel 8a. This keeps
  remaining watched parity artifacts tied to `47091JEKB05516` instead of only
  recording the requested device after the fact.

## Remaining

- Extend Pixel 8a evidence with a known-good visual artifact and watched target
  setup for each preset. The current automated benchmark covers rendered frame
  jank, thermal status, metadata recording stability, and encoded MP4 validity,
  but not visual parity.
- Use `docs/experiments/pixel8a_live_linear_validation.md` for Pulse and
  Breathing watched visual evidence, and use
  `docs/experiments/pixel8a_live_phase_validation.md` for Object vibration and
  Fast tremor phase-motion evidence.
- Use `tools/capture_live_validation_evidence.ps1` for watched preset runs so
  each visual claim has a screenshot or recording plus logcat, gfxinfo, thermal,
  battery, focused-window, and manifest context.
- Mark the README/parity docs as visually validated only after those watched
  target notes and artifacts exist.

## Done When

- Users can choose a preset and get setup instructions specific enough to reproduce the visible effect.
- Each parity preset has a Pixel 8a benchmark note and a known-good sample/evidence artifact.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.
