# Android Pipeline Survey

## Pipeline Options

| Pipeline | Strengths | Limits | Fit |
| --- | --- | --- | --- |
| CameraX `Preview` + `ImageAnalysis` | Fastest path to frames, portable device behavior, easy debug | CPU-accessible YUV frames add copies and processing cost | MVP |
| CameraX `VideoCapture` | Mature raw camera recording path | Does not satisfy processed-output recording by itself | Reference only |
| Camera2 + `ImageReader` | Precise control over stream sizes and formats | Still CPU-oriented for analysis frames | Capability fallback |
| Camera2 or CameraX interop + `SurfaceTexture` | Camera frames arrive as GPU textures for preview, processing, and encoder rendering | More EGL/OpenGL lifecycle complexity | Performance target |

## Current Decision

Use CameraX first for MVP preview and CPU ROI color magnification. Move to Camera2 or CameraX interop with `SurfaceTexture` once the algorithm and controls are proven.

The long-term recording architecture should render the same processed OpenGL texture to the display surface and to a `MediaCodec` input surface, then mux MP4 with `MediaMuxer`.

## Pixel 8a Capability Handling

The app should not assume advertised camera-app modes are exposed to third-party processing streams. It now includes a capability reporter that logs:

- device model and Android version
- camera IDs, lens facing, preview sizes, FPS ranges, high-speed sizes, and stabilization modes
- encoder names and MIME types
- battery percent, power-save state, and thermal status

The first real Pixel 8a run should save this output into `docs/experiments/pixel8a_camera_capabilities.json` and summarize stable combinations in `docs/experiments/pixel8a_camera_notes.md`.

## Sources Checked

- Android CameraX release notes: stable CameraX 1.6.1 is current, with 1.6.0 adding the unified camera stack and support-query APIs.
- Android Compose release notes: Compose stable artifacts are current through July 2026, with the BOM managing compatible versions.
- ML Kit face detection docs: face detection supports live/video use cases and recommends sufficient input resolution for low latency.
- Android GPU Inspector docs for later GPU profiling.
