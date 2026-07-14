# Pixel 8a Live Linear EVM Validation

Date: 2026-07-14

## Purpose

Validate the live full-frame linear EVM renderer on Pixel 8a with watched,
known-target evidence. This is the final visual gate for the AE/AP live linear
roadmap work and feeds the AT preset visual-validation checklist.

This protocol is not for proving that the renderer path exists; debug-panel
smoke captures already cover that. This protocol proves whether the live output
visibly magnifies a controlled target without falling back to ROI tint, uniform
flashing, a frozen camera stream, or a misleading debug overlay.

## Setup

- Device: Pixel 8a, package `com.dnrohr.eulerianmagnification`.
- Mount the phone on a stable stand or tripod.
- Use bright, steady lighting. Avoid auto-exposure hunting and screen flicker.
- Use hidden controls or Clean preview for visual-quality captures. Use the
  Debug panel only for a separate diagnostic capture.
- Let the thermal readiness wait pass before judging visual quality.

Recommended targets:

- Pulse color: a face, fingertip, or skin region under steady light, with the
  phone fixed and the subject still.
- Breathing / slow motion: a high-contrast edge or chest/cloth target moving
  slowly and repeatedly, ideally around the preset frequency band.

## Expected Live Diagnostics

The Debug panel should show all of the following before a visual run counts:

- `Preview: Full-frame linear EVM preview`.
- `Renderer: Live linear EVM reconstruction`.
- `GL renderer: Live reconstruction`.
- A ready pyramid diagnostic with at least three levels.
- A camera cadence label near 30 FPS and no low-FPS quality warning.

Fallback labels such as `GL color bridge`, `fallback`, `Full frame slow`,
`Thermal high`, or missing camera cadence do not necessarily indicate a bug, but
they do prevent the capture from counting as final AP/AE visual evidence.

## Expected Visual Result

Pulse color pass criteria:

- Amplified or Split view shows visible color change on the biological target.
- The effect is stronger than raw preview variation, not just a colored ROI box.
- The image remains upright, nonblank, and not stretched.
- The camera remains responsive through at least a 10 second recording.

Breathing / slow-motion pass criteria:

- Amplified or Split view shows visible full-frame reconstructed motion or edge
  displacement on the controlled target.
- Difference view localizes change near moving structure instead of flashing the
  whole frame.
- Output does not collapse to uniform color shift, ROI-only tint, or a frozen
  frame.

Known acceptable artifacts:

- Brief temporal warmup where the output looks raw or weak.
- Mild halos at high amplification.
- Reduced effect when exposure/lighting gates attenuate Pulse color.

Failure artifacts:

- Only the boxed region changes color while the full image stays raw.
- Whole-frame flashing with no structure-localized difference.
- Upside-down, stretched, blank, or frozen preview.
- Debug overlay covers the target in the final visual capture.
- Runtime crash, ANR, GL error, low camera cadence, or thermal warning.

## Evidence Flow

Run a diagnostic setup capture first. It should prove the renderer state and
stop at `target_visible_unvalidated` until the operator inspects the recording:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "live-linear-pulse-setup" `
  -WaitForThermalReady `
  -ThermalReadyBelowStatus 4 `
  -ThermalReadySamples 2 `
  -ThermalReadyTimeoutSeconds 900 `
  -ThermalReadyPollSeconds 30 `
  -Mode Pulse `
  -View Split `
  -RoiSource FullFrame `
  -GlPreview $true `
  -Controls $true `
  -Panel Debug `
  -ScreenRecordSeconds 15 `
  -RequireScreenrecord `
  -RequireThermalReady `
  -RequireCameraFps `
  -RequireFocusedApp `
  -RequireRendererDiagnostics `
  -RequireEvidenceVerdict target_visible_unvalidated `
  -RequireUiText "Preview: Full-frame linear EVM preview","Renderer: Live linear EVM reconstruction","GL renderer: Live reconstruction" `
  -TargetDescription "watched pulse target under steady lighting" `
  -VisualClaim "Live Pulse Split view shows full-frame linear reconstruction rather than ROI tint" `
  -TargetVisible $true `
  -VisualValidated $false `
  -OperatorNotes "Set VisualValidated true only after inspecting the recording against the pass criteria." `
  -Summarize
```

