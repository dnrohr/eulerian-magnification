# Live Visual Validation Capture

Use `tools\capture_live_validation_evidence.ps1` whenever a Pixel run needs
evidence for ROI mapping, live reconstruction, live phase motion, or preset
visual validation.

The script creates an ignored bundle under `sample-videos\exports\live-validation`
with:

- `screenshot.png`
- optional `screenrecord.mp4`
- `logcat_tail.txt`
- `gfxinfo.txt`
- `thermalservice.txt`
- `battery.txt`
- `window_focus.txt`
- `device_props.txt`
- `manifest.json`
- optional `roi_overlay_measurement.json` when `-MeasureRoiExpected` is passed
- optional `evidence_summary.json` when `-Summarize` is passed

## Command

```powershell
.\tools\capture_live_validation_evidence.ps1 -Label "manual-roi-target" -ScreenRecordSeconds 15
```

The script launches the app before capture by default, then waits briefly for
the preview to settle. Scripted launches force-stop the package first so ADB
extras are applied deterministically. Use `-ScreenRecordSeconds 0` for a
screenshot/log-only smoke capture. Use `-SkipLaunch` only when the operator has
already navigated to a specific controls state that should not be disturbed.

Pass `-Summarize` to write `evidence_summary.json` as part of the capture:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "live-linear-pulse" `
  -Mode Pulse `
  -View Split `
  -RoiSource FullFrame `
  -GlPreview $true `
  -Controls $false `
  -ScreenRecordSeconds 10 `
  -Summarize
```

The summary writes `evidence_summary.json` with launch state, required artifact
presence, screenshot dimensions, gfx frame pacing, runtime crash/ANR/GL-error
signals, and optional ROI overlay measurement status. A passing runtime smoke
summary still does not prove visual validation unless the target is visible and
inspected.

For visual-quality captures, prefer hidden controls or Clean mode. Use
`-Controls $true -Panel Debug` only when the goal is to capture renderer
diagnostics, because the debug overlay can add enough UI jank to make the
preview look worse than the camera/render path actually is. The capture manifest
and summary include a warning when the debug panel is used.

For ROI overlay validation, pass `-MeasureRoiExpected` with the expected
normalized screenshot-space rectangle. The capture script then writes
`roi_overlay_measurement.json` and the summary embeds the result:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "manual-roi-overlay" `
  -Mode Tremor `
  -View Raw `
  -RoiSource Manual `
  -ManualRoi "0.25,0.25,0.75,0.75" `
  -GlPreview $true `
  -Controls $false `
  -Clean $true `
  -MeasureRoiExpected "0.083,0.250,0.919,0.751" `
  -MeasureRoiKind Manual `
  -Summarize
```

`-MeasureRoiExpected` must use screenshot coordinates, not camera analysis
coordinates. A passing measurement proves the visible overlay position; it only
proves target alignment when the expected rectangle was derived from the visible
known target in that screenshot.

## Scripted App State

The capture script can launch the app into a specific validation state through
ADB extras:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "live-phase-motion" `
  -Mode Tremor `
  -View Split `
  -RoiSource Manual `
  -ManualRoi "0.25,0.25,0.75,0.75" `
  -Amplification 18 `
  -GlPreview $true `
  -Controls $true `
  -Panel Debug `
  -ScreenRecordSeconds 15
```

Available launch parameters:

- `-Mode`: `Pulse`, `Breathing`, `Tremor`, or `ObjectVibration`.
- `-View`: `Raw`, `Amplified`, `Difference`, or `Split`.
- `-RoiSource`: `Auto`, `FullFrame`, or `Manual`.
- `-ManualRoi`: optional normalized `left,top,right,bottom` rectangle, for
  example `0.25,0.25,0.75,0.75`.
- `-Amplification`: clamped by the app to `1x-30x`.
- `-GlPreview`: requests GL preview.
- `-Controls`: opens or hides expanded controls.
- `-Clean`: opens or hides clean preview.
- `-Panel`: when expanded controls are open, selects `Controls`, `Setup`,
  `Recording`, or `Debug`.
- `-LockAeAwb`: requests locked exposure/white balance.
- `-MeasureRoiExpected`: optional normalized screenshot-space
  `left,top,right,bottom` rectangle to check against the visible ROI overlay.
- `-MeasureRoiKind`: `Manual` or `Auto`; defaults from `-RoiSource` when
  omitted.
- `-MeasureRoiColorTolerance`, `-MeasureRoiSearchMargin`,
  `-MeasureRoiMaxEdgeError`, and `-MeasureRoiMinimumMatchedPixels`: optional
  analyzer thresholds forwarded to the ROI overlay measurement script.
- `-MeasureRoiAllowMultipleComponents`: allows more than one connected overlay
  component when a test intentionally expects duplicate marks.
- `-Summarize`: writes `evidence_summary.json` immediately after capture.
- `-PersistLaunchSettings`: saves the launch settings. Omit this for normal
  validation captures so saved user settings are left unchanged.

Requested modes still obey the app's device capability gating. If a mode is not
available on the current device report, the app falls back to the first available
mode.

The same extras can be sent manually:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start `
  -n com.dnrohr.eulerianmagnification/.MainActivity `
  --es validation.mode Tremor `
  --es validation.view Split `
  --es validation.roiSource Manual `
  --es validation.manualRoi 0.25,0.25,0.75,0.75 `
  --ez validation.glPreview true `
  --ez validation.controls true `
  --es validation.panel Debug
```

## When The Evidence Counts

The bundle counts toward a visual-validation checklist only when the screenshot
or recording contains the intended target and the result is inspected against the
matching pass criteria:

- ROI mapping: the visible ROI outline must overlap the same target that was
  selected or detected.
- Live full-frame reconstruction: the preview must be upright, nonblank, not
  stretched, and visibly processed on a stable target.
- Live phase motion: motion must localize near the moving edge/object rather
  than flashing the whole ROI or full frame.
- Preset validation: the selected preset, target, lighting, support, and visible
  output must match the preset setup guidance.

An unattended capture with no known target is useful as a runtime smoke check,
but it does not close a visual-validation task.

## Recommended Labels

- `manual-roi-target`
- `auto-face-roi`
- `live-linear-pulse`
- `live-phase-object`
- `preset-pulse-color`
- `preset-breathing`
- `preset-object-vibration`
- `preset-fast-tremor`
