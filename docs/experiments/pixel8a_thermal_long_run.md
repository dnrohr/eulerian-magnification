# Pixel 8a Thermal And Preview Benchmark

Date: 2026-07-05

Device: Pixel 8a (`akita`, serial `47091JEKB05516`)

Build: debug APK from `main` after launcher-icon and permission-copy polish.

## Setup

- Installed `app/build/outputs/apk/debug/app-debug.apk` with `adb install -r`.
- Granted camera permission through ADB.
- Force-stopped and relaunched `com.dnrohr.eulerianmagnification/.MainActivity`.
- Cleared logcat and reset `dumpsys gfxinfo` before the timed samples.
- Mode: Pulse, Amplified view, default 12x amplification.
- Environment: phone connected over USB and charging during the run.

## CameraX Preview Sample

Duration: 3 minutes

End-of-run overlay:

- Analysis: about `23.2 fps / 0 ms`
- Quality: `Low FPS, Signal weak`
- App process stayed alive.

`dumpsys gfxinfo com.dnrohr.eulerianmagnification framestats`:

- Total frames rendered: `4801`
- Janky frames: `245` (`5.10%`)
- 50th percentile: `10 ms`
- 90th percentile: `14 ms`
- 95th percentile: `16 ms`
- 99th percentile: `20 ms`
- GPU 50th percentile: `2 ms`
- GPU 99th percentile: `6 ms`

Battery temperature from `dumpsys battery` moved from `34.4 C` at launch to
`35.4 C` after the 3-minute CameraX sample. The app stayed responsive, but the
analysis overlay dipped below the 30 fps target.

## GL Preview Sample

Duration: 2 minutes after switching from CameraX preview to GL preview.

End-of-run overlay:

- Analysis: about `24.0 fps / 0 ms`
- GL readout: about `720.6 fps / 1.39 ms`
- Benchmark: GL OK, CPU analysis still below 30 fps.
- App process stayed alive.
- Crash buffer contained no matching `FATAL EXCEPTION` or `GlException` entry.

`dumpsys gfxinfo com.dnrohr.eulerianmagnification framestats`:

- Total frames rendered: `3567`
- Janky frames: `75` (`2.10%`)
- 50th percentile: `11 ms`
- 90th percentile: `14 ms`
- 95th percentile: `15 ms`
- 99th percentile: `17 ms`
- GPU 50th percentile: `2 ms`
- GPU 99th percentile: `3 ms`

Battery temperature from `dumpsys battery` reached `36.5 C` by the end of the GL
sample. A later full `dumpsys thermalservice` read reported `Thermal Status: 1`,
with current HAL temperatures around:

- Battery: `37.1 C`
- Virtual skin: `40.2 C`
- G3D: `54.0 C`
- CPU clusters: `57-58 C`
- SOC therm: `40.4 C`

Some HAL sensor statuses were at `1` or higher after the combined sample, while
CPU/GPU/battery sensors remained status `0`. Treat this as a bounded preview
smoke run, not a final sustained-performance qualification.

## Notes

- GL preview remained upright and alive after the earlier shader-version and
  aspect-fill fixes.
- The UI still has button label wrapping on the Pixel 8a portrait viewport
  (`Breathing`, `Tremor`, `Amplified`, and `Difference` wrap awkwardly). That is
  tracked by the remaining Milestone J controls-polish task.
- Longer 10-30 minute thermal tests should avoid USB charging or record the
  charging state explicitly because charging changes thermal behavior.
