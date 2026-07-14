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
- `thermalservice_preflight.txt`
- `thermalservice.txt`
- `battery.txt`
- `window_focus.txt`
- `ui_dump.xml`
- `device_props.txt`
- `app_package.txt`
- `manifest.json`
- optional `roi_overlay_measurement.json` when `-MeasureRoiExpected` is passed
- optional `evidence_summary.json` when `-Summarize` is passed

If thermal preflight aborts the run before app launch, the bundle intentionally
contains only the preflight artifacts, `manifest.json`, and optional
`evidence_summary.json`.

## Command

```powershell
.\tools\capture_live_validation_evidence.ps1 -Label "manual-roi-target" -ScreenRecordSeconds 15
```

The script launches the app before capture by default, then waits briefly for
the preview to settle. Scripted launches force-stop the package first so ADB
extras are applied deterministically. Use `-ScreenRecordSeconds 0` for a
screenshot/log-only smoke capture. Use `-SkipLaunch` only when the operator has
already navigated to a specific controls state that should not be disturbed.
By default, the script clears logcat before launch/capture so runtime findings
belong to the current evidence bundle. Pass `-PreserveLogcat` only when the
older device log context is intentionally needed.

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

The manifest records the current Git branch, full and short commit, dirty
worktree flag, short status lines, and installed package dump artifact. The
summary carries the same source identity plus launch state, required artifact
presence, screenshot dimensions, gfx frame pacing, runtime crash/ANR/GL-error
signals, camera HAL FPS samples from logcat, Android thermal status, battery
temperature/charging context, sampled screenshot content metrics,
machine-readable UI text from `ui_dump.xml`, extracted renderer/ROI/quality/
phase labels, optional thermal readiness wait results, and optional ROI overlay
measurement status.
If the source worktree was dirty when the bundle was captured, the summary adds
an explicit warning. Dirty captures can be useful when validating a script or
in-progress fix, but do not treat them as release-quality visual-validation
evidence until the same claim is repeated from a clean commit.
The script also records `thermalservice_preflight.txt` before clearing logcat or
launching the app. The manifest and summary expose this as `thermalPreflight`,
so a capture can show that the device was already throttled before the app was
started.
Thermal, external-power, high-battery-temperature, and low camera-cadence
warnings are advisory by default: they flag conditions that may affect a visual
or benchmark run, but they do not make runtime smoke fail unless a crash, ANR,
or GL error is also present. A passing runtime smoke summary still does not
prove visual validation unless the target is visible and inspected.
When `-Summarize` is used, the capture script exits with the summary result:
`0` for a passing runtime/evidence gate, `2` for runtime smoke failure, `3` for
missing required UI text, `4` for a thermal/preflight abort, `5` when visual
validation was required but the bundle does not count as visually validated,
and `6` when a clean source tree was required but source metadata is missing or
dirty, and `7` when a warning-free bundle was required but the summary contains
warnings, `8` when an ROI measurement was required but missing or failed, and
`9` when the bundle did not match a required evidence verdict, and `10` when a
required renderer or phase diagnostic label was not found, and `11` when a
required `screenrecord.mp4` artifact is missing, empty, or does not look like an
MP4 file, and `12` when required thermal-readiness evidence is missing or not
ready, and `13` when required camera HAL FPS evidence is missing or below the
warning threshold, and `14` when the expected app package is not present in the
focused-window dump.
This keeps automated validation commands from silently passing when an explicit
evidence assertion failed.
If preflight thermal status or sensor status is `critical` or worse, do not use
the run to judge full-frame FPS, apparent camera freeze, or visual parity. Let
the phone cool, then repeat with a short capture. By default, the capture script
aborts before clearing logcat or launching the app when preflight status reaches
`critical` or worse. The summary classifies that bundle as
`thermal_preflight_aborted`; it is useful operational evidence, not runtime
smoke or visual validation. If strict gates such as `-RequireCleanSource` or
`-RequireVisualValidation` were passed, the aborted summary still records those
requirements under `requiredGates`, but the command exits with the thermal abort
code `4`.

Use the thermal wait helper before watched validation runs:

```powershell
.\tools\wait_for_device_thermal_ready.ps1 `
  -ReadyBelowThermalStatus 4 `
  -RequiredReadySamples 2 `
  -TimeoutSeconds 900 `
  -PollSeconds 30 `
  -OutputPath "sample-videos\exports\live-validation\thermal-ready.json"
```

The helper exits `0` when both Android thermal status and max sensor status are
below the threshold for the requested number of consecutive samples. It exits
`2` on timeout and can write the polling history to JSON.
For final visual-validation captures, pass `-RequireThermalReady` with
`-WaitForThermalReady` so the summary fails unless `thermal_ready_wait.json`
exists and reports `ready=true`.

For a single capture command, pass `-WaitForThermalReady`. The capture writes
`thermal_ready_wait.json`, embeds it in `evidence_summary.json` as
`thermalReadyWait`, and aborts before launch if the wait times out:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "watched-target" `
  -WaitForThermalReady `
  -ThermalReadyBelowStatus 4 `
  -ThermalReadySamples 2 `
  -ThermalReadyTimeoutSeconds 900 `
  -ThermalReadyPollSeconds 30 `
  -Mode Pulse `
  -View Split `
  -Summarize
```

