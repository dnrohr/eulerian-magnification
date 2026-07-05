# Milestone C: Recording Prototype

Goal: record processed output, not the raw camera feed.

## Tasks

- [x] Add start/stop recording controls.
- [x] Encode composited processed frames with monotonic timestamps.
- [x] Save MP4 to app-specific media storage.
- [x] Save sidecar metadata JSON with FPS, resolution, algorithm settings, ROI, dropped frames, and thermal state.
- [x] Add recording indicator and elapsed time.
- [x] Add export/share affordance.
- [x] Add encoder output validity checks.
- [x] Document recording architecture.
- [x] Commit and push to `main`.

## Completed Slice: Metadata Recording Prototype

- Added start/stop recording controls to the overlay.
- Added elapsed recording indicator.
- Added app-specific metadata JSON output under `recordings/processed-*/metadata.json`.
- Captures mode, band, amplification, thermal state, sample count, dropped-frame estimate, ROI, FPS, latency, average green, and bandpassed signal.
- Added unit tests for metadata writing and dropped-frame estimate.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Monotonic Recording Timeline Foundation

- Added a `MonotonicFrameTimeline` for processed recording sessions.
- Recording metadata now stores both the source `timestampNanos` and monotonic `presentationTimestampNanos`.
- Repeated or decreasing source timestamps advance by a 30 FPS interval, keeping the presentation timeline strictly increasing after the first frame.
- Added unit coverage for increasing, repeated, decreasing, and before-first source timestamps.

The current debug MP4 encoder uses `Surface.lockCanvas`, which does not expose explicit presentation timestamp control. True encoded PTS assignment remains part of the final GL encoder-surface recording path, so the composited-frame checklist item stays open.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.recording.MonotonicFrameTimelineTest" --tests "com.dnrohr.eulerianmagnification.recording.ProcessedRecordingSessionTest"`
- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Supporting Slice: Processed GL Frame Export Hook

- Added a GL callback model that exposes the processed texture and presentation
  timestamp after the color pass.
- Documented the intended next encoder step: draw that texture into
  `MediaCodec`'s input surface from EGL and assign the timestamp with
  `eglPresentationTimeANDROID`.

This is the renderer-side handoff only. The debug MP4 path still uses
`Surface.lockCanvas`, so explicit encoded PTS assignment remains open.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.gl.ColorMagnificationPassTest" --tests "com.dnrohr.eulerianmagnification.gl.ProcessedGlFrameTest"`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

## Supporting Slice: Recording Timeline Return Value

- `ProcessedRecordingSession.record(...)` now returns the stored
  `RecordingSample`.
- Callers can use the returned `presentationTimestampNanos` to feed the future
  GL texture encoder path with the same monotonic timestamps written to
  metadata.
- Added unit coverage for the returned monotonic timestamps on non-monotonic
  source frames and video-recorder forwarding.

This exposes the timeline value needed by the encoder surface integration. The
actual encoded frame PTS assignment remains open.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.recording.ProcessedRecordingSessionTest"`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

## Supporting Slice: GL Recording Timestamp Wiring

- The GL preview analyzer now records the sample before updating renderer color
  uniforms.
- Recording mode uses the returned monotonic `presentationTimestampNanos` in
  `ColorMagnificationUniforms`.
- Emitted `ProcessedGlFrame` instances now carry the same monotonic timeline
  that recording metadata stores.

This aligns the GL frame-export timestamp with recording metadata. Rendering the
processed texture into the encoder input surface remains open.

## Verification

- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

## Supporting Slice: EGL Encoder Surface Renderer Foundation

- Added `GlEncoderSurfaceRenderer` for recordable GLES 3 EGL window surfaces
  backed by a `MediaCodec` input `Surface`.
- The renderer shares the current GL context, blits a processed texture to the
  encoder surface, assigns `eglPresentationTimeANDROID`, swaps buffers, and
  restores the previous EGL context.
- Added unit coverage for the recordable EGL config and GLES 3 context
  attributes.

This creates the encoder-surface renderer component. It is not yet wired into
`ProcessedVideoRecorder`, so the composited-frame checklist item stays open.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.gl.GlEncoderSurfaceRendererTest"`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

## Completed Slice: Live GL Processed MP4 Recorder

- Added `GlProcessedMp4Recorder`, which owns a H.264 `MediaCodec` surface-input
  encoder and `MediaMuxer`.
- GL preview recordings now use `GlProcessedMp4Recorder`; CameraX preview keeps
  the canvas debug recorder.
- `ProcessedRecordingSession` forwards emitted `ProcessedGlFrame` instances to
  the active recorder.
- The GL recorder renders processed textures into the encoder input surface and
  applies each frame's monotonic `presentationTimestampNanos` with
  `eglPresentationTimeANDROID`.
- Added a Pixel 8a instrumentation test that records generated GL textures
  through the real encoder-surface path, stops the muxer, and validates the MP4
  container with `EncodedOutputValidator`.

## Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.dnrohr.eulerianmagnification.recording.ProcessedRecordingSessionTest" --tests "com.dnrohr.eulerianmagnification.gl.GlEncoderSurfaceRendererTest"`
- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug assembleDebugAndroidTest`
- `.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.dnrohr.eulerianmagnification.recording.GlProcessedMp4RecorderInstrumentedTest'`

## Completed Slice: Debug Processed MP4 Recorder

- Added a `ProcessedVideoRecorder` interface and `DebugProcessedMp4Recorder` implementation.
- Uses `MediaCodec` H.264 surface input and `MediaMuxer` to write `debug_processed.mp4` in each app-specific recording session folder.
- Encodes the processed debug visualization: ROI tint, mode, band, amplification, FPS, latency, green value, and bandpassed signal.
- Metadata now includes the debug video path.
- Added unit coverage that recording sessions forward samples to the video recorder and stop it.

This is not the final camera-preview MP4. It proves the app-owned MP4 encoder/muxer path and records processed state, while the final preview-matching recording still needs the Camera/GPU texture path.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: MP4 Structure Validation

- Extended `EncodedOutputValidator` to parse top-level MP4 atoms.
- Validator now requires non-empty `.mp4` output with `ftyp`, `moov`, and `mdat` atoms.
- Added unit tests for valid candidates and missing required atoms.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Completed Slice: Metadata Share And Encoder Gate Scaffold

- Added a `FileProvider` for app-specific recording artifacts.
- Added a share button for the latest recording metadata JSON.
- Added an `EncodedOutputValidator` scaffold for future MP4 output checks.
- Added unit tests for missing, empty, and non-empty `.mp4` output candidates.

## Verification

- `.\gradlew.bat clean testDebugUnitTest assembleDebug`

## Success Criteria

- Recorded MP4 visually matches preview.
- Preview does not stutter substantially when recording starts.
- Audio is explicitly optional for the first version.
