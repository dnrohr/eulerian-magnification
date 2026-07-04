# Video Magnification Survey

## Summary

Eulerian Video Magnification (EVM) decomposes frames spatially, filters each pixel or pyramid level over time, amplifies the filtered signal, and reconstructs the result. It is a good first fit for pulse-like color variation because the MVP can restrict processing to a face ROI and a low internal resolution.

Phase-based magnification estimates local phase changes rather than directly amplifying pixel intensity. It handles subtle motion with fewer blur and halo artifacts than linear EVM, but it costs more engineering time and needs a reference implementation before Android optimization.

Riesz pyramids are a compact phase-based representation designed for real-time phase-based video magnification. They are the likely long-term path for higher quality general motion on mobile, after the app already has capture, timing, recording, and GPU infrastructure.

Learning-based approaches can produce impressive examples, but they are not the first implementation target because they add model dependency, device acceleration questions, and harder failure-mode control.

## Cost And Mobile Suitability

| Approach | Cost | Latency | Common Artifacts | Mobile Suitability |
| --- | --- | --- | --- | --- |
| ROI color EVM | Low | Low | exposure flicker, face motion tinting | Best MVP |
| Linear pyramid EVM | Medium | Medium | halos, blur, noise amplification | Good beta path |
| Phase-based | High | Medium-high | phase noise, orientation errors | Later motion mode |
| Riesz pyramid | High | Medium | phase artifacts, GPU complexity | Best long-term quality path |
| Learning-based | Variable | Model-dependent | hallucinated motion, domain mismatch | Defer |

## Implementation Decision

Implement first:

- Face/skin ROI color magnification using CameraX `ImageAnalysis`.
- Temporal filter utilities with unit tests.
- Debug overlays for signal, FPS, latency, and ROI stability.

Defer:

- Full-frame Laplacian pyramid until CPU ROI mode validates timing and UX.
- Riesz/phase mode until an offline reference is validated.
- Learning-based methods until the app has repeatable benchmark assets.

Do not copy:

- RenderScript-era acceleration paths.
- Desktop UI or threading assumptions from OpenCV/Qt demos.
- Any code path that records only the raw camera stream when the product requirement is processed output.

## Sources Checked

- MIT CSAIL Eulerian Video Magnification project.
- MIT CSAIL Video Magnification code page.
- MIT Riesz Pyramids project.
- Live-Video-Magnification desktop reference.
- Android EVM pulse prior art.
