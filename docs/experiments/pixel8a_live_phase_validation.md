# Pixel 8a Live Phase Validation

Date: 2026-07-12

## Purpose

Validate the live phase ROI renderer on Pixel 8a after synthetic moving-edge and
`local-euler` recorded validation have passed.

This is the final AR milestone gate before calling live Motion/Object phase
magnification useful. It must show a controlled subtle movement target, not just
shader compilation or recorded-video processing.

## Setup

- Device: Pixel 8a, package `com.dnrohr.eulerianmagnification`.
- Mount the phone on a stable stand or tripod.
- Use a high-contrast target with a crisp vertical or diagonal edge.
- Move the target with a small repeatable displacement. Good options:
  - a ruler or card edge tapped lightly at roughly 4-8 Hz;
  - a phone or small object on a table with a tiny vibration source;
  - a printed black/white edge attached to a flexible support.
- Avoid handheld camera motion. If the overlay reports camera/ROI motion, reset
  the ROI after stabilizing the setup.

## App Settings

- Preview path: GL preview.
- Mode: Motion or Object.
- View: Amplified first, then Difference and Split.
- Manual ROI: draw a box around the high-contrast edge only.
- Keep the expanded GL debug overlay visible until phase reports ready, then use
  clean preview if the overlay blocks visual inspection.

## Expected Live Diagnostics

The expanded GL debug area should progress through:

- `phase fallback: manual ROI required` before drawing an ROI.
- `phase: <width>x<height> / phase warmup: filling temporal state / ...`
  immediately after the ROI is selected.
- `phase: <width>x<height> / phase ready / ...` after at least one temporal
  history frame.

Fallbacks are acceptable only when they match the setup:

- `phase fallback: preview timing unhealthy`: reduce ROI size or restart the app.
- `phase fallback: GL phase resources unavailable`: device/GLES support issue.
- `phase fallback: phase renderer disabled after GL error`: capture logs and do
  not count the run as passing.

## Expected Visual Result

Pass criteria:

- Amplified view shows edge displacement inside the ROI that is visibly larger
  than the raw camera motion.
- Pixels outside the manual ROI remain raw full-frame context.
- Difference view localizes motion energy near the moving edge instead of
  flashing the whole frame.
- Split view shows raw left/processed right, with the processed side displaying
  stronger edge movement or phase contrast.
- The app remains responsive and does not crash during at least a 10 second run.

Known acceptable artifacts:

- Brief warmup where the output looks raw or weak.
- Mild grayscale/contrast change inside the ROI; the first live phase compose
  path reconstructs luminance, not full color.
- Edge halos at high amplification.
- Low-amplitude regions may remain unamplified because of amplitude gating.

Failure artifacts:

- Whole ROI flickers uniformly with no edge-localized motion.
- Output is upside down, stretched, or not aligned with the selected ROI.
- Difference view flashes over the full frame instead of the moving edge.
- Phase diagnostics stay in fallback or warmup indefinitely.
- App crash, GL error fallback, or severe preview latency.

## Evidence To Capture

Preferred capture command after installing the debug build:

Planner command name: `live-phase-object-setup`.

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "live-phase-object" `
  -WaitForThermalReady `
  -ThermalReadyBelowStatus 4 `
  -ThermalReadySamples 2 `
  -ThermalReadyTimeoutSeconds 900 `
  -ThermalReadyPollSeconds 30 `
  -Mode Tremor `
  -View Split `
  -RoiSource Manual `
  -ManualRoi "0.25,0.25,0.75,0.75" `
  -GlPreview $true `
  -Controls $true `
  -Panel Debug `
  -ScreenRecordSeconds 15 `
  -RequireScreenrecord `
  -RequireThermalReady `
  -RequireCameraFps `
  -RequireFocusedApp `
  -RequirePhaseDiagnostics `
  -RequireEvidenceVerdict target_visible_unvalidated `
  -RequireUiText "Renderer: Live phase motion","GL renderer: Live phase motion","phase:" `
  -TargetDescription "high-contrast edge or small object moving subtly inside manual ROI" `
  -VisualClaim "Live phase Split view shows edge-localized amplified motion inside the manual ROI" `
  -TargetVisible $true `
  -VisualValidated $false `
  -OperatorNotes "Set VisualValidated true only after inspecting the recording against the pass criteria." `
  -Summarize
```

This first command is intentionally a pre-inspection capture. It should stop at
`target_visible_unvalidated`: the target is visible and runtime evidence is
present, but the operator has not yet accepted the visual claim.

For the closing AR run, repeat the capture after inspection with hidden controls
or Clean preview if the Debug panel obscures the target, set
`-VisualValidated $true`, and replace the exploratory verdict gate with the
final profile:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "live-phase-object-final" `
  -WaitForThermalReady `
  -ThermalReadyBelowStatus 4 `
  -ThermalReadySamples 2 `
  -ThermalReadyTimeoutSeconds 900 `
  -ThermalReadyPollSeconds 30 `
  -Mode Tremor `
  -View Split `
  -RoiSource Manual `
  -ManualRoi "0.25,0.25,0.75,0.75" `
  -GlPreview $true `
  -Controls $false `
  -ScreenRecordSeconds 15 `
  -RequirePhaseDiagnostics `
  -RequireUiText "Renderer: Live phase motion","GL renderer: Live phase motion","phase:" `
  -TargetDescription "high-contrast edge or small object moving subtly inside manual ROI" `
  -VisualClaim "Live phase Split view shows edge-localized amplified motion inside the manual ROI" `
  -TargetVisible $true `
  -VisualValidated $true `
  -OperatorNotes "Accepted only if the recording shows edge-localized amplified motion, not uniform ROI flashing." `
  -RequireFinalVisualEvidence `
  -Summarize
