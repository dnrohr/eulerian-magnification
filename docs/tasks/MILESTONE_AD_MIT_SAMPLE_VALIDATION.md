# Milestone AD - MIT Sample Validation

Importance: Very high. We need known samples to prove the renderer behaves like EVM.

Goal: validate the recorded full-frame EVM renderer against MIT-style sample videos and artifacts.

## Tasks

- [ ] Run the recorded full-frame EVM renderer on the chosen MIT/local parity samples.
- [ ] Save non-sensitive output artifacts outside git or document their local paths.
- [ ] Compare output against expected MIT-style behavior for pulse/color, breathing/slow motion, and small edge motion.
- [ ] Tune bands, amplification, attenuation, and pyramid levels based on validation results.
- [ ] Add automated or semi-automated parity checks where practical.
- [ ] Document results, failures, and remaining gaps in `docs/experiments/`.

## Done When

- At least one color sample and one motion sample produce plausible MIT-style output.
- Remaining gaps are documented with concrete next steps.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
