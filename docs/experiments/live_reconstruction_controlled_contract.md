# Live Reconstruction Controlled Contract

Date: 2026-07-11

Milestone: AP - True Live Linear EVM

## Summary

The phone is unavailable, so this slice verifies the live reconstruction path as
a local structural contract rather than as camera-visible output.

`LiveReconstructionContract.evaluate()` checks that the current live GL path has
enough spatial and temporal structure to be a plausible full-frame linear EVM
renderer:

- at least three active pyramid levels
- per-level temporal lowpass/highpass/bandpass targets
- Pulse starts reconstruction at level `0`
- Breathing starts reconstruction at level `1`
- the reconstruction shader samples `uBandpassTexture0/1/2`
- the reconstruction shader does not consume the old ROI scalar
  `uAmplifiedSignal`
- the reconstruction shader uses Laplacian-style deltas

## Local Result

The local report for a 1280x720 surface is:

```text
Live reconstruction contract: passed, 3 levels, 15 temporal targets, Pulse starts L0, Breathing starts L1
```

This means the current live GL path is no longer merely an ROI tint or single
scalar color bridge by contract. It still does not prove that the Pixel preview
visibly magnifies a real target; that needs a phone run with a known color pulse
and slow-motion setup.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.gl.LiveReconstructionContractTest"
```

Phone validation was not run because the phone is currently unavailable.
