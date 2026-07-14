# ROI Device Validation

Use this procedure to verify that analysis ROI coordinates match the visible preview on the Pixel 8a.

## Device

- Device: Pixel 8a-class Android phone
- App package: `com.dnrohr.eulerianmagnification`
- Orientation: portrait
- Preview path: CameraX first; repeat with GL preview when the GL path is under test
- Evidence capture: use `tools\capture_live_validation_evidence.ps1` as
  described in `docs/testing/LIVE_VISUAL_VALIDATION_CAPTURE.md`.

## Manual ROI Procedure

1. Install the latest debug APK.
2. Launch the app and keep the phone in portrait orientation.
3. Place a non-sensitive known target in frame, such as a high-contrast paper rectangle or wall fixture.
4. Drag a manual ROI tightly around the visible target.
5. Confirm the compact label says `Manual ROI`.
6. Confirm the yellow ROI outline and tint region overlap the same target with no horizontal flip, vertical offset, or stretch.
7. Repeat in `Raw`, `Amp`, `Diff`, and `Split` where supported.

For repeatable evidence, capture a clean-preview bundle and measure the visible
yellow manual ROI outline against the expected screenshot-space target
rectangle. The first command is a pre-inspection capture: it should prove that
the target is visible and the ROI measurement exists, but it should stop at
`target_visible_unvalidated` until the operator accepts the alignment.

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "manual-roi-known-target" `
  -WaitForThermalReady `
  -ThermalReadyBelowStatus 4 `
  -ThermalReadySamples 2 `
  -ThermalReadyTimeoutSeconds 900 `
  -ThermalReadyPollSeconds 30 `
  -Mode Tremor `
  -View Raw `
  -RoiSource Manual `
  -ManualRoi "0.25,0.25,0.75,0.75" `
  -GlPreview $true `
  -Controls $false `
  -Clean $true `
  -MeasureRoiExpected "<visible-target-bounds-in-screenshot-space>" `
  -MeasureRoiKind Manual `
  -RequireRoiMeasurement `
  -ScreenRecordSeconds 10 `
  -RequireScreenrecord `
  -RequireThermalReady `
  -RequireCameraFps `
  -RequireFocusedApp `
  -RequireEvidenceVerdict target_visible_unvalidated `
  -TargetDescription "known high-contrast target inside manually selected ROI" `
  -VisualClaim "Manual ROI outline overlaps the same visible target that was selected" `
  -TargetVisible $true `
  -VisualValidated $false `
  -OperatorNotes "Set VisualValidated true only after inspecting the screenshot/recording and measurement JSON." `
  -Summarize
```

`ExpectedRoi` is normalized screenshot space, not analysis-camera space. Derive
it from the visible known target or fixture in the captured screenshot. The
measurement proves the overlay landed where expected on screen; it only proves
manual target alignment if that expected rectangle was derived from the visible
target. The analyzer also requires a single connected ROI component by default,
which catches duplicate visible ROI boxes in the search region.

For the final accepted manual ROI run, repeat the command from a clean committed
source tree with `-VisualValidated $true`, keep `-RequireRoiMeasurement`, and
replace the exploratory verdict gate with `-RequireFinalVisualEvidence`. The
final command should fail if the source tree is dirty, the recording is missing,
thermal readiness is missing, the camera cadence is missing or low, the app is
not focused, the ROI measurement fails, or the accepted visual claim still has
warnings.

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "manual-roi-known-target-final" `
  -WaitForThermalReady `
  -ThermalReadyBelowStatus 4 `
  -ThermalReadySamples 2 `
  -ThermalReadyTimeoutSeconds 900 `
  -ThermalReadyPollSeconds 30 `
  -Mode Tremor `
  -View Raw `
  -RoiSource Manual `
  -ManualRoi "0.25,0.25,0.75,0.75" `
  -GlPreview $true `
  -Controls $false `
  -Clean $true `
  -MeasureRoiExpected "<visible-target-bounds-in-screenshot-space>" `
  -MeasureRoiKind Manual `
  -RequireRoiMeasurement `
  -ScreenRecordSeconds 10 `
  -RequireScreenrecord `
  -RequireThermalReady `
  -RequireCameraFps `
  -RequireFocusedApp `
  -TargetDescription "known high-contrast target inside manually selected ROI" `
  -VisualClaim "Manual ROI outline overlaps the same visible target that was selected" `
  -TargetVisible $true `
  -VisualValidated $true `
  -OperatorNotes "Accepted only if the screenshot/recording and measurement JSON show one manual ROI outline aligned to the visible target." `
  -RequireFinalVisualEvidence `
  -Summarize
```

