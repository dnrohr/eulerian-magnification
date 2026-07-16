# Eulerian Magnification

Native Android prototype for real-time Eulerian video and motion magnification on Pixel 8a-class hardware.

This is a visualization and research prototype. It must not make medical-grade
heart-rate, breathing-rate, tremor, or diagnostic claims without separate
validation.

## Current App

- Kotlin Android app with Jetpack Compose, CameraX, ML Kit face detection, and
  optional OpenGL ES preview.
- Live ROI analysis for pulse-color and early breathing/motion experiments.
- Modes: Object/Motion (`3.0-12.0 Hz`), Pulse (`0.7-3.0 Hz`), and Breathing
  (`0.1-0.6 Hz`).
- Views: Raw, Amplified, Difference, and Split.
- Manual ROI selection, face ROI smoothing/tracking, basic translation estimate,
  quality warnings, and amplification/noise guardrails.
- Debug processed MP4 recording with sidecar metadata JSON.
- Offline recorded-frame validation, recorded full-frame linear EVM processing,
  and a Python Riesz reference pyramid for phase-mode work.

## What To Expect

The current live app is best understood as a research viewer, not a finished
motion-magnification camera.

- Live Pulse mode currently shows color/tint changes inside the ROI. A good
  live result is a subtle rhythmic color change in a skin patch, not visible
  movement of the face.
- GL Pulse `Amplified`/`Split` can use an early full-frame color preview bridge
  when timing is healthy. This is still signal-driven color output, not live
  MIT-style pyramid reconstruction.
- Expanded controls and exports now separate the measured signal source,
  renderer, and visualization style, so mode names no longer have to imply that
  every renderer is equally mature.
- Recorded-video `Amplified` and `Split` exports now run full-frame linear EVM
  for Pulse and recorded Riesz phase motion for non-Pulse motion modes. These
  are the first paths that should look like true frame processing rather than an
  ROI overlay.
- Breathing mode estimates low-frequency vertical translation and shows the
  breathing signal in the expanded controls. It does not yet warp the preview to
  make chest motion visually larger.
- Fast Motion exposes a higher-frequency band and quality warnings,
  but the live preview is still driven mostly by ROI color/tint visualization.
- The Riesz/phase motion work now has a recorded CPU renderer, but it is not yet
  the primary live preview renderer.

If the only obvious effect you see is ROI flicker or color flicker, that is a
limitation of the current implementation rather than a sign that you are using
it wrong.

## MIT Parity Roadmap

The roadmap now treats MIT-style Eulerian Video Magnification parity as explicit
work rather than an implied future outcome:

- Define parity targets, samples, settings, and acceptance criteria.
- Build a recorded full-frame linear EVM renderer that runs
  pyramid/filter/amplify/reconstruct over whole frames.
- Validate against MIT-style color and motion samples.
- Integrate a live full-frame EVM preview path.
- Promote the existing Riesz/phase reference work into a real app renderer.
- Rework mode/visualization labels once true color and motion renderers exist.

The first target definition is tracked in
`docs/testing/MIT_PARITY_TARGETS.md`.

## Live Usage Guide

1. Mount the phone or hold it as still as possible.
2. Launch the app and grant camera permission.
3. Start with the default motion mode, `Amplified`, and the compact preview.
   Switch to `Pulse` when you specifically want color/skin-pulse behavior.
4. Let exposure settle for a few seconds, then tap `Controls` and use
   `Lock AE/AWB`.
5. Leave `ROI Source` on `Auto ROI` for the default path. Choose `Full frame`
   only for controlled tests where FPS stays healthy and the phone is not
   thermally hot, or `Manual ROI` when you need a deliberately placed box for a
   difficult target, debugging, or comparison.
6. Tap `Hide` so the image is mostly unobstructed.
7. Use `Raw`, `Amplified`, `Difference`, or `Split` from the compact preview to compare whether the
   processed view is adding useful signal or only noise.
8. Tap `Clean` when you want the least obstructed preview. Tap `Controls` to return.
9. Use short recordings and sample videos for processing checks before trusting live
   handheld behavior.

### Best Pulse Setup

- Put the phone on a stand, front camera facing you.
- Use bright, steady, diffuse light. Avoid flickering LEDs and sunlight patches.
- Fill much of the frame with your face.
- Select a stable forehead or cheek ROI.
- Stay still and avoid talking, smiling, or moving your head.
- Expect a subtle color pulse, not motion.

