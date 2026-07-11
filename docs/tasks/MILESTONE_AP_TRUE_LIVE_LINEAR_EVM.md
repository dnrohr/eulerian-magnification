# Milestone AP - True Live Linear EVM

Status: Planned

Importance: Critical. The live app should visibly magnify reconstructed motion/color across the frame, not merely change the boxed ROI color.

Goal: promote the live GL path to a truthful MIT-style linear EVM renderer with spatial pyramid decomposition, temporal filtering, amplification, and reconstruction.

## Tasks

- [ ] Verify the existing live reconstruction path against a controlled target and identify whether it is using enough spatial structure to produce visible motion magnification.
- [ ] Add explicit Gaussian/Laplacian pyramid levels for live color and slow-motion presets.
- [ ] Apply temporal bandpass state per pyramid level instead of only applying an ROI-derived scalar signal.
- [ ] Reconstruct a full processed frame for Raw, Amplified, Difference, and Split views.
- [ ] Add per-level attenuation and gain clamps to reduce halos, clipping, and full-frame flashing.
- [ ] Add renderer diagnostics that report active pyramid levels, internal resolution, temporal warmup state, fallback reason, and display FPS.
- [ ] Validate on Pixel 8a with a known motion/color target and store screenshots or exported evidence notes.

## Done When

- Live amplified preview shows visible reconstructed output for at least one color sample and one slow-motion sample.
- The app can explain when it is showing true reconstructed EVM versus a fallback bridge or ROI signal visualization.
- Relevant tests/device checks pass, documentation is updated, and the task is committed and pushed to `main`.