```

`-RequireFinalVisualEvidence` expands to the closing evidence gates: clean
source, accepted visual validation, warning-free summary, valid MP4
screenrecord, thermal-readiness evidence, camera-FPS evidence, and focused-app
evidence. A closing AR evidence run should fail fast if it was captured from a
dirty worktree, if the bundle is still only `target_visible_unvalidated`, if the
camera cadence is missing or low, or if known-good evidence is requested while
warnings remain.

For the separate Fast tremor parity slot, use a high-contrast fast-motion target
and keep the label, target description, and visual claim distinct from Object
vibration. The setup command is:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "live-phase-fast-tremor-setup" `
  -WaitForThermalReady `
  -ThermalReadyBelowStatus 4 `
  -ThermalReadySamples 2 `
  -ThermalReadyTimeoutSeconds 900 `
  -ThermalReadyPollSeconds 30 `
  -Mode Tremor `
  -View Split `
  -RoiSource Manual `
  -ManualRoi "0.25,0.25,0.75,0.75" `
  -GlPreview $true `
  -Controls $true `
  -Panel Debug `
  -ScreenRecordSeconds 15 `
  -RequireScreenrecord `
  -RequireThermalReady `
  -RequireCameraFps `
  -RequireFocusedApp `
  -RequirePhaseDiagnostics `
  -RequireEvidenceVerdict target_visible_unvalidated `
  -RequireUiText "Renderer: Live phase motion","GL renderer: Live phase motion","phase:" `
  -TargetDescription "fast tremor target with a high-contrast edge inside manual ROI" `
  -VisualClaim "Live phase Split view shows edge-localized amplified fast tremor without uniform ROI flashing" `
  -TargetVisible $true `
  -VisualValidated $false `
  -OperatorNotes "Set VisualValidated true only after inspecting the recording against the Fast tremor pass criteria." `
  -Summarize
```

After inspection, close the Fast tremor slot with:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "live-phase-fast-tremor-final" `
  -WaitForThermalReady `
  -ThermalReadyBelowStatus 4 `
  -ThermalReadySamples 2 `
  -ThermalReadyTimeoutSeconds 900 `
  -ThermalReadyPollSeconds 30 `
  -Mode Tremor `
  -View Split `
  -RoiSource Manual `
  -ManualRoi "0.25,0.25,0.75,0.75" `
  -GlPreview $true `
  -Controls $false `
  -ScreenRecordSeconds 15 `
  -RequirePhaseDiagnostics `
  -RequireUiText "Renderer: Live phase motion","GL renderer: Live phase motion","phase:" `
  -TargetDescription "fast tremor target with a high-contrast edge inside manual ROI" `
  -VisualClaim "Live phase Split view shows accepted edge-localized amplified fast tremor without uniform ROI flashing" `
  -TargetVisible $true `
  -VisualValidated $true `
  -OperatorNotes "Accepted only if the recording shows edge-localized amplified fast tremor, not uniform ROI flashing or color-only change." `
  -RequireFinalVisualEvidence `
  -Summarize
```

The generated `evidence_summary.json` must be checked before the run can count:

- `source.dirty` is `false`.
- `requiredGates.phaseDiagnostics.passed` is `true`.
- Pre-inspection captures have `requiredGates.evidenceVerdict.passed` for
  `target_visible_unvalidated`.
- Final captures have the final visual evidence gate components present and
  passing.
- `evidenceVerdict.status` is `target_visible_unvalidated` before visual
  inspection, then `visual_validated` only after operator acceptance.
- `passedRuntimeSmoke` is `true`.
- `uiDump.phaseLabels` includes a live phase fallback, warmup, or ready line.
- `runtimeFindings` has no crash, ANR, or GL error.
- Thermal preflight is below `critical`; hot-device captures can be useful for
  diagnosis but must not be used as pass evidence.

Manual ADB fallback:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -c
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell monkey -p com.dnrohr.eulerianmagnification 1
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell screenrecord --time-limit 15 /sdcard/Download/live-phase-validation.mp4
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -d -t 300 > sample-videos\exports\live-phase-validation-logcat.txt
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" pull /sdcard/Download/live-phase-validation.mp4 sample-videos\exports\live-phase-validation.mp4
```

Record the result in this file:

- selected mode/view
- ROI size/placement
- phase diagnostic summary
- whether Amplified/Difference/Split passed the visual criteria
- artifacts observed
- logcat crash/GL-error status

## Current Status

Pre-phone validation is complete:

- JVM synthetic moving-edge parity passed.
- Connected Pixel 8a `local-euler` parity harness passed.
- Latest debug build installed on Pixel 8a.

Device smoke check:

- Granted camera permission with ADB after reinstall.
- Launched `com.dnrohr.eulerianmagnification/.MainActivity` with `adb shell
  monkey -p com.dnrohr.eulerianmagnification 1`.
- Recent logcat showed camera 1 opened and camera streaming started for the app.
- Recent logcat contained no `FATAL EXCEPTION`, app `AndroidRuntime` crash,
  `GlException`, or `GL error` entry.

Live controlled object-motion validation remains open because it requires a
physical moving target in front of the camera.
