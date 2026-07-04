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

The color magnification pass is not wired into the camera GL renderer yet. The next GPU color slice should run this pass over the internal RGB texture and display the processed result.

## Verification

- Unit tests verify ROI-limited shader source expectations, difference-mode source expectations, and uniform mapping from analysis/settings.
- Android build verifies the shader/pass Kotlin code compiles into the debug APK.
