# Milestone AH - Gated Rate Estimates

Importance: Low for now. Rate estimates are tempting but risky before signal validation is stronger.

Goal: only add heart/breath-rate estimates after evidence quality is strong enough to avoid misleading claims.

## Tasks

- [ ] Define strict prerequisites for showing any rate estimate.
- [ ] Implement estimates only as experimental/non-diagnostic values.
- [ ] Gate display behind stable signal, timing, ROI, lighting, and motion checks.
- [ ] Compare estimates against known synthetic/recorded references.
- [ ] Add tests for estimator behavior and gating.
- [ ] Update README warnings and validation docs.

## Done When

- Rate estimates are hidden unless quality gates pass.
- Documentation clearly states that estimates are experimental and non-diagnostic.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