Pulse color magnification is intentionally conservative. The app may dampen or
hide color amplification when lighting is still settling, the ROI is too dark,
LED flicker or exposure pumping is likely, the ROI is moving, or skin pixels are
near channel clipping. In recorded exports, `signal_timeline.csv` includes
`colorGate`, `colorGateGain`, and `saturatedPixelFraction` so you can tell when
the app reduced the effect on purpose.

### Best Breathing Setup

- Put the phone on a stand several feet away.
- Frame the upper torso or shoulder area.
- Select a manual ROI on clothing or a visible chest/shoulder edge.
- Breathe normally and keep the phone still.
- Watch the breathing value/waveform in expanded controls. The preview itself is
  not yet a full motion-warped result.

### Best Fast Motion Setup

- Use a stable phone mount and a high-contrast object.
- Good test targets are a black stripe on white paper, a ruler edge, or a small
  tag attached to a speaker, fan, or vibrating surface.
- Use `Motion` for high-frequency biological or small mechanical motion
  experiments.
- Keep the camera and background still. Small camera motion can dominate these
  bands.

## UI Reference

The default screen is intentionally compact so the preview remains visible.

- `Controls`: opens the full controls and diagnostics panel.
- `Hide`: closes the full panel and returns to the compact preview.
- `Clean`: hides the compact mode, ROI, quality, FPS, and view controls so the
  image is easier to inspect. A small `Controls` button remains available.
- Compact sparkline: shows the active signal history under the view buttons when
  enough samples are available. It is hidden in Clean preview.
- `Pulse demo`, `Breathing demo`, `Object demo`, `Motion demo`:
  expanded-controls presets that apply locked mode, view, amplification, band,
  and setup guidance together before you follow the matching recipe.
- `Pulse setup`, `Breathing setup`, `Fast motion setup`: expanded-controls
  recipes that name the best target, stabilization steps, and realistic expected
  output for the active mode.
- `Color amp`, `Breath sig`, `Motion exp`: compact output labels
  that state what kind of processing is active.
- `Object`, `Motion`, `Pulse`, `Breath`: select the temporal band and analysis
  preset. The app defaults to the motion path so the first-run experience is
  aimed at visible movement rather than color-only pulse tinting.
- `Raw`: shows the camera preview without the app's amplified tint.
- `Amp`: shows the current amplified/tinted visualization.
- `Diff`: shows the magnitude of the added signal, which is useful for spotting
  noise and flicker. Warm/red-orange means positive signal, blue means negative
  signal, and darker output means weak or no signal. Recorded Difference exports
  keep dim raw context outside the ROI.
- `Split`: shows raw and processed views side by side in the GL preview path.
  Selecting `Split` automatically uses GL preview when available so the live
  comparison is raw on the left and processed on the right.
- `Amplification`: scales the measured signal. Higher values are easier to see
  but amplify noise and camera motion.
- `Band`: displays the active frequency range in Hz.
- `Lock AE/AWB`: locks auto-exposure and auto-white-balance after the camera has
  settled. This helps reduce brightness/color pumping.
- `Unlock AE/AWB`: returns exposure and white balance to camera auto mode.
- `Reset Settings`: clears saved preferences and returns mode, view,
  amplification, preview path, and AE/AWB lock to first-launch defaults. Manual
  ROI is intentionally not persisted.
- `Quality Cues`: opt-in haptic feedback for major quality changes. It is off
  by default, rate-limited, and skipped when system haptic feedback is disabled.
- `ROI Source`: chooses how the app selects the measured region. `Auto ROI`
  uses face tracking when available and falls back to the center; it is the
  first-launch/default source because it is lighter for live preview. `Full
  frame` uses the whole image and is available for controlled tests when FPS
  stays healthy. `Manual ROI` uses one explicit box for difficult targets,
  debugging, and repeatable experiments.
- `Edit ROI`: switches to `Manual ROI` and enables manual ROI placement by
  dragging on the preview. Normal viewing ignores preview drags so accidental
  touches do not move the ROI.
- `Done ROI`: exits ROI edit mode and hides corner handles.
- `Clear ROI`: removes the manual ROI and returns to automatic/center ROI.
- `Use GL Preview`: switches to the OpenGL preview path when available. GL is
  the GPU preview/processing path used for live Split and phase-motion work; it
  is the path expected to become primary for motion magnification.
