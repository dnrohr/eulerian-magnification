# Live Full-Frame Preview Bridge

Date: 2026-07-10

## Purpose

Start milestone AE without phone access by making the live GL preview path able
to show a full-frame Pulse color output and by adding policy safeguards that keep
non-Pulse modes truthful.

## What Changed

- Added `LiveEvmPreviewPolicy`.
- Added `ColorMagnificationUniforms.fullFrameMode`.
- Added `uFullFrameMode` to the GL color shader.
- Suppressed the Compose ROI tint overlay when the GL full-frame Pulse color
  preview is active.
- Added an expanded-controls preview label so the user can distinguish
  `Full-frame color preview` from `ROI signal preview`.

## Policy

The bridge enables full-frame GL color preview only when:

- GL preview is active.
- Mode is `Pulse`.
- View is `Amplified` or `Split`.
- GL timing is still settling, or settled timing stays at least `24 fps` and at
  most `42 ms` per frame.

`Raw` and `Difference` remain diagnostic views. Breathing and Fast Motion remain
ROI signal previews until live spatial reconstruction or phase rendering is
implemented.

## Current Limitation

This is a full-frame color-preview bridge driven by the live ROI pulse signal.
It is not yet the MIT-style pyramid/filter/amplify/reconstruct live renderer.
The recorded-video path has that CPU renderer; the live GL path still needs
temporal pyramid reconstruction or phase-motion integration before AE can be
marked complete.

## Next Reconstruction Slice

`LivePyramidReconstructionPlan` and `LivePyramidShaderSource` now define the
planned downsample, temporal bandpass, and reconstruction shader contracts. This
is still source-level scaffolding: `CameraOesRenderer` must still connect the
pass graph before the live preview is true reconstructed EVM output.

`GlTemporalState` now matches the temporal bandpass contract: each pyramid level
has lowpass and highpass ping-pong textures plus a current bandpass texture, and
can bind those outputs for a future multiple-render-target shader pass.

## Live Reconstruction Pass Graph

`CameraOesRenderer` now compiles and invokes the live pyramid reconstruction
passes whenever the existing full-frame preview policy enables GL full-frame
output. The path writes temporal lowpass, highpass, and bandpass outputs into
half-float render targets, then reconstructs the processed target from the
current RGB frame plus amplified bandpass pyramid levels.

This is locally build-verified only. Phone validation was not run because the
device is unavailable today, so AE remains open until the Pixel portrait check
confirms the image is upright, nonblank, and visibly magnified.

The temporal path now checks whether the GL context supports renderable
half-float or float color buffers before allocating signed bandpass targets. If
unsupported, the renderer intentionally falls back to the earlier GL color
preview bridge.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.LiveEvmPreviewPolicyTest" --tests "com.dnrohr.eulerianmagnification.gl.ColorMagnificationPassTest"
```

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.gl.LivePyramidReconstructionTest"
```

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.gl.GlTemporalStateTest" --tests "com.dnrohr.eulerianmagnification.gl.LivePyramidReconstructionTest"
```

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.gl.*"
```

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.gl.GlRenderTargetTest"
```

Result: pass.

Phone validation was intentionally skipped because the device is unavailable
today.
