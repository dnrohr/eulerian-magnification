# MIT EVM Parity Targets

This document defines what "MIT Eulerian Video Magnification parity" means for
this app. It is intentionally stricter than the current live app behavior: parity
requires full-frame pyramid/filter/amplify/reconstruct output, not ROI tinting or
signal-only overlays.

## Target Classes

| Target | Purpose | First Renderer | Expected Visible Result |
| --- | --- | --- | --- |
| Color pulse | Match MIT-style color amplification on skin/face content. | Recorded full-frame linear EVM | Subtle skin-color variation becomes visibly stronger without face motion. |
| Slow motion / breathing | Match low-frequency full-frame motion magnification. | Recorded full-frame linear EVM, then live preview | Chest/shoulder or soft large-scale movement becomes spatially larger. |
| Small edge/object motion | Match subtle local motion magnification. | Phase/Riesz path after linear baseline | Small edge displacement becomes visibly larger with controlled halos. |

## Initial Samples

Media files stay under ignored `sample-videos/` and must not be committed.

| ID | Local Path | Source / License Handling | SHA-256 | Target Class |
| --- | --- | --- | --- | --- |
| `mit-baby` | `sample-videos/mit-evm-baby.mp4` | MIT CSAIL EVM public example clip from `https://people.csail.mit.edu/mrub/evm/video/baby.mp4`. Treat as reference media only; do not redistribute in this repo. | `2C5E744384AB88FCCD3AA4883959B33EB4CDB7384C3E46E788CEDE821B2478EE` | Slow motion / breathing-like subtle motion |
| `local-euler` | `sample-videos/euler.mp4` | Local user-provided sample. Provenance/license not yet documented, so use only for local validation and do not redistribute. | `BF549FEAA994104817A6AFCC39037FB80A013D4074E0AC00EC167F4471B0ACBF` | Qualitative app regression sample |
| `synthetic-color-pulse` | Generated in tests | Repo-generated deterministic frames. | Not applicable | Color pulse |
| `synthetic-moving-edge` | Generated in tests | Repo-generated deterministic frames. | Not applicable | Small edge/object motion |

## Baseline Settings

| Target | Band | Amplification | Color Space / Channel | Pyramid | Attenuation |
| --- | --- | ---: | --- | --- | --- |
| Color pulse | `0.7-3.0 Hz` | `12x-30x` | Chrominance/green-first baseline | Gaussian/Laplacian, 3-5 levels | Clamp per-level output to avoid clipping and skin-color inversion. |
| Slow motion / breathing | `0.1-0.6 Hz` | `8x-20x` | Luminance or RGB intensity baseline | Laplacian, coarse levels favored | Suppress fine levels that produce shimmer/halos. |
| Small edge/object motion | `3.0-12.0 Hz` | `4x-20x` | Luminance/phase response | Riesz/phase preferred | Gate by amplitude; avoid amplifying flat regions. |

These are starting settings. A parity run should record the exact settings in
`metadata.json`, `signal_timeline.csv`, and `evidence_report.html`.

## Acceptance Criteria

### Color Pulse

- Raw vs processed output shows visible color amplification in skin/face regions.
- Background and static clothing do not show comparable color pumping.
- `signal_timeline.csv` shows nonzero in-band energy.
- Processed frames avoid obvious clipping, posterization, or full-frame flashes.

### Slow Motion / Breathing

- Raw vs processed output shows spatial movement magnified, not just tint.
- Motion remains localized to the moving body/object region as much as possible.
- Halos are limited and documented when present.
- Stationary synthetic frames remain visually stable after processing.

### Small Edge/Object Motion

- A known translating synthetic edge shows larger apparent displacement after processing.
- Flat or low-texture regions do not acquire visible false motion.
- Phase/Riesz output is compared against the linear EVM baseline before promotion.

## Local Commands

Current recorded-video processing is app-driven. The `Amplified` and `Split`
recorded export views now use the CPU full-frame linear EVM renderer; live
preview parity is still separate work. Until a CLI renderer exists:

1. Place sample media under `sample-videos/`.
2. Copy or share the target sample to a picker-visible location when device testing
   is available.
3. Select the closest preset and use `Process Video`.
4. Inspect:
   - `debug_processed.mp4`
   - `metadata.json`
   - `signal_timeline.csv`
   - `evidence_report.html`

For existing Riesz reference validation:

```powershell
python tools\riesz_reference\validate_decoded_sample.py sample-videos\exports\mit-evm-baby-riesz-frames.json
```

For recorded full-frame linear EVM parity checks that do not require Android
media decode:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.RecordedEvmParityValidatorTest"
```

For the deterministic parity harness core:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.ParityHarnessTest"
```

The harness currently runs app-native renderer paths against deterministic
synthetic color-pulse and moving-edge frame sequences, then produces manifest
JSON, timeline CSV, objective metrics, and a compact HTML summary in memory.

