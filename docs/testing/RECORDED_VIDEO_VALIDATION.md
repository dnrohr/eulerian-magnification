# Recorded Video Validation

Recorded-video validation comes before phone testing. It gives the signal path repeatable inputs before CameraX, device exposure, thermal behavior, and GLES lifecycle issues enter the loop.

## Current Test Fixture

`RecordedVideoAnalyzer` accepts timestamped RGB frames and a fixed normalized ROI. It uses the same analysis settings, bandpass filter, FPS meter, and timestamp tracker as the live analysis path.

`RecordedVideoAnalysisRunner` consumes a sequence of `RgbFrame` inputs and returns a report with frame count, average FPS, average green, total bandpassed energy, max bandpassed magnitude, and timestamp monotonicity. This is the metric layer that synthetic fixtures, public samples, and future local phone recordings can share.

The JVM tests generate synthetic 30 fps RGB frame sequences with known green-channel changes:

- pulse-band signal at 1.2 Hz
- slow drift outside the pulse band
- non-monotonic timestamp input
- fixed-ROI average-green extraction

This proves the offline analysis fixture can detect the intended pulse-band energy from recorded-frame-style input.

## Local MP4 Decoder

`RecordedVideoFrameDecoder` uses Android `MediaMetadataRetriever` to sample a local video file into timestamped `RgbFrame` instances. `RecordedVideoDecodeOptions` controls target FPS and max frame count so long public samples can be bounded during early runs.

The decoder preserves planned timestamps in nanoseconds before frames are passed to `RecordedVideoAnalysisRunner`. JVM tests cover the timestamp plan; full Android builds verify the media decoder code compiles. Real sample-video decoding still needs an on-device or instrumented Android runtime because JVM unit tests cannot execute Android media APIs.

## Next Sample Step

Download one public clip into `sample-videos/`, decode a short bounded frame sequence, and compare the recorded-video report against expected pulse or EVM behavior. Keep the media file local and uncommitted.

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
