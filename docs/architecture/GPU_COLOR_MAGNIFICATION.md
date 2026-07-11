# GPU Color Magnification

## Current Slice

Milestone E now has the first shader-level building block for GPU color magnification.

`ColorMagnificationShaderSource` defines a GLES 3.0 pass that:

- samples an RGB texture
- limits amplification to a normalized ROI
- applies a clamped amplified color delta
- supports a difference mode for debug visualization

`ColorMagnificationParameters` maps the existing CPU analysis output and UI settings into shader-style uniforms. It reuses `ArtifactSuppressor`, so CPU overlay, debug MP4, and future GPU output share the same basic noise-floor and amplification-cap behavior.

## Current Limit

The camera GL renderer now renders the OES camera texture into a framebuffer-backed RGBA texture before drawing to the screen. This creates the internal RGB texture needed for later shader passes.

The renderer also allocates a three-level reduced-resolution pyramid using `GlPyramid`. These are framebuffer-backed texture levels sized from half resolution downward. They are allocation scaffolding only; the downsample shader pass is not wired yet.

`GlTemporalState` now allocates ping-pong render targets for each pyramid level. The renderer swaps the temporal state each frame, but the shader pass that updates lowpass/bandpass state is not wired yet.

The color magnification pass is now wired into the camera GL renderer's display path. The renderer runs OES camera frames into an internal RGB texture, applies the ROI color magnification shader to a processed render target, then displays that processed texture.

After the color pass, the renderer also publishes a `ProcessedGlFrame` callback
containing the processed texture id, render-target size, split-mode flag, and
presentation timestamp from the latest analysis uniforms. This is the handoff
point for the future EGL encoder-surface renderer; it does not yet write the
texture into `MediaCodec`'s input surface.

`GlEncoderSurfaceRenderer` can consume that processed texture on a recordable
EGL window surface, using the shared GL context and `eglPresentationTimeANDROID`
for the processed frame timestamp. GL preview recording now forwards emitted
`ProcessedGlFrame` instances to `GlProcessedMp4Recorder`, so the processed
texture is rendered both to display and to the encoder surface.

The `Split` view mode reuses those two render targets for visual comparison: raw RGB is drawn into the left half of the GL surface and the processed texture is drawn into the right half. The split is implemented with deterministic viewport layout logic so odd-width and tiny surfaces remain drawable.

The GL preview overlay also reports a benchmark summary from `PerformanceBenchmark`, comparing CPU analysis FPS/latency with GL render FPS/frame time and flagging whether GL meets the 30 fps display target. A short Pixel 8a run is recorded in `docs/experiments/pixel8a_gpu_benchmark.md`: the GL preview sample hit 11 ms median frame time and 1.14% janky frames, compared with 15 ms median and 32.55% janky frames in the CameraX CPU preview sample. Longer thermal and CPU-load runs are still needed before treating this as a sustained-performance result.

For now, CPU analysis still supplies the ROI and bandpassed signal uniforms.
True GPU temporal-filter updates are still pending.

The live GL path now has a tested reconstruction pass plan and shader-source
contracts for the next stage:

- downsample the RGB texture into a reduced pyramid
- update low/high temporal state per pyramid level
- reconstruct the frame from amplified bandpass levels

`GlTemporalState` now allocates the targets needed for that temporal update:
lowpass ping-pong, highpass ping-pong, and a current bandpass output per
pyramid level. Each level also has a framebuffer binding for the temporal
shader's multiple render outputs.

`CameraOesRenderer` now compiles and invokes those live reconstruction passes
when the existing full-frame preview policy enables GL full-frame output. The
temporal targets use half-float storage so signed bandpass deltas are preserved
for reconstruction. This replaces the visible Pulse color bridge on the
full-frame path in local builds, but it still needs Pixel portrait validation
before the milestone can be treated as complete.

## Verification

- Unit tests verify ROI-limited shader source expectations, difference-mode source expectations, split-mode uniform mapping, viewport layout, benchmark summary mapping, and uniform mapping from analysis/settings.
- Android build verifies the shader/pass Kotlin code compiles into the debug APK.
