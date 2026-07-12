# Pixel 8a Parity Preset Preview Benchmark

Date: 2026-07-12

Device: Pixel 8a, Android 16

Command:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.dnrohr.eulerianmagnification.ParityPresetBenchmarkInstrumentedTest"
```

Artifacts:

- Device: `/sdcard/Download/eulerian-preset-benchmark/preset_benchmark.csv`
- Device: `/sdcard/Download/eulerian-preset-benchmark/preset_benchmark.json`
- Pulled local copy: `sample-videos/exports/eulerian-preset-benchmark/`

## Results

| Preset | Mode | Band | View | Frames | Janky Frames | Janky % | Median | P90 | P95 | P99 | Thermal | Recording Samples | Recording Drops | Recording Stability |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | ---: | ---: | --- |
| Pulse color | Pulse | 0.7-3 Hz | Amplified | 127 | 20 | 15.748 | 13 ms | 17 ms | 20 ms | 36 ms | none | 30 | 0 | metadata ok |
| Breathing | Breathing | 0.1-0.6 Hz | Difference | 108 | 9 | 8.333 | 11 ms | 15 ms | 19 ms | 23 ms | none | 30 | 0 | metadata ok |
| Object vib | Object | 3-12 Hz | Split | 108 | 13 | 12.037 | 11 ms | 18 ms | 19 ms | 24 ms | none | 30 | 0 | metadata ok |
| Fast tremor | Fast Motion | 4-12 Hz | Split | 106 | 3 | 2.830 | 11 ms | 13 ms | 14 ms | 19 ms | none | 30 | 0 | metadata ok |

## Notes

- This is a short preview benchmark. It launches each locked preset, lets the
  app run briefly, and reads `dumpsys gfxinfo` plus Android thermal status.
- The recording probe writes 30 monotonic samples through
  `ProcessedRecordingSession` for each preset and verifies zero dropped-frame
  estimates in the metadata path.
- It does not validate visual parity, target quality, camera dropped frames, or
  encoded MP4 correctness for each preset.
- Pulse color showed the highest jank in this unattended run.
- Thermal status remained `none` during the run.
