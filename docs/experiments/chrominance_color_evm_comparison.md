# Chrominance Color EVM Comparison

Date: 2026-07-11

Milestone: AQ - Chrominance Color EVM

## Summary

This local comparison uses a deterministic synthetic skin pulse with mild
background color/lighting flicker. It compares four recorded-side color
amplification strategies:

- `GreenOnly`: amplifies only the green-channel delta.
- `Rgb`: amplifies RGB deltas equally.
- `Chrominance`: keeps current luminance and amplifies YIQ `I/Q` chroma deltas.
- `RoiWeightedChrominance`: uses full chroma amplification inside the target ROI
  and dampens chroma amplification outside it.

## Metrics

- `targetChromaResponse`: mean chroma distance from the base frame inside the
  synthetic skin target.
- `backgroundPumping`: mean extra background chroma plus luminance change added
  by amplification outside the target.
- `backgroundLuminanceShift`: extra background luminance change added by
  amplification outside the target.
- `responseToPumpRatio`: target chroma response divided by background pumping.

The useful direction is higher target response, lower background pumping, and
lower luminance shift.

## Local Result

JVM coverage asserts that chrominance amplification improves the target
response-to-background-pumping ratio versus full RGB amplification and produces
lower background luminance shift than both RGB and green-only amplification.
Green-only can create a large chroma distance on this fixture, but it does so
by pushing one channel and increasing background luminance pumping.

The ROI-weighted chrominance strategy keeps target chroma response within 95%
of full-frame chrominance while reducing background pumping by more than half
on the synthetic fixture. This makes it the preferred recorded-side candidate
for the next production renderer slice.

This is a synthetic recorded-side comparison only. It does not yet replace the
recorded renderer, tune clamps, or validate on phone camera input.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.ChrominanceAmplificationComparisonTest"
```
