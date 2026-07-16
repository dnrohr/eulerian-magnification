# Milestone U - ROI Device Validation

Status: In progress

Importance: Very high. If the ROI is not over the visible target, every output is suspect.

Goal: prove that automatic and manual ROI coordinates align with the live preview on the target device.

## Tasks

- [x] Create a repeatable Pixel 8a validation procedure for portrait/front-camera preview.
- [ ] Verify manual ROI placement against a known target in the preview.
- [ ] Verify automatic face/skin ROI placement against a visible face target.
- [x] Capture non-sensitive evidence or describe the validation setup and result.
- [ ] Fix CameraX/GL preview mapping if the ROI does not align.
- [ ] Add or update coordinate-mapping tests for the verified transform.
- [x] Update README and device notes with the final validation result.

## Current Slice

- Added `docs/testing/ROI_DEVICE_VALIDATION.md` with manual and automatic ROI validation procedures.
- Installed and launched the latest debug APK on connected device `47091JEKB05516`.
- Captured an unattended front-camera probe on 2026-07-05.
- Result: no visible face target was in frame; the app showed `Center ROI`, which is expected fallback behavior but does not verify automatic face ROI alignment.

## Pixel Probe: 2026-07-11

- Connected device: Pixel 8a `47091JEKB05516`.
- Installed latest debug APK after the scrollable expanded-controls fix.
- Confirmed the app launches in portrait with camera permission granted.
- Confirmed GL preview can be enabled and reports `GL renderer: Live
  reconstruction`.
- Captured unattended current-frame evidence showing `Center ROI`, which is
  expected without a deliberate target but does not validate automatic ROI
  placement.
- Attempted unattended manual ROI placement over a non-sensitive visible object;
  the result was inconclusive and is not counted as validation because the final
  screenshot did not clearly show `Manual ROI` aligned to that target.

## Remaining Validation

- Manual ROI still needs a non-sensitive known target deliberately placed in frame.
- Automatic ROI still needs a visible face target in frame.
- If either validation shows mismatch, update `PreviewRoiMapper` and its tests before marking this milestone complete.
- This milestone gates Milestone AU, where manual ROI becomes a selectable non-default option. Automatic/default ROI behavior should not become the primary motion path until the mapped region is proven trustworthy on device.
- The mapper now has regression coverage for ROIs clipped by aspect-fill preview
  crop. Remaining validation is visual alignment on the Pixel, not uncovered
  crop arithmetic.

## Supporting Slice: Evidence Capture Tool

- Added `tools/capture_live_validation_evidence.ps1` to capture a Pixel
  screenshot, optional screen recording, logcat, gfxinfo, thermal state, battery
  state, focused window, and a manifest into ignored
  `sample-videos/exports/live-validation/` bundles.
- Added `docs/testing/LIVE_VISUAL_VALIDATION_CAPTURE.md` and linked it from the
  ROI validation guide.
- This supports manual/automatic ROI validation, but does not count as passing
  evidence unless the captured target is visible and inspected.

## Supporting Slice: Scripted Validation Launch

- Added validation launch extras for mode, view, amplification, GL preview, ROI
  source, AE/AWB lock, controls visibility, and clean preview.
- The evidence capture script can now launch directly into the desired
  validation state before collecting artifacts.
- Launch overrides are non-persistent by default so scripted validation does not
  rewrite normal saved settings unless `validation.persist` is explicitly set.

## Supporting Slice: Manual ROI Overlay Analyzer

- Added `tools/measure_roi_overlay_screenshot.ps1` to measure the visible yellow
  manual ROI outline in a captured screenshot.
- The tool compares the measured screenshot-space ROI against an expected
  normalized screenshot rectangle and writes JSON with matched-pixel count,
  measured bounds, edge errors, and pass/fail status.
- Verified the tool with a synthetic screenshot pass/fail smoke test.
- Ran a Pixel 8a clean-preview smoke capture:
  `sample-videos/exports/live-validation/20260712-160611-manual-roi-overlay-analyzer-smoke`.
- The analyzer passed on that screenshot with `33600` matched pixels and maximum
  normalized edge error `0.0034`.
- This makes manual ROI evidence less subjective, but does not close manual
  target validation until the expected rectangle is derived from a deliberate
  physical target in frame.

## Supporting Slice: Automatic ROI Overlay Analyzer

- Extended `tools/measure_roi_overlay_screenshot.ps1` with
  `-OverlayKind Manual|Auto`, default overlay colors for yellow manual ROI and
  green automatic ROI, and connected-component counting.
- The analyzer now fails by default when more than one ROI-colored component is
  found inside the search region, making duplicate-box artifacts visible in the
  evidence JSON.
- Verified synthetic manual, automatic, and duplicate-box cases.
- Ran a Pixel 8a clean-preview automatic fallback ROI smoke capture:
  `sample-videos/exports/live-validation/20260712-160930-auto-roi-overlay-analyzer-smoke`.