- `Use CameraX Preview`: returns to the standard CameraX preview and CPU
  analysis path. CameraX remains useful as a stable camera baseline and fallback,
  even as motion rendering moves toward GL.
- `Clean` / `Annotated`: chooses the live recording style. Clean records the
  processed GL preview texture without app controls when GL preview is active;
  without GL preview, it falls back to annotated evidence frames. Annotated
  always records labels, ROI, signal, mode, FPS, and latency for validation.
- `Start Recording` / `Stop Recording`: records a processed MP4 plus metadata
  JSON in app storage.
- `Process Video`: selects a recorded/sample video and runs the offline
  processing/export path. Use this for `sample-videos/euler.mp4` after copying
  it to the phone or another picker-visible location. In `Amplified` and
  `Split`, recorded exports use full-frame linear EVM reconstruction; in
  `Difference`, the export shows a signed ROI diagnostic over dim context. The
  export writes a processed MP4, metadata JSON, `signal_timeline.csv`, and
  `evidence_report.html`.
- Ten-second validation flow: defines a setup/countdown/recording/processing/
  review contract for guided short runs. Live one-tap capture still needs phone
  verification, but the required evidence bundle and state transitions are
  tested in code.
- `Share Metadata`: shares the JSON metadata for the latest recording.
- Recent recordings: shows the newest app-owned processed sessions with a short
  quality/artifact summary plus `Metadata`, `Video`, `Report`, and `Delete`
  actions where artifacts exist. Delete removes only that app-owned `processed-*`
  session directory.
- `Quality`: summarizes whether the current run looks usable. Warnings can come
  from low FPS, camera/ROI motion, lighting flicker, timestamp jumps, thermal
  state, or risky high-frequency/amplification combinations. The compact
  preview stays terse; expanded controls show a short action for each warning.
  If the phone reaches `severe` thermal state or worse, live full-frame
  reconstruction falls back to ROI signal preview and `ROI Source` is switched
  back to `Auto ROI`.
- `Lighting`: expanded-controls diagnostic for lighting stability. It separates
  settling, stable, too dark, likely flicker, exposure pumping, and lighting
  changes mixed with ROI motion. Live recording metadata includes the lighting
  diagnostic when available. Recorded Pulse exports use these diagnostics to
  dampen color amplification when the input is likely unreliable.
- Experimental rate estimates: recorded-video summaries and metadata can include
  a pulse or breathing rate only when strict quality gates pass. Estimates are
  hidden when timing, ROI, lighting, motion, FPS, or signal strength is not good
  enough, and visible estimates are non-diagnostic.
- `Output`: expanded-controls label that spells out the active pipeline:
  `Color amplification`, `Breathing signal`, or
  `Experimental fast-motion analysis`.
- `Manual ROI`, `Full frame`, `Tracking`, `Frozen ROI`, `Center ROI`: compact
  status label showing whether the app is using your selected region, the whole
  frame, an actively detected face region, the last good detected region, or the
  center fallback.

Only one ROI outline should be visible when a manual ROI is set. If automatic
tracking briefly loses the face, the app holds the last good region and labels it
`Frozen ROI` instead of immediately jumping to the center. If the automatic ROI
appears far from the visible face for a sustained period, that likely indicates a
preview-to-analysis coordinate mapping issue rather than a good target choice.

## Modes And Frequency Bands

The presets are broad first-pass bands:

- `Pulse`: `0.7-3.0 Hz`, roughly 42-180 beats per minute.
- `Breath`: `0.1-0.6 Hz`, roughly 6-36 breaths per minute.
- `Object`: `3.0-12.0 Hz`, intended for small mechanical vibration with a
  visible high-contrast edge.
- `Fast Motion`: `4.0-12.0 Hz`, intended for high-frequency biological or small
  mechanical motion experiments.

Object vibration and Fast Motion intentionally overlap. At this stage the setup,
target, renderer path, and validation evidence matter more than the label:
object vibration should use a controlled high-contrast mechanical target, while
Fast Motion is for tremor-like or other small high-frequency motion where camera
motion must be lower than target motion.

## Preset Validation Status

As of 2026-07-12, all four locked presets have Pixel 8a short-run benchmark
evidence for rendered frame timing, thermal status, processed-recording metadata,
and encoded MP4 validity. None of the presets has a watched-target visual parity
artifact yet, so they should be treated as benchmark-validated setup presets,
not final MIT-parity visual claims.

