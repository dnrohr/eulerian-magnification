# Milestone Y - Export Confidence Plots

Importance: High. Plots make exported evidence faster to inspect than CSV alone.

Goal: generate a quick visual confidence/signal plot for processed video exports.

## Tasks

- [ ] Define the plotted signals: bandpassed signal, quality state, FPS/timing, and optional motion score.
- [ ] Generate a PNG or simple HTML report beside `metadata.json` and `signal_timeline.csv`.
- [ ] Link the plot path from metadata.
- [ ] Add share support if the platform flow allows it cleanly.
- [ ] Add tests for report data serialization or plot metadata.
- [ ] Update recorded-video validation docs.

## Done When

- A processed sample video produces a visual report that is easy to inspect.
- The report is discoverable from metadata or recent exports.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
