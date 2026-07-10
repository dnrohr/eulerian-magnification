# Recorded Riesz Phase Motion Renderer

Date: 2026-07-10

## Purpose

Promote the Riesz/phase work from reference scripts and shader-source
scaffolding into an executable app renderer path.

## Implementation

`RieszPhaseMotionRenderer` is a recorded-frame CPU renderer used by
`RecordedVideoProcessor` for non-Pulse `Amplified` and `Split` exports.

For each RGB frame it:

- computes normalized luminance
- applies 3x3 Riesz-like x/y derivative kernels
- estimates a dominant orientation
- projects local phase and amplitude
- unwraps per-pixel phase over time
- applies temporal low/high filtering using the selected mode band
- amplifies the bandpassed phase
- reconstructs luminance from phase/amplitude and applies the luminance delta
  back to RGB

Pulse recorded exports still use `FullFrameLinearEvmRenderer`; Fast Motion,
Object, and Breathing recorded amplified/split exports now use the phase-motion
renderer.

## Validation

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.RieszPhaseMotionRendererTest" --tests "com.dnrohr.eulerianmagnification.analysis.RecordedVideoProcessorTest"
```

Result: pass.

The tests cover:

- stationary high-contrast edge frames remain stable
- in-band translating edge changes rendered output
- in-band motion produces more renderer output than out-of-band drift
- phase motion clips fewer pixels than linear EVM on the same high-contrast edge
- `RecordedVideoProcessor` routes Fast Motion amplified output through the phase
  path

## Scope

This is a recorded-output path. Live GL phase rendering is still future work,
and phone validation was intentionally skipped because the device is unavailable
today.
