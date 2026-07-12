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

## Command

```powershell
.\tools\capture_live_validation_evidence.ps1 -Label "manual-roi-target" -ScreenRecordSeconds 15
```

The script launches the app before capture by default, then waits briefly for
the preview to settle. Use `-ScreenRecordSeconds 0` for a screenshot/log-only
smoke capture. Use `-SkipLaunch` only when the operator has already navigated to
a specific controls state that should not be disturbed.

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
