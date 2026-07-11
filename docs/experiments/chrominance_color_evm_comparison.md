# Chrominance Color EVM Comparison

Date: 2026-07-11

Milestone: AQ - Chrominance Color EVM

## Summary

This local comparison uses a deterministic synthetic skin pulse with mild
background exposure flicker. It compares three recorded-side color
amplification strategies:

- `GreenOnly`: amplifies only the green-channel delta.
- `Rgb`: amplifies RGB deltas equally.
- `Chrominance`: keeps current luminance and amplifies YIQ `I/Q` chroma deltas.

## Metrics

- `targetChromaResponse`: mean chroma distance from the base frame inside the
  synthetic skin target.
- `backgroundPumping`: mean background chroma plus luminance change outside the
  target.
- `backgroundLuminanceShift`: background luminance change outside the target.
- `responseToPumpRatio`: target chroma response divided by background pumping.

The useful direction is higher target response, lower background pumping, and
lower luminance shift.

## Local Result

JVM coverage asserts that chrominance amplification improves the target
response-to-background-pumping ratio versus full RGB amplification and produces
lower background luminance shift than both RGB and green-only amplification.
Green-only can create a large chroma distance on this fixture, but it does so
by pushing one channel and increasing background luminance pumping.

This is a synthetic recorded-side comparison only. It does not yet replace the
recorded renderer, tune clamps, or validate on phone camera input.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.analysis.ChrominanceAmplificationComparisonTest"
```
