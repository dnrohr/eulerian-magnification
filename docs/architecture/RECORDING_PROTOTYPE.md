# Recording Prototype

## Current Slice

The app now has recording controls in the live overlay. Start creates a `ProcessedRecordingSession`; stop writes a metadata sidecar JSON into the app-specific files directory under `recordings/processed-*/metadata.json`.

The metadata captures:

- selected mode and frequency band
- view mode
- amplification
- duration
- sample count
- dropped-frame estimate from non-monotonic timestamps
- per-sample analysis FPS, latency, ROI, average green, and bandpassed green values

The latest metadata JSON can be shared through an Android `FileProvider` using the overlay's share button after a recording stops.

This does not encode an MP4 yet. It is the first recording contract for the processed-output pipeline and gives the encoder path a concrete metadata format to preserve.

An `EncodedOutputValidator` now exists as the first local gate for future MP4 work. It currently verifies basic file presence, non-empty output, and `.mp4` naming. Once `MediaCodec` output exists, this should grow into container and track validation.

## Next Work

- Add a processed frame source suitable for video encoding.
- Encode processed frames with `MediaCodec`.
- Save MP4 and metadata together.
- Add gallery/share/export flow.