For durable synthetic harness artifacts:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.ParityHarnessArtifactWriterTest" -PparityHarnessOutputDir=parity-output
```

This writes ignored evidence bundles to:

- `parity-output/synthetic-color-pulse/`
- `parity-output/synthetic-moving-edge/`

Each bundle contains `manifest.json`, `artifact_index.json`,
`evidence_report.html`, per-view `signal_timeline.csv`, and first/middle/last
PPM frame snapshots for Raw, Amplified, Difference, and Split views.

For decoded-video parity artifacts on a connected device, run the bundled MIT
sample:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.dnrohr.eulerianmagnification.analysis.ParityHarnessInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.sampleId=mit-baby" "-Pandroid.testInstrumentationRunnerArguments.outputDirPath=/sdcard/Download/eulerian-parity-output"
```

The output directory used above is:

```text
/sdcard/Download/eulerian-parity-output/
```

To run the local `euler` sample, copy the ignored local video into ignored
androidTest assets before building the test APK:

```powershell
Copy-Item -LiteralPath sample-videos\euler.mp4 -Destination app\src\androidTest\assets\euler.mp4 -Force
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.dnrohr.eulerianmagnification.analysis.ParityHarnessInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.sampleId=local-euler" "-Pandroid.testInstrumentationRunnerArguments.sampleAssetName=euler.mp4" "-Pandroid.testInstrumentationRunnerArguments.outputDirPath=/sdcard/Download/eulerian-parity-output"
```

Pull results with:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" pull /sdcard/Download/eulerian-parity-output sample-videos\exports\eulerian-parity-output
```

Pixel 8a evidence from 2026-07-11:

- `mit-baby`: 36 decoded frames at 160x90, `recorded_riesz_phase_motion`,
  amplified mean absolute delta `0.314350`, changed-pixel fraction `0.142095`,
  clipped fraction `0.004720`.
- `local-euler`: 36 decoded frames at 51x90, `recorded_linear_evm`, amplified
  mean absolute delta `14.687229`, changed-pixel fraction `0.711002`, clipped
  fraction `0.059150`.

## Locked App Presets

The app exposes four locked parity presets in expanded controls:

| Preset | Mode | Band | View | Amplification | Target |
| --- | --- | --- | --- | --- | --- |
| Pulse color | Pulse | 0.7-3.0 Hz | Amplified | 12x | forehead or cheek skin |
| Breathing | Breathing | 0.1-0.6 Hz | Difference | 16x | torso, shoulder, or clothing edge |
| Object vibration | Object | 3.0-12.0 Hz | Split | 18x | high-contrast vibrating object edge |
| Fast tremor | Fast Motion | 4.0-12.0 Hz | Split | 20x | small high-frequency biological or mechanical motion |

Object vibration and Fast Motion overlap by design. They are separated by setup
and evidence rather than frequency alone: controlled mechanical object tests
should isolate the object from the phone support, while tremor-like tests need a
stable target region and stricter camera-motion rejection. Pixel 8a benchmark
rows still need live captured evidence before these presets can be called
validated.

Short-run Pixel 8a preview benchmark artifacts are documented in
`docs/experiments/pixel8a_parity_preset_benchmark.md`. These cover rendered
frame/jank percentiles, thermal status, processed-recording metadata stability,
and encoded MP4 validity. They do not yet cover visual parity, target quality,
or live camera dropped-frame diagnosis for each preset.

## Preset Validation Status

As of 2026-07-12, the locked presets have automated Pixel 8a benchmark evidence
but not watched-target visual parity evidence:

| Preset | Pixel Preview/Jank Benchmark | Recording Metadata Probe | Encoded MP4 Probe | Visual Parity Artifact |
| --- | --- | --- | --- | --- |
| Pulse color | Validated | Validated | Validated | Not yet validated |
| Breathing | Validated | Validated | Validated | Not yet validated |
| Object vibration | Validated | Validated | Validated | Not yet validated |
| Fast tremor | Validated | Validated | Validated | Not yet validated |

This means the presets are supported for repeatable setup and automated
performance/regression checks. They are not yet supported as final MIT-parity
visual claims. A preset becomes visually validated only after a watched run with
a known target stores a screenshot, processed export, or evidence note showing
the expected visible effect.

Watched visual evidence must use the strict live validation protocols:

- Pulse color and Breathing:
  `docs/experiments/pixel8a_live_linear_validation.md`.
- Object vibration and Fast tremor / public Motion:
  `docs/experiments/pixel8a_live_phase_validation.md`.
- ROI alignment and target mapping:
  `docs/testing/ROI_DEVICE_VALIDATION.md`.

Final visual parity evidence should require `-RequireFinalVisualEvidence` plus
the matching domain-specific gate: `-RequireRendererDiagnostics` for live
linear reconstruction, `-RequirePhaseDiagnostics` for live phase motion, and
`-RequireRoiMeasurement` for ROI alignment. Object vibration remains an
internal/parity setup label; normal public high-frequency validation should use
the Fast Motion / `Tremor` path unless a task explicitly asks for the internal
compatibility mode.

For local file integrity:

```powershell
Get-FileHash sample-videos\mit-evm-baby.mp4 -Algorithm SHA256
Get-FileHash sample-videos\euler.mp4 -Algorithm SHA256
```

## Non-Goals For Parity Claims

- ROI tint overlays do not count as motion magnification.
- A signal graph alone does not count as EVM output.
- Live preview parity cannot be claimed until the live renderer reconstructs or
  warps frames spatially.
- Rate estimates are outside MIT visual parity and remain gated separately.