## Automatic Face ROI Procedure

1. Install the latest debug APK.
2. Launch the app in portrait orientation with the front camera facing a visible face.
3. Use `Pulse` mode.
4. Wait for the compact ROI label to change from `Center ROI` or `Frozen ROI` to `Tracking`.
5. Confirm the automatic ROI outline lands on the visible face/skin region rather than the room, shoulder, or center fallback.
6. Move slightly and confirm short detection misses show `Frozen ROI` without visible wandering.
7. If the ROI remains far from the face while `Tracking`, capture non-sensitive evidence and inspect `PreviewRoiMapper`.

Use the same screenshot analyzer for the green automatic ROI outline:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "auto-face-roi" `
  -WaitForThermalReady `
  -ThermalReadyBelowStatus 4 `
  -ThermalReadySamples 2 `
  -ThermalReadyTimeoutSeconds 900 `
  -ThermalReadyPollSeconds 30 `
  -Mode Pulse `
  -RoiSource Auto `
  -GlPreview $true `
  -Controls $false `
  -Clean $true `
  -MeasureRoiExpected "<visible-face-or-skin-target-bounds-in-screenshot-space>" `
  -MeasureRoiKind Auto `
  -RequireRoiMeasurement `
  -ScreenRecordSeconds 10 `
  -RequireScreenrecord `
  -RequireThermalReady `
  -RequireCameraFps `
  -RequireFocusedApp `
  -RequireEvidenceVerdict target_visible_unvalidated `
  -TargetDescription "visible face or skin target tracked by automatic ROI" `
  -VisualClaim "Automatic ROI outline overlaps the visible face or skin region being tracked" `
  -TargetVisible $true `
  -VisualValidated $false `
  -OperatorNotes "Set VisualValidated true only after the automatic ROI is inspected against the visible face/skin target." `
  -Summarize
```

For automatic face validation, `ExpectedRoi` must come from the visible face or
skin target in the screenshot. Measuring the center fallback box is useful as an
overlay smoke test, but it does not prove face tracking.

For the final accepted automatic ROI run, repeat the command from a clean
committed source tree with `-VisualValidated $true`, keep
`-RequireRoiMeasurement`, and replace the exploratory verdict gate with
`-RequireFinalVisualEvidence`. Do not mark automatic ROI as validated from a
`Center ROI`, `Frozen ROI`, or fallback measurement unless the milestone note
explicitly says the run was only an overlay smoke test.

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "auto-face-roi-final" `
  -WaitForThermalReady `
  -ThermalReadyBelowStatus 4 `
  -ThermalReadySamples 2 `
  -ThermalReadyTimeoutSeconds 900 `
  -ThermalReadyPollSeconds 30 `
  -Mode Pulse `
  -RoiSource Auto `
  -GlPreview $true `
  -Controls $false `
  -Clean $true `
  -MeasureRoiExpected "<visible-face-or-skin-target-bounds-in-screenshot-space>" `
  -MeasureRoiKind Auto `
  -RequireRoiMeasurement `
  -ScreenRecordSeconds 10 `
  -RequireScreenrecord `
  -RequireThermalReady `
  -RequireCameraFps `
  -RequireFocusedApp `
  -TargetDescription "visible face or skin target tracked by automatic ROI" `
  -VisualClaim "Automatic ROI outline overlaps the visible face or skin region being tracked" `
  -TargetVisible $true `
  -VisualValidated $true `
  -OperatorNotes "Accepted only if the screenshot/recording and measurement JSON show one automatic ROI outline aligned to the visible face or skin target, not center or frozen fallback." `
  -RequireFinalVisualEvidence `
  -Summarize
```

## Live Reconstruction Procedure

