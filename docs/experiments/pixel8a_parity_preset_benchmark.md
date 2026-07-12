# Pixel 8a Parity Preset Preview Benchmark

Date: 2026-07-12

Device: Pixel 8a, Android 16

Command:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.dnrohr.eulerianmagnification.ParityPresetBenchmarkInstrumentedTest" "-Pandroid.testInstrumentationRunnerArguments.outputDirPath=/sdcard/Download/eulerian-preset-benchmark"
```

Artifacts:

- Device: `/sdcard/Download/eulerian-preset-benchmark/preset_benchmark.csv`
- Device: `/sdcard/Download/eulerian-preset-benchmark/preset_benchmark.json`
- Pulled local copy: `sample-videos/exports/eulerian-preset-benchmark/`

## Results

| Preset | Mode | Band | View | Frames | Janky Frames | Janky % | Median | P90 | P95 | P99 | Thermal | Recording Stability |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |
| Pulse color | Pulse | 0.7-3 Hz | Amplified | 131 | 26 | 19.847 | 13 ms | 19 ms | 22 ms | 125 ms | light | not exercised |
| Breathing | Breathing | 0.1-0.6 Hz | Difference | 105 | 6 | 5.714 | 12 ms | 14 ms | 20 ms | 48 ms | light | not exercised |
| Object vib | Object | 3-12 Hz | Split | 107 | 8 | 7.477 | 11 ms | 14 ms | 17 ms | 23 ms | light | not exercised |
| Fast tremor | Fast Motion | 4-12 Hz | Split | 107 | 8 | 7.477 | 11 ms | 15 ms | 16 ms | 25 ms | light | not exercised |

## Notes

- This is a short preview benchmark. It launches each locked preset, lets the
  app run briefly, and reads `dumpsys gfxinfo` plus Android thermal status.
- It does not validate visual parity, target quality, dropped camera frames, or
  processed recording stability.
- Pulse color showed the highest jank and a large p99 spike in this unattended
  run. Treat that as a prompt for a watched follow-up rather than a parity
  failure by itself.
- Thermal status reached `light` during the run.