| Preset | Pixel Benchmark | Recording Metadata | Encoded MP4 | Visual Parity |
| --- | --- | --- | --- | --- |
| Pulse color | Validated | Validated | Validated | Not yet validated |
| Breathing | Validated | Validated | Validated | Not yet validated |
| Object vibration | Validated | Validated | Validated | Not yet validated |
| Fast tremor | Validated | Validated | Validated | Not yet validated |

Watched visual parity must be closed through the strict live evidence flows:
Pulse color and Breathing use
`docs/experiments/pixel8a_live_linear_validation.md`; high-frequency Motion /
Object setups use `docs/experiments/pixel8a_live_phase_validation.md`; ROI
alignment uses `docs/testing/ROI_DEVICE_VALIDATION.md`. Final accepted evidence
should use `-RequireFinalVisualEvidence` and any domain-specific gate such as
`-RequireRendererDiagnostics`, `-RequirePhaseDiagnostics`, or
`-RequireRoiMeasurement`. Connected Pixel evidence should also use
`-RequireDeviceSerial 47091JEKB05516` so accepted bundles cannot be confused
with emulator or alternate-device captures.

## Requirements

- Windows with PowerShell.
- Android Studio or the Android SDK installed.
- JDK compatible with the checked-in Gradle/Android plugin configuration.
- Pixel 8a-class Android device for camera, GL, encoder, and thermal validation.

The repo ignores local media under `sample-videos/`. Keep downloaded public clips
and phone recordings there rather than committing them. The current sample
catalog is documented in `docs/testing/SAMPLE_VIDEO_SOURCES.md`; it records
sample IDs, local paths, hashes, recommended modes, and redistribution notes
without bundling the videos into the app.

The app stores the last durable test setup in SharedPreferences: mode, view
mode, amplification, requested preview path, AE/AWB lock preference, recording
output mode, ROI source, and the opt-in quality-cue preference. First launch and
`Reset Settings` prefer the motion/GL/Auto ROI path when available. It does not store
transient ROI placement, signal history, validation summaries, or recording
state.

## Build

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\tools\test_offline_project_tooling.ps1
```

The phone-free tooling suite also runs in GitHub Actions on pushes to `main`
and pull requests.

Before a connected Pixel validation pass, print the current device-session
checklist and command templates:

```powershell
.\tools\show_next_pixel_validation_plan.ps1
```

For a device-session handoff bundle, write the plan, closeout summary, and
paste-ready command list together:

```powershell
.\tools\prepare_pixel_validation_handoff.ps1 `
  -OutputRoot sample-videos\exports\live-validation `
  -DeviceSerial 47091JEKB05516
