# Recorded Full-Frame EVM Parity Validation

Date: 2026-07-10

## Purpose

Validate the recorded full-frame linear EVM renderer added in milestone AC with
repeatable local checks before returning to phone-side MP4 decoding/export.

## Local Automated Checks

`RecordedEvmParityValidator` processes timestamped `RgbFrame` sequences through
`FullFrameLinearEvmRenderer` and compares reconstructed output frames against
the source frames. It reports:

- frame count
- changed frame count
- mean absolute RGB-channel delta
- maximum RGB-channel delta
- changed-pixel fraction
- newly clipped-pixel fraction
- pass/fail reasons

The current JVM parity tests cover:

- stationary flat field: must remain unchanged
- synthetic pulse color at `1.2 Hz`, Pulse mode, amplification `4.0`
- synthetic translating edge at `6.0 Hz`, Fast Motion mode, amplification `0.5`
- failing-expectation reporting

## Results

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.RecordedEvmParityValidatorTest"
```

Result: pass.

The first trial used AC-era validation gains of `20.0` for color and `12.0` for
motion. Those settings produced strong response but excessive clipping:

- synthetic pulse color: `meanAbsDelta=36.418889`,
  `changedPixels=0.953333`, `clippedPixels=0.793333`
- synthetic translating edge: `meanAbsDelta=64.700833`,
  `changedPixels=0.918333`, `clippedPixels=0.645000`

The validator was then used to tune sample-validation gains downward:

- Pulse color validation gain: `4.0`
- Fast Motion translating-edge validation gain: `0.5`

This keeps visible reconstructed-frame response while preventing the validator
from accepting heavy clipping artifacts.

## MIT / Riesz Reference Checks

Existing local decoded MIT baby luminance frames remain valid through the Riesz
reference path:

```powershell
python -m unittest discover -s tools\riesz_reference\tests
python tools\riesz_reference\validate_sample_sequences.py
python tools\riesz_reference\validate_decoded_sample.py sample-videos\exports\mit-evm-baby-riesz-frames.json
```

Results:

```text
Ran 20 tests in 0.306s - OK
PASS stationary flat field: 4 frames, linear_delta=0.000000, phase_delta=0.000000, ratio=0.000000, max_orientation_error=0.000000
PASS vertical edge translating right: 5 frames, linear_delta=0.116667, phase_delta=0.077858, ratio=0.542655, max_orientation_error=0.000000
PASS horizontal edge translating down: 5 frames, linear_delta=0.175000, phase_delta=0.116786, ratio=0.542655, max_orientation_error=0.000000
PASS mit-evm-baby.mp4: 60 frames, 59 pairs, linear_delta=0.001602, phase_delta=0.000472, ratio=0.294765, linear_roughness=0.071753, phase_roughness=0.071722
```

## Local Euler Device Harness Check

Date: 2026-07-12

After the live phase ROI renderer was wired through visible preview output, the
repeatable pre-phone-validation gate was rerun:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.ParityHarnessTest" --tests "com.dnrohr.eulerianmagnification.analysis.RecordedEvmParityValidatorTest" --tests "com.dnrohr.eulerianmagnification.analysis.ParityHarnessArtifactWriterTest"
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.dnrohr.eulerianmagnification.analysis.ParityHarnessInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.sampleId=local-euler" "-Pandroid.testInstrumentationRunnerArguments.sampleAssetName=euler.mp4" "-Pandroid.testInstrumentationRunnerArguments.outputDirPath=/sdcard/Download/eulerian-parity-output"
```

Results:

- JVM synthetic parity checks passed, including synthetic translating-edge
  motion through the recorded Riesz phase renderer.
- Pixel 8a connected parity harness passed for `local-euler`.
- `local-euler` source SHA-256:
  `BF549FEAA994104817A6AFCC39037FB80A013D4074E0AC00EC167F4471B0ACBF`.
- `local-euler` decoded `36` frames at `51x90`.
- Generated artifact set on device:
  `/sdcard/Download/eulerian-parity-output/local-euler/`.
- The pulled ignored manifest reported:
  - Raw: `meanAbsDelta=0.000000`, `changedPixelFraction=0.000000`.
  - Amplified: `renderer=recorded_linear_evm`,
    `meanAbsDelta=12.500363`, `changedPixelFraction=0.710899`,
    `clippedPixelFraction=0.000000`.
  - Difference: `renderer=roi_signal_diagnostic`,
    `meanAbsDelta=122.949304`, `changedPixelFraction=1.000000`,
    `clippedPixelFraction=0.001386`.
  - Split: `renderer=recorded_linear_evm`, output `102x90`,
    `meanAbsDelta=12.500363`, `changedPixelFraction=0.710899`,
    `clippedPixelFraction=0.000000`.

This validates the synthetic moving-edge and local recorded-sample gate before
live Pixel camera validation. It does not prove live camera phase magnification;
the remaining AR validation must use a controlled object-motion setup on the
Pixel 8a and document expected live artifacts.
