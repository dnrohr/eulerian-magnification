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

When recording from GL preview, `GlProcessedMp4Recorder` records the processed
GL texture instead of the canvas debug visualization. It uses the same
`MediaCodec`/`MediaMuxer` surface-input pattern, but feeds frames through
`GlEncoderSurfaceRenderer` so the processed preview texture is rendered directly
to the encoder input surface.

Recording metadata stores both the original frame timestamp and a monotonic presentation timestamp for each processed sample. The presentation timeline starts at zero and advances by at least a 30 FPS frame interval when camera timestamps repeat or move backward. This gives recorded captures a deterministic processed-frame timeline for validation and for the future GL encoder-surface path.

`ProcessedRecordingSession.record(...)` returns the stored `RecordingSample`, so
future GL recording integration can pass the same monotonic
`presentationTimestampNanos` into the processed texture export and encoder
surface.

The GL preview analysis callback now records the sample first, then passes the
returned monotonic `presentationTimestampNanos` into
`ColorMagnificationUniforms`. As a result, each emitted `ProcessedGlFrame`
carries the same presentation timeline written to recording metadata.

The GL renderer now exposes a `ProcessedGlFrame` callback with the processed
texture id, target size, split-mode flag, and presentation timestamp. The next
encoder slice should render that texture into `MediaCodec`'s input surface from
an EGL context and assign the same timestamp with
`eglPresentationTimeANDROID`.

`GlEncoderSurfaceRenderer` is the first encoder-surface rendering component. It
creates a recordable GLES 3 EGL window surface around a `MediaCodec` input
surface, shares the current GL context, blits a processed texture to that
surface, assigns `eglPresentationTimeANDROID`, swaps buffers, and restores the
previous EGL context. It is wired into GL preview recording through
`GlProcessedMp4Recorder` and covered by a Pixel 8a instrumentation test that
validates the resulting MP4 container.

Recording metadata now also includes the active preview path, GL renderer path,
GL timing, live reconstruction summary/fallback, and live phase summary/fallback
when the run stops. This lets an exported MP4 be interpreted as live linear EVM,
phase EVM, fallback color bridge, or CameraX/debug output without relying on a
separate screenshot of the UI.

This is not the final camera-preview MP4 yet. It proves the app-owned MP4 encoder/muxer path and records processed state, while final preview-matching recording still needs the Camera/GPU texture path.

An `EncodedOutputValidator` now exists as the first local gate for future MP4 work. It verifies file presence, non-empty output, `.mp4` naming, and top-level `ftyp`, `moov`, and `mdat` atoms. Once `MediaCodec` output exists, this should grow into track-level validation.

## Next Work

- Add deeper MP4 validation for track duration and sample timestamps.
- Run longer GL preview recording benchmarks on Pixel 8a.