Screenshot content metrics include sampled luminance mean, luminance standard
deviation, dark/light pixel fractions, a `nonBlank` flag, and a portrait
orientation flag. These checks catch blank, near-uniform, or wrongly oriented
captures before spending time on visual review; they do not prove magnification
quality by themselves.

For visual-quality captures, prefer hidden controls or Clean mode. Use
`-Controls $true -Panel Debug` only when the goal is to capture renderer
diagnostics, because the debug overlay can add enough UI jank to make the
preview look worse than the camera/render path actually is. The capture manifest
and summary include a warning when the debug panel is used.

The UI dump is not OCR. It records Android view hierarchy text, which is useful
for proving that the app reported labels such as `Thermal high`, `Full frame
slow`, `Renderer: Live linear EVM reconstruction`, or the active ROI source
during unattended captures.
The capture script retries UI dump collection up to three times because Android
can transiently report `could not get idle state` while the camera preview is
active. If all attempts fail, the bundle still includes screenshots, logs, and
runtime artifacts, but required UI-text assertions will fail because no
view-hierarchy text is available.

Pass `-RequireUiText` with one or more strings when a capture must prove
specific labels are visible in the Android view hierarchy:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "thermal-label" `
  -Mode Pulse `
  -View Split `
  -GlPreview $true `
  -Controls $true `
  -RequireUiText "Thermal high","Renderer: Live linear EVM reconstruction" `
  -TargetDescription "non-sensitive face target" `
  -VisualClaim "Pulse split preview shows live linear reconstruction" `
  -TargetVisible $true `
  -VisualValidated $false `
  -OperatorNotes "Target visible; output not yet accepted as magnified" `
  -Summarize
```

The summary writes `uiTextAssertions` with each required text and whether it was
found. Missing required text adds a warning and makes the summary command exit
with code `3`. Runtime smoke remains separate, so crashes, ANRs, and GL errors
still use the existing runtime-smoke result.

For live renderer validation, pass `-RequireRendererDiagnostics` when the
expanded UI must expose renderer labels such as `Renderer:`,
`GL renderer:`, `Pyramid:`, or `Benchmark:`. For live phase validation, pass
`-RequirePhaseDiagnostics` when the UI dump must contain phase labels such as
`phase:`, `phase fallback`, `phase warmup`, or `phase ready`. Missing required
diagnostic categories add warnings and make the summary command exit with code
`10`.

For watched motion-validation runs, pass `-RequireScreenrecord` with a positive
`-ScreenRecordSeconds` value when a still screenshot would not prove the visual
claim. The summary records whether `screenrecord.mp4` is present, its byte
count, whether it is non-empty, and whether the file has an MP4 `ftyp`
signature near the start. Missing, empty, or invalid required recordings add a
warning and make the summary command exit with code `11`.

For final live-preview evidence, pass `-RequireCameraFps` so the summary fails
unless scoped logcat contains camera HAL `FPS:` samples and the minimum sample
is at least the configured warning threshold, `23.5 FPS` by default. This
prevents final evidence from passing when the camera cadence was missing,
frozen, or too low to judge motion.

Pass `-RequireFocusedApp` for final captured evidence so the summary fails
unless `window_focus.txt` contains the expected app package. This helps catch
captures where the launcher, a permission prompt, or another foreground window
was recorded instead of the app preview.

Use the visual-review fields for watched target runs. `TargetDescription` and
`VisualClaim` describe what was in frame and what the capture is intended to
prove. `TargetVisible` records whether the target is actually visible in the
evidence. `VisualValidated` records whether the operator accepted the visual
claim after inspection. The summary only marks `countsAsVisualValidation` true
when both `TargetVisible` and `VisualValidated` are true.

Pass `-RequireVisualValidation` when a command is meant to close a roadmap
visual gate, pass `-RequireCleanSource` when the evidence must come from a
clean committed source tree, and pass `-RequireNoWarnings` when final evidence
must be free of advisory warnings such as thermal, low-FPS, dirty-source, or
debug-overlay warnings. For normal watched final evidence, prefer
`-RequireFinalVisualEvidence`, which turns on the standard final gates for
clean source, visual validation, screenrecord, thermal readiness, camera FPS,
focused app, and no warnings:

```powershell
.\tools\capture_live_validation_evidence.ps1 `
  -Label "preset-object-vibration" `
  -WaitForThermalReady `
  -Mode ObjectVibration `
  -View Split `
  -RoiSource Manual `
  -GlPreview $true `
  -ScreenRecordSeconds 15 `
  -TargetDescription "watched high-contrast vibrating object inside manual ROI" `
  -VisualClaim "Object preset visibly magnifies localized object vibration" `
  -TargetVisible $true `
  -VisualValidated $true `
  -RequireFinalVisualEvidence `
  -Summarize
