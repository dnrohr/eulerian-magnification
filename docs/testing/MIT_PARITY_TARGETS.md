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
The next AO slice should add a file-writing wrapper that decodes `mit-baby` and
`local-euler` and writes reviewable artifacts under an ignored output directory.

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