```

The plan includes the current closeout blocker count and next commands from
missing evidence slots. Pass `-EvidenceRoot <path>` to review a specific
live-validation export folder, or `-NextOnly` to print only the recommended captures
needed by the current closeout blockers. Add `-Slot pulseLinear` or another
closeout slot id to focus the recommended capture queue on one validation target;
the plan prints available missing slots when a slot filter does not match. Use
`-CaptureStage Setup` or `-CaptureStage Final` to print only pre-inspection or
closing evidence commands, and add `-CommandsOnly` when you want paste-ready
command templates without the surrounding checklist. Add `-FailOnInvalidSlot`
in scripted runs so a mistyped slot filter exits nonzero instead of producing
an empty capture queue, or `-FailOnEmptyQueue` when automation should require
at least one recommended capture. Add `-FailOnPendingReviewSheets` when a
handoff should fail until every captured screenrecord has a matching review
sheet. Add `-FailOnDirtySource` when a handoff should fail if the current
worktree is not clean, or `-FailOnUnpushedSource` when the source commit must
already be reachable from `origin/main`. Add `-OutputPath sample-videos\exports\live-validation\pixel_validation_plan.json`
to save the full machine-readable plan used for a device session. Generated
capture commands default to `-DeviceSerial 47091JEKB05516`; pass
`-DeviceSerial <serial>` to target a different connected device. Generated
commands also add `-RequireDeviceSerial` with the same value, so summaries fail
when a final bundle was not captured from the intended Pixel.
The handoff bundle writes `pixel_validation_plan.json`,
`pixel_closeout_summary.json`, `pixel_validation_commands.txt`,
`live_validation_review_queue.json`, `live_validation_review_commands.txt`,
`live_validation_review_dashboard.html`, the human-readable
`pixel_validation_handoff.md`, and
`pixel_validation_handoff_manifest.json` with SHA-256 hashes for those handoff
artifacts. The handoff records the target device serial, source branch, commit,
clean-tree state, whether the commit is reachable from `origin/main`, and a
compact pending review-sheet issue count. The planner and handoff also include
a `wait_for_device_thermal_ready.ps1` preflight command; run it before watched
phone validation when the device is warm, FPS is low, or the preview looks
nearly frozen. Generated Pixel validation commands wait below thermal status
`3` (`severe`) so full-frame runs do not start in a state where the app will
fall back to Auto ROI.

After a connected Pixel validation pass, summarize which accepted evidence
bundles are ready to close the remaining roadmap items. The summary includes
source branch/commit plus screenshot and screenrecord SHA-256 values for each
accepted bundle, and prints the expected final label for each slot so final
docs can cite the exact visual artifacts:

```powershell
.\tools\summarize_pixel_validation_closeout.ps1
.\tools\summarize_pixel_validation_closeout.ps1 -OutputPath sample-videos\exports\live-validation\pixel_closeout_summary.json
```

Use `pixel_closeout_summary.json` as the saved closeout artifact when updating
README or parity documentation after the gates pass. Satisfied slots include an
`artifactNote` with the bundle, source, screenshot hash, and screenrecord hash
needed for release notes, plus a matching review contact sheet hash when one
exists.
The closeout JSON also includes `closeoutBlockers`, a compact list of missing
slots and accepted-evidence issues that still block roadmap closeout.

For screenrecord review, generate a contact sheet beside a captured bundle:

```powershell
.\tools\export_live_validation_review_sheet.ps1 `
  -BundlePath sample-videos\exports\live-validation\<bundle> `
  -FfmpegPath <path-to-ffmpeg.exe>