```

Use these gates only after the operator has inspected the screenshot or
recording. For pre-inspection captures, keep `VisualValidated` false and omit
`-RequireVisualValidation`; the summary should then report
`target_visible_unvalidated`. If a pre-inspection command is expected to stop at
that exact state, pass `-RequireEvidenceVerdict target_visible_unvalidated` so
the bundle cannot accidentally be treated as final accepted evidence. Omit
`-RequireNoWarnings` for exploratory diagnostics where the warnings themselves
are the evidence.

The summary also writes `evidenceVerdict`, a compact classification for the
bundle. Expected statuses include `runtime_smoke_only`, `visual_validated`,
`target_visible_unvalidated`, `visual_claim_without_target`,
`ui_assertion_failed`, `screenshot_blank`, `wrong_orientation`, and
`runtime_failed`. A prelaunch thermal abort is reported as
`thermal_preflight_aborted`. Only `visual_validated` counts as visual
validation. Pass `-RequireEvidenceVerdict` when a scripted run must produce one
specific status, such as `target_visible_unvalidated` for setup review or
`visual_validated` for final known-target evidence.

Run the summary self-test after editing capture or summary tooling:

```powershell
.\tools\test_live_validation_summary.ps1
```

The test synthesizes a thermal-aborted bundle and an incomplete runtime bundle,
then verifies the summary exit codes, verdicts, UI assertion behavior,
clean-source/visual-validation/verdict/diagnostic/screenrecord/thermal-ready/
camera-FPS/focused-app gates, and dirty source warning.

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
- `-RequireUiText`: comma-separated expected Android view-hierarchy text values.
- `-RequireFinalVisualEvidence`: with `-Summarize`, enables the standard final
  visual evidence gates: clean source, visual validation, screenrecord, thermal
  readiness, camera FPS, focused app, and no warnings.
- `-RequireCleanSource`: with `-Summarize`, fail the summary when source
  metadata is missing or the captured commit had a dirty worktree.
- `-RequireVisualValidation`: with `-Summarize`, fail the summary unless
  `evidenceVerdict.countsAsVisualValidation` is true.
- `-RequireNoWarnings`: with `-Summarize`, fail the summary when any warning is
  present before the no-warnings gate adds its own failure warning.
- `-RequireRoiMeasurement`: with `-Summarize`, fail the summary unless
  `roi_overlay_measurement.json` exists and reports `passed=true`.
- `-RequireScreenrecord`: with `-Summarize`, fail the summary unless
  `screenrecord.mp4` exists, is non-empty, and has an MP4 signature.
- `-RequireThermalReady`: with `-Summarize`, fail the summary unless
  `thermal_ready_wait.json` exists and reports `ready=true`.
- `-RequireCameraFps`: with `-Summarize`, fail the summary unless camera HAL
  `FPS:` samples exist and the minimum sample is at least `-WarnCameraFps`.
- `-RequireFocusedApp`: with `-Summarize`, fail the summary unless
  `window_focus.txt` contains the expected app package.
- `-RequireEvidenceVerdict`: with `-Summarize`, fail the summary unless
  `evidenceVerdict.status` exactly matches the requested status.
- `-RequireRendererDiagnostics`: with `-Summarize`, fail the summary unless
  `uiDump.rendererLabels` contains at least one renderer diagnostic label.
- `-RequirePhaseDiagnostics`: with `-Summarize`, fail the summary unless
  `uiDump.phaseLabels` contains at least one phase diagnostic label.
- `-TargetDescription`: short description of the visible target/setup.
- `-VisualClaim`: short claim this evidence is intended to prove.
- `-TargetVisible`: whether the target is visible in the screenshot/recording.
- `-VisualValidated`: whether the operator accepted the visual claim.
- `-OperatorNotes`: free-form watched-run notes.
- `-WarnPreflightThermalStatus`: Android thermal status threshold for prelaunch
  warnings; defaults to `2` (`moderate`).
- `-AbortPreflightThermalStatus`: Android thermal status threshold for aborting
  before app launch; defaults to `4` (`critical`).
- `-AllowThermalLaunch`: overrides the preflight abort. Use only for deliberate
  diagnostics where extra device heat is acceptable.
- `-WaitForThermalReady`: polls thermal state before preflight and aborts before
  app launch if the device does not become ready.
- `-ThermalReadyBelowStatus`, `-ThermalReadySamples`,
  `-ThermalReadyTimeoutSeconds`, and `-ThermalReadyPollSeconds`: thresholds
  forwarded to the thermal wait helper.
- `-PreserveLogcat`: keeps existing device logcat instead of clearing it before
  the capture starts.
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
- Live phase diagnostics: `evidence_summary.json` should include phase status
  lines under `uiDump.phaseLabels`, such as phase fallback, warmup, ready, or
  processing-size summaries.
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