For the final accepted Pulse evidence, repeat the capture from a clean committed
source tree with hidden controls or Clean preview, set `-VisualValidated $true`,
and use the final evidence profile:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "live-linear-pulse-final" `
  -WaitForThermalReady `
  -ThermalReadyBelowStatus 4 `
  -ThermalReadySamples 2 `
  -ThermalReadyTimeoutSeconds 900 `
  -ThermalReadyPollSeconds 30 `
  -Mode Pulse `
  -View Split `
  -RoiSource FullFrame `
  -GlPreview $true `
  -Controls $false `
  -ScreenRecordSeconds 15 `
  -RequireRendererDiagnostics `
  -RequireUiText "Preview: Full-frame linear EVM preview","Renderer: Live linear EVM reconstruction","GL renderer: Live reconstruction" `
  -TargetDescription "watched pulse target under steady lighting" `
  -VisualClaim "Live Pulse Split view shows accepted full-frame linear reconstruction rather than ROI tint" `
  -TargetVisible $true `
  -VisualValidated $true `
  -OperatorNotes "Accepted only if the recording shows target-visible magnification without ROI-only tint or full-frame flashing." `
  -RequireFinalVisualEvidence `
  -Summarize
```

For Breathing / slow motion, keep the labels and target language distinct from
Pulse. The Breathing setup command is:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "live-linear-breathing-setup" `
  -WaitForThermalReady `
  -ThermalReadyBelowStatus 4 `
  -ThermalReadySamples 2 `
  -ThermalReadyTimeoutSeconds 900 `
  -ThermalReadyPollSeconds 30 `
  -Mode Breathing `
  -View Split `
  -RoiSource FullFrame `
  -GlPreview $true `
  -Controls $true `
  -Panel Debug `
  -ScreenRecordSeconds 15 `
  -RequireScreenrecord `
  -RequireThermalReady `
  -RequireCameraFps `
  -RequireFocusedApp `
  -RequireRendererDiagnostics `
  -RequireEvidenceVerdict target_visible_unvalidated `
  -RequireUiText "Preview: Full-frame linear EVM preview","Renderer: Live linear EVM reconstruction","GL renderer: Live reconstruction" `
  -TargetDescription "watched slow-motion edge or breathing target" `
  -VisualClaim "Live Breathing Split view shows full-frame reconstructed motion on the watched target rather than ROI tint" `
  -TargetVisible $true `
  -VisualValidated $false `
  -OperatorNotes "Set VisualValidated true only after inspecting the recording against the Breathing pass criteria." `
  -Summarize
```

After inspection, close the Breathing slot with:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "live-linear-breathing-final" `
  -WaitForThermalReady `
  -ThermalReadyBelowStatus 4 `
  -ThermalReadySamples 2 `
  -ThermalReadyTimeoutSeconds 900 `
  -ThermalReadyPollSeconds 30 `
  -Mode Breathing `
  -View Split `
  -RoiSource FullFrame `
  -GlPreview $true `
  -Controls $false `
  -ScreenRecordSeconds 15 `
  -RequireRendererDiagnostics `
  -RequireUiText "Preview: Full-frame linear EVM preview","Renderer: Live linear EVM reconstruction","GL renderer: Live reconstruction" `
  -TargetDescription "watched slow-motion edge or breathing target" `
  -VisualClaim "Live Breathing Split view shows accepted full-frame reconstructed motion on the watched target rather than ROI tint" `
  -TargetVisible $true `
  -VisualValidated $true `
  -OperatorNotes "Accepted only if the recording shows target-visible Breathing motion magnification without ROI-only tint, whole-frame flashing, or a frozen camera stream." `
  -RequireFinalVisualEvidence `
  -Summarize
```

## Summary Checks

The generated `evidence_summary.json` must show:

- `source.dirty` is `false` for final evidence.
- `source.commitReachableFromOriginMain` is `true` for final evidence.
- `requiredGates.rendererDiagnostics.passed` is `true`.
- Setup captures pass `requiredGates.evidenceVerdict` for
  `target_visible_unvalidated`.
- Final captures pass the final visual evidence gate components.
- `evidenceVerdict.status` is `target_visible_unvalidated` before visual
  inspection, then `visual_validated` only after operator acceptance.
- `passedRuntimeSmoke` is `true`.
- `uiDump.rendererLabels` includes live linear reconstruction labels.
- `runtimeFindings` has no crash, ANR, or GL error.
- Camera FPS evidence is present and above the warning threshold.
- Thermal readiness is present and ready.

Record accepted Pulse and Breathing results in the AP milestone. If either
capture fails only because the diagnostics are missing but the screenshot
visibly shows the labels, fix the evidence tooling before counting the result.

## Current Status

Pre-phone structural validation is complete:

- The live GL path has pyramid, temporal bandpass, Laplacian-style
  reconstruction, per-mode profiles, display headroom, and Pulse color gates.
- Pixel smoke evidence has shown the renderer can report live reconstruction
  diagnostics and remain runtime-stable.

Watched known-target visual validation remains open because it requires the
physical Pixel and a controlled target in frame.