```

The helper writes `review_contact_sheet.jpg` and
`review_contact_sheet_manifest.json` with hashes for the source screenrecord and
contact sheet. The next `evidence_summary.json` records the contact sheet under
`artifacts.reviewContactSheet`, and closeout includes its hash in `artifactNote`
when present. `-RequireReviewContactSheet` makes the summary fail unless the
contact-sheet manifest exists and its screenrecord SHA-256 matches the current
`screenrecord.mp4`. This is a review aid only; accepted final evidence still
has to pass the strict summary and closeout gates.

If `ffmpeg` is not available, omit `-FfmpegPath` and the helper will try a
browser fallback through `npx playwright screenshot` using local Chrome. Pass
`-BrowserChannel <channel>` to select a different Playwright channel, or
`-NoBrowserFallback` when automation should fail instead of using the browser
sampler.

To see which captured bundles still need review sheets, run:

```powershell
.\tools\show_live_validation_review_queue.ps1
.\tools\show_live_validation_review_queue.ps1 -CommandsOnly -FfmpegPath <path-to-ffmpeg.exe>
```

The queue reports screenrecord bundles with missing, manifest-less, or
hash-mismatched review sheets and can save JSON with `-OutputPath`. Pending
entries include a `reviewSheetIssue` reason such as `missingContactSheet`,
`missingManifest`, or `screenrecordHashMismatch`.

When `ffmpeg` is not installed, create a local review dashboard instead. It
does not satisfy the review contact sheet gate, but it gives one browser page
with playable recordings, existing sheets, hashes, issue reasons, and
regeneration commands. Each card also shows the captured mode, ROI source,
evidence verdict, target description, visual claim, operator notes, and key
gate status from `evidence_summary.json`:

```powershell
.\tools\export_live_validation_review_dashboard.ps1
.\tools\export_live_validation_review_dashboard.ps1 -PendingOnly -Open
```

Pixel validation handoff bundles also include
`live_validation_review_dashboard.html` for the current pending review-sheet
queue. The Markdown handoff lists each pending review-sheet issue reason and
the exact regeneration command beside the bundle path.

Use the closeout gates before editing visual-validation status in README or
parity docs. `-FailOnPresetDocsNotReady` requires the four preset slots to be
present and rejects unmatched, ambiguous, duplicate, non-`main`, unpushed, or
missing-artifact-hash accepted evidence, plus accepted evidence whose label is
not one of the final capture labels, does not match its closeout slot, lacks
operator notes, lacks target description / visual claim text, or was captured
from a device serial other than the expected Pixel, and accepted final evidence
without a matching review contact sheet.
Wrong-slot reports include the expected final label for each mismatched slot:

```powershell
.\tools\summarize_pixel_validation_closeout.ps1 -FailOnMissing -FailOnUnmatched -FailOnAmbiguous -FailOnDuplicate -FailOnNonMain -FailOnUnpushedSource -FailOnMissingArtifactHashes -FailOnNonFinalLabel -FailOnWrongSlotLabel -FailOnMissingOperatorNotes -FailOnMissingVisualReviewText -FailOnWrongDeviceSerial -FailOnReviewContactSheetIssues
.\tools\summarize_pixel_validation_closeout.ps1 -FailOnCloseoutNotReady
.\tools\summarize_pixel_validation_closeout.ps1 -FailOnPresetDocsNotReady
```

Open the repo in Android Studio, connect a Pixel 8a, and run the `app`
configuration when device testing is needed. From PowerShell, the repo helper
also finds the Android SDK `adb.exe`, installs the debug APK, grants camera
permission, and can launch the app:

```powershell
.\tools\install_debug_on_pixel.ps1 -Build -Launch
```

## Offline Validation First

Use recorded or synthetic inputs before relying on live phone behavior:

- Recorded-video validation flow: `docs/testing/RECORDED_VIDEO_VALIDATION.md`
- MIT parity targets: `docs/testing/MIT_PARITY_TARGETS.md`
- Signal/renderer/visualization model: `docs/architecture/SIGNAL_VISUALIZATION_MODEL.md`
- ROI device validation flow: `docs/testing/ROI_DEVICE_VALIDATION.md`
- Live linear Pixel validation: `docs/experiments/pixel8a_live_linear_validation.md`
- Live phase Pixel validation: `docs/experiments/pixel8a_live_phase_validation.md`
- Public sample plan: `docs/testing/SAMPLE_VIDEO_SOURCES.md`
- Riesz / phase-mode reference: `docs/architecture/RIESZ_MODE.md`

The intended order is:

1. Run JVM/unit tests for deterministic math and timestamp behavior.
2. Validate recorded or public sample clips through the recorded-video path.
3. Move to the phone for CameraX, GLES, encoder, exposure, and thermal checks.

## Device Demo Flow

1. Install and launch the debug app on the Pixel.
2. Grant camera permission.
3. Start in the default motion mode with Amplified view and wait for the preview
   timing to settle near 30 FPS.
4. Open `Controls` and choose `Pulse demo`, `Breathing demo`, or
   `Motion demo` to apply a repeatable preset.
5. Switch between Raw, Amplified, Difference, and Split to inspect the ROI
   visualization.
6. Try Breathing mode with a stable manual ROI around visible torso/shoulder
   motion.
7. Use `Start Recording` for a short run, then stop and share metadata if needed.
8. Toggle `Use GL Preview` for the GPU display path and compare framing, FPS, and
   quality warnings against CameraX preview.

## Device Notes

- The `ROI motion` quality warning can be caused by phone movement, subject
  movement, visible heartbeat/face movement, or tracker drift.
- The debug MP4 is an app-owned processed visualization. Recorded-video
  processing now reconstructs full frames for `Amplified`/`Split`, while the
  live preview-matching camera/GPU recording path is still separate work.
- GL preview currently uses CPU analysis for ROI and signal uniforms while GPU
  color processing/display work continues.
- Public or synthetic recorded-video validation should come before asking for a
  new phone recording unless the path under test is inherently device-only.

## Useful Docs

- `docs/tasks/README.md` - roadmap task files.
- `docs/demo/DEMO_LINKS.md` - public demo references and local demo flow.
- `docs/architecture/CPU_PULSE_COLOR_MVP.md` - CPU analysis and ROI pipeline.
- `docs/architecture/GPU_COLOR_MAGNIFICATION.md` - GL color path status.
- `docs/architecture/RECORDING_PROTOTYPE.md` - recording metadata and debug MP4.
- `docs/architecture/QUALITY_STATUS.md` - quality warning meanings.
- `docs/experiments/pixel8a_camera_notes.md` - Pixel 8a observations.
