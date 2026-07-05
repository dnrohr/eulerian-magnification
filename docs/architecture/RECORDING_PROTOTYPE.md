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

The overlay also lists recent app-specific recording sessions by scanning
`recordings/processed-*/metadata.json`. Each gallery row shows the mode, view,
sample count, and duration, and can share that recording's metadata JSON.

The app now also writes a debug processed MP4 for each recording session. `DebugProcessedMp4Recorder` uses `MediaCodec` H.264 surface input plus `MediaMuxer` and draws the processed visualization state into the encoder surface: ROI tint, signal, mode, band, amplification, FPS, and latency.

Recording metadata stores both the original frame timestamp and a monotonic presentation timestamp for each processed sample. The presentation timeline starts at zero and advances by at least a 30 FPS frame interval when camera timestamps repeat or move backward. This gives recorded captures a deterministic processed-frame timeline for validation and for the future GL encoder-surface path.

This is not the final camera-preview MP4 yet. It proves the app-owned MP4 encoder/muxer path and records processed state, while final preview-matching recording still needs the Camera/GPU texture path.

An `EncodedOutputValidator` now exists as the first local gate for future MP4 work. It verifies file presence, non-empty output, `.mp4` naming, and top-level `ftyp`, `moov`, and `mdat` atoms. Once `MediaCodec` output exists, this should grow into track-level validation.

## Next Work

- Add a processed frame source suitable for video encoding.
- Feed the actual camera/processed preview texture to the encoder surface.
- Set explicit presentation timestamps from camera or render timing.
- Save final preview-matching MP4 and metadata together.