- The analyzer passed on that screenshot with `14378` matched pixels, one
  connected component, and maximum normalized edge error `0.0056`.
- This validates the automatic-overlay measurement path, but does not close
  automatic face ROI validation because no visible face target was present.

## Supporting Slice: Evidence Summary Tool

- Added `tools/summarize_live_validation_evidence.ps1` to turn a captured live
  validation bundle into `evidence_summary.json`.
- The summary records launch settings, required artifact presence, screenshot
  dimensions, gfx frame pacing, runtime crash/ANR/GL-error flags, and optional
  ROI overlay measurement status.
- Verified the summary tool on recent Pixel 8a bundles, including the automatic
  ROI overlay analyzer smoke bundle.
- This reduces manual audit errors for ROI validation evidence, but does not
  close manual or automatic target validation without a visible target.

## Supporting Slice: Capture-Integrated ROI Measurement

- Added optional ROI overlay measurement to
  `tools/capture_live_validation_evidence.ps1`.
- Passing `-MeasureRoiExpected` now runs the screenshot analyzer after capture,
  writes `roi_overlay_measurement.json`, records the artifact in the manifest,
  and lets `-Summarize` embed the measurement result in `evidence_summary.json`.
- The capture script preserves measurement failures as warnings instead of
  dropping the rest of the evidence bundle.
- Updated ROI validation docs so manual and automatic ROI captures can produce
  screenshot, logcat, gfxinfo, thermal, manifest, ROI measurement, and summary
  artifacts in one command.
- Verified on Pixel 8a with clean-preview manual ROI smoke bundle
  `sample-videos/exports/live-validation/20260712-163701-integrated-manual-roi-measurement-smoke`.
  The generated ROI measurement passed with one connected component, `33600`
  matched pixels, measured ROI `0.0796,0.2483,0.9204,0.7517`, maximum edge
  error `0.0034`, and the evidence summary embedded the measurement with no
  runtime warnings.
- This improves repeatability for the remaining known-target/visible-face ROI
  validation, but does not close those tasks until a watched target capture is
  inspected.

## Supporting Slice: Required ROI Measurement Gate

- Live validation summaries now accept `-RequireRoiMeasurement`.
- The gate exits `8` when `roi_overlay_measurement.json` is missing or reports
  a failed measurement.
- `tools/capture_live_validation_evidence.ps1` forwards the gate during
  `-Summarize`, so final manual/automatic ROI validation commands can require
  a passing overlay measurement rather than merely summarizing one if it
  happens to exist.
- The summary script now exits `0` explicitly on success, preventing stale
  PowerShell exit codes from leaking out of a previous failed summary.
- The summary self-test covers missing and passing ROI measurement cases.

## Supporting Slice: Final ROI Evidence Flow

- Updated `docs/testing/ROI_DEVICE_VALIDATION.md` so manual and automatic ROI
  validation use the same two-step watched evidence flow as live renderer
  validation.
- Setup captures require a visible target, a passing ROI overlay measurement,
  valid screenrecord, thermal-readiness evidence, camera-FPS evidence,
  focused-app evidence, and `target_visible_unvalidated`.
- Final accepted manual and automatic ROI runs must keep
  `-RequireRoiMeasurement` and add `-RequireFinalVisualEvidence`, so overlay
  alignment cannot close from a dirty source tree, missing video, low/missing
  camera cadence, unfocused app, failed ROI measurement, or unaccepted visual
  claim.
- This does not complete U because the Pixel still needs a deliberate manual
  known target and visible face/skin automatic target in frame.

## Supporting Slice: Scoped Logcat Evidence

- Updated `tools/capture_live_validation_evidence.ps1` to clear device logcat
  before a capture by default, then record `logcatCleared` in the manifest.
- Added `-PreserveLogcat` for the uncommon case where older device log context
  is intentionally needed.
- This keeps runtime smoke summaries from being polluted by stale crash or GL
  error lines from previous app sessions, which is important when ROI mapping
  and live-preview validation depend on clean per-run evidence.

## Supporting Slice: Evidence Source Identity

- Capture manifests now record Git branch, full and short commit, dirty
  worktree state, and short status lines.
- Each live validation bundle now includes `app_package.txt` from
  `dumpsys package`, so evidence can be tied back to the installed Android
  package state as well as the source checkout.
- `evidence_summary.json` carries the source identity and reports whether the
  package dump artifact is present.
- This improves auditability for remaining watched target/face validation, but
  does not close those target-alignment tasks by itself.

## Supporting Slice: UI Text Evidence

- Live validation captures now run `uiautomator dump` and store `ui_dump.xml`
  beside the screenshot, logs, thermal, battery, and package artifacts.
- `evidence_summary.json` now reports whether the UI dump is present and
  extracts app-visible text, quality labels, renderer labels, and ROI labels.