1. Install the latest debug APK.
2. Launch the app and switch to `Pulse` mode.
3. Enable GL preview from expanded controls.
4. Use `Amplified` view first, then repeat with `Split`.
5. Keep expanded controls visible long enough to read `GL renderer:`.
6. Confirm `GL renderer:` reports either `Live reconstruction` or `Live reconstruction fallback`.
7. If it reports `Live reconstruction`, confirm the preview is upright, nonblank, not stretched, and shows visible full-frame magnification on a stable pulse target.
8. If it reports `Live reconstruction fallback`, record that the device fell back to the GL color bridge and inspect GL half-float support or runtime GL errors before marking AE complete.
9. In `Split`, confirm the left side is raw preview and the right side is reconstructed or fallback processed output.
10. Keep the phone in portrait orientation for the validation note.

## Current Unattended Probe

Date: 2026-07-05

- Connected device: `47091JEKB05516`
- Latest debug APK installed successfully.
- The unattended front-camera screenshot did not contain a visible face target.
- The app showed `Center ROI`, which is expected when automatic face detection has no face to track.
- This probe does not complete automatic face ROI validation.

## Current Pixel Probe

Date: 2026-07-11

- Connected device: Pixel 8a `47091JEKB05516`
- Latest debug APK installed successfully.
- GL preview was enabled after making expanded controls scrollable.
- Expanded controls reported `GL renderer: Live reconstruction`.
- The unattended frame showed `Center ROI`, which is expected without a
  deliberate face or known target.
- An unattended manual ROI attempt over a visible object was inconclusive
  because the final screenshot did not clearly show `Manual ROI` aligned to the
  object.
- Manual ROI validation should be repeated with a deliberately placed target and
  the operator watching the screen.

## Manual ROI Overlay Analyzer Smoke

Date: 2026-07-12

- Connected device: Pixel 8a `47091JEKB05516`.
- Captured a clean-preview scripted manual ROI bundle:
  `sample-videos/exports/live-validation/20260712-160611-manual-roi-overlay-analyzer-smoke`.
- Ran `tools/measure_roi_overlay_screenshot.ps1` against the screenshot with
  expected visible ROI `0.083,0.250,0.919,0.751`.
- Result: pass, `33600` matched yellow ROI pixels, measured ROI
  `0.0796,0.2483,0.9204,0.7517`, maximum edge error `0.0034`.
- This validates the evidence analyzer and visible overlay measurement path.
  It does not close manual target validation because the expected rectangle was
  taken from the overlay smoke screenshot rather than a deliberate physical
  target boundary.

## Automatic ROI Overlay Analyzer Smoke

Date: 2026-07-12

- Connected device: Pixel 8a `47091JEKB05516`.
- Captured a clean-preview automatic ROI fallback bundle:
  `sample-videos/exports/live-validation/20260712-160930-auto-roi-overlay-analyzer-smoke`.
- Ran `tools/measure_roi_overlay_screenshot.ps1` with `-OverlayKind Auto`
  against expected visible ROI `0.197,0.363,0.535,0.646`.
- Result: pass, `14378` matched green ROI pixels, one connected component,
  measured ROI `0.1944,0.3596,0.5343,0.6404`, maximum edge error `0.0056`.
- This validates automatic-overlay measurement and duplicate-box detection on
  the fallback ROI. It does not close automatic face ROI validation because no
  visible face target was present.

## Pass Criteria

- Manual ROI: selected preview rectangle maps back to the same analysis region and displays over the same visible target.
- Automatic ROI: face detector output maps to the same visible face/skin region in portrait/front-camera preview.
- No duplicate ROI boxes are visible during normal manual ROI use.
- CameraX and GL preview paths document any intentional differences.
- AE live reconstruction: expanded controls identify the active GL renderer path, and `Live reconstruction` output is upright, nonblank, not stretched, and visibly magnified before AE is marked complete.
- A captured evidence bundle alone is not sufficient unless the target is
  visible and the screenshot or recording has been inspected against these pass
  criteria.
- Final manual or automatic ROI evidence must include both a passing
  `roi_overlay_measurement.json` and a final visual evidence summary from a
  clean, target-visible, visually accepted run.
