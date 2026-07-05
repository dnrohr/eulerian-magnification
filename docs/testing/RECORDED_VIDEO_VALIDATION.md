# Recorded Video Validation

Recorded-video validation comes before phone testing. It gives the signal path repeatable inputs before CameraX, device exposure, thermal behavior, and GLES lifecycle issues enter the loop.

## Current Test Fixture

`RecordedVideoAnalyzer` accepts timestamped RGB frames and a fixed normalized ROI. It uses the same analysis settings, bandpass filter, FPS meter, and timestamp tracker as the live analysis path.

`RecordedVideoAnalysisRunner` consumes a sequence of `RgbFrame` inputs and returns a report with frame count, average FPS, average green, total bandpassed energy, max bandpassed magnitude, and timestamp monotonicity. This is the metric layer that synthetic fixtures, public samples, and future local phone recordings can share.

`RecordedVideoValidator` ties the decode and report steps together for a local video file. It returns a concise `Video processing` summary string that includes the source name, selected mode/band, frame count, FPS, energy, peak bandpassed magnitude, and timing status.

`RecordedVideoProcessor` is the first export-pipeline building block. It consumes decoded `RgbFrame` inputs and produces processed `RgbFrame` outputs for `Raw`, `Amplified`, `Difference`, and `Split` views. This is app-native CPU color processing, not the earlier Python diagnostic render.

`RecordedVideoMp4Exporter` can encode those processed frames to H.264 MP4 on Android. The exporter is covered by an instrumented Pixel test that writes to app cache and validates the generated MP4 atoms. The picker UI now saves `debug_processed.mp4` and `metadata.json` under `recordings/processed-*`; recent rows expose `Metadata` and `Video` share buttons when available.

The JVM tests generate synthetic 30 fps RGB frame sequences with known green-channel changes:

- pulse-band signal at 1.2 Hz
- slow drift outside the pulse band
- non-monotonic timestamp input
- fixed-ROI average-green extraction

This proves the offline analysis fixture can detect the intended pulse-band energy from recorded-frame-style input.

## Local MP4 Decoder

`RecordedVideoFrameDecoder` uses Android `MediaMetadataRetriever` to sample a local video file into timestamped `RgbFrame` instances. `RecordedVideoDecodeOptions` controls target FPS and max frame count so long public samples can be bounded during early runs.

The decoder preserves planned timestamps in nanoseconds before frames are passed to `RecordedVideoAnalysisRunner`. JVM tests cover the timestamp plan; full Android builds verify the media decoder code compiles. Real sample-video decoding still needs an on-device or instrumented Android runtime because JVM unit tests cannot execute Android media APIs.

## Validation Flow

1. Place a public sample in `sample-videos/`.
2. Build and open the app.
3. Set the intended mode and amplification.
4. Tap `Process Video` in the recording controls.
5. Pick the local sample video from the system picker.
6. Record the displayed summary metrics in notes or a future benchmark artifact.

The app copies the selected video into cache using the selected display name when available, decodes a bounded frame set, runs `RecordedVideoValidator`, and displays the summary in the overlay. Selecting `euler.mp4` should produce a summary beginning with `Video processing: euler.mp4`.

## Running `euler.mp4`

1. Copy `sample-videos/euler.mp4` to the phone's downloads, camera roll, or another location visible to the Android system picker.
2. Open the app and select the mode to test, usually `Pulse` for the current color-amplification path.
3. Tap `Controls`, then `Process Video`.
4. Select `euler.mp4`.
5. Use the displayed frame count, FPS, energy, peak, and timing status as the repeatable baseline for that mode.
6. Use the recent recording/export row's `Video` button to share the processed MP4, or `Metadata` to inspect the JSON.

## Next Sample Step

Download one public clip into `sample-videos/`, run the in-app validation flow, and compare the recorded-video report against expected pulse or EVM behavior. Keep the media file local and uncommitted.

## Public Sample Plan

Use public samples before asking for a phone recording:

- MIT Eulerian Video Magnification examples for qualitative EVM sanity checks: <https://people.csail.mit.edu/mrub/evm/>
- UBFC-rPPG for face pulse clips with pulse-ox ground truth: <https://sites.google.com/view/ybenezeth/ubfcrppg>

Do not commit sample videos to the repository. Keep downloaded videos in the ignored `sample-videos/` folder or pass them through a future command-line decoder. See `docs/testing/SAMPLE_VIDEO_SOURCES.md`.

## Device Testing Gate

Ask for the phone only after recorded-video validation can exercise the relevant path. Phone testing is still required for:

- CameraX preview and `ImageAnalysis` behavior
- SurfaceTexture and GLES rendering
- encoder-surface output
- exposure/white-balance lock behavior
- thermal and sustained-FPS behavior