- This makes unattended captures easier to audit for states such as `Thermal
  high`, `Full frame slow`, active ROI source, and live renderer diagnostics,
  without relying on OCR or manual screenshot reading.
- Captures and summaries now accept `-RequireUiText`, which records required UI
  strings in the manifest, writes per-string `uiTextAssertions`, and exits the
  summary with code `3` if an explicitly required label is missing.
- This improves evidence review for remaining ROI/live-preview validation, but
  does not close watched target alignment without a visible target.

## Supporting Slice: Visual Review Metadata

- Live validation captures now include a `visualReview` manifest block with
  target description, intended visual claim, target visibility, operator
  validation result, and operator notes.
- `evidence_summary.json` carries that block and marks
  `countsAsVisualValidation` true only when both `TargetVisible` and
  `VisualValidated` are true.
- Summaries warn when a visual claim is provided without target visibility or
  when a target is visible but the visual claim has not been accepted.
- Summaries also include `evidenceVerdict`, a compact status such as
  `runtime_smoke_only`, `target_visible_unvalidated`, or `visual_validated`.
- This makes the remaining watched manual/automatic ROI target runs auditable
  without treating unattended runtime smoke as visual validation.

## Supporting Slice: UI Dump Retry For Watched Evidence

- Live validation capture now retries Android `uiautomator dump` up to three
  times before declaring UI text unavailable.
- This targets transient `could not get idle state` failures observed while the
  camera preview and debug overlay were active.
- If all retry attempts fail, the capture still preserves screenshot, logcat,
  gfxinfo, thermal, battery, and manifest artifacts, but required UI-text
  assertions fail explicitly instead of silently passing.
- This improves reliability for remaining watched manual/automatic ROI target
  runs and renderer-label evidence, but does not replace visual target
  inspection.
- Pixel 8a runtime smoke bundle
  `sample-videos/exports/live-validation/20260712-183323-ui-dump-retry-smoke`
  passed with `uiDumpPresent=true`, no crash/ANR/GL error, camera HAL
  `29.82` fps average, and extracted UI text for renderer and ROI labels.
  The bundle was captured from an in-progress dirty worktree to validate the
  script change itself, so it is tooling smoke evidence rather than release
  evidence.

## Supporting Slice: Device-Targeted Evidence Capture

- `tools/capture_live_validation_evidence.ps1` and
  `tools/wait_for_device_thermal_ready.ps1` now accept `-AdbPath` and
  `-DeviceSerial`.
- Capture, launch, screenshot, screenrecord, logcat, UI dump, package dump,
  focused-window, thermal, battery, and thermal-readiness commands all forward
  the selected ADB serial.
- Evidence manifests record the requested device serial and ADB arguments so
  remaining Pixel 8a bundles can be traced to `47091JEKB05516` even if another
  emulator/device is connected or ADB has recently recovered from an `offline`
  state.
- Live evidence summaries now expose and can require the manifest device
  serial, and ROI planner commands add `-RequireDeviceSerial 47091JEKB05516` so
  final manual/automatic ROI evidence fails if it was not captured from the
  intended Pixel 8a.
- This improves repeatability and auditability for the remaining watched manual
  and automatic ROI validation, but does not close either target-alignment task
  without a visible known target.

## Supporting Slice: Screenshot Bounds Converter

- Added `tools/convert_roi_bounds_to_normalized.ps1` so watched ROI validation
  can convert visible target pixel bounds from a screenshot into the normalized
  `-MeasureRoiExpected` value used by the overlay analyzer.
- Added `tools/test_roi_bounds_converter.ps1` and included it in the offline
  project tooling suite so malformed, out-of-image, image-derived, and
  paste-ready text outputs stay covered without a connected phone.
- Updated `docs/testing/ROI_DEVICE_VALIDATION.md` to show where the converter
  fits in the manual and automatic ROI evidence flow.
- Updated the generated Pixel validation plan so manual and automatic ROI
  operator setup guidance points to the converter before final evidence capture.
- `tools/capture_live_validation_evidence.ps1` now fails before ADB launch when
  `-MeasureRoiExpected` or `-ManualRoi` still contains a placeholder or an
  invalid normalized rectangle, preventing wasted Pixel captures from stale
  command templates.
- Added `tools/prepare_roi_final_capture_command.ps1` so watched setup bundles
  plus measured target pixel bounds can produce the final manual or automatic
  ROI capture command with `-MeasureRoiExpected` already filled in.
- Pixel validation handoff runbooks now surface that final-command helper when
  manual or automatic ROI final commands still contain setup-derived placeholder
  bounds, reducing the chance of a watched session pasting an intentionally
  invalid placeholder command.

## Done When

- Manual and automatic ROI overlays align with the visible target on device.
- The validation is documented with exact device, orientation, preview path, and result.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
