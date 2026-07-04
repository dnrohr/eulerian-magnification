# Recorded Video Validation

Recorded-video validation comes before phone testing. It gives the signal path repeatable inputs before CameraX, device exposure, thermal behavior, and GLES lifecycle issues enter the loop.

## Current Test Fixture

`RecordedVideoAnalyzer` accepts timestamped RGB frames and a fixed normalized ROI. It uses the same analysis settings, bandpass filter, FPS meter, and timestamp tracker as the live analysis path.

The JVM tests generate synthetic 30 fps RGB frame sequences with known green-channel changes:

- pulse-band signal at 1.2 Hz
- slow drift outside the pulse band
- non-monotonic timestamp input
- fixed-ROI average-green extraction

This proves the offline analysis fixture can detect the intended pulse-band energy from recorded-frame-style input. It does not yet decode real MP4 files.

## Public Sample Plan

Use public samples before asking for a phone recording:

- MIT Eulerian Video Magnification examples for qualitative EVM sanity checks: <https://people.csail.mit.edu/mrub/evm/>
- UBFC-rPPG for face pulse clips with pulse-ox ground truth: <https://sites.google.com/view/ybenezeth/ubfcrppg>

Do not commit sample videos to the repository. Keep downloaded videos in a local ignored folder such as `sample-videos/` or pass them through a future command-line decoder.

## Device Testing Gate

Ask for the phone only after recorded-video validation can exercise the relevant path. Phone testing is still required for:

- CameraX preview and `ImageAnalysis` behavior
- SurfaceTexture and GLES rendering
- encoder-surface output
- exposure/white-balance lock behavior
- thermal and sustained-FPS behavior
