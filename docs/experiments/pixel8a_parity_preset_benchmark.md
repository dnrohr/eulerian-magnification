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

| Preset | Mode | Band | View | Frames | Janky Frames | Janky % | Median | P90 | P95 | P99 | Thermal | Recording Samples | Recording Drops | Recording Stability | MP4 Valid | MP4 Bytes |
| --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | ---: | ---: | --- | --- | ---: |
| Pulse color | Pulse | 0.7-3 Hz | Amplified | 127 | 31 | 24.409 | 14 ms | 21 ms | 25 ms | 81 ms | light | 30 | 0 | metadata ok | true | 3606 |
| Breathing | Breathing | 0.1-0.6 Hz | Difference | 130 | 10 | 7.692 | 12 ms | 16 ms | 19 ms | 29 ms | light | 30 | 0 | metadata ok | true | 5328 |
| Object vib | Object | 3-12 Hz | Split | 129 | 4 | 3.101 | 11 ms | 15 ms | 15 ms | 19 ms | light | 30 | 0 | metadata ok | true | 3913 |
| Fast tremor | Fast Motion | 4-12 Hz | Split | 126 | 4 | 3.175 | 10 ms | 15 ms | 16 ms | 26 ms | light | 30 | 0 | metadata ok | true | 3913 |

## Notes

- This is a short preview benchmark. It launches each locked preset, lets the
  app run briefly, and reads `dumpsys gfxinfo` plus Android thermal status.
- The recording probe writes 30 monotonic samples through
  `ProcessedRecordingSession` for each preset and verifies zero dropped-frame
  estimates in the metadata path.
- The encoded probe exports synthetic processed frames for each preset to MP4
  and verifies the output has required MP4 atoms.
- It does not validate visual parity, target quality, camera dropped frames, or
  live analysis latency for each preset.
- Pulse color showed the highest jank in this unattended run.
- Thermal status reached `light` during the run.
