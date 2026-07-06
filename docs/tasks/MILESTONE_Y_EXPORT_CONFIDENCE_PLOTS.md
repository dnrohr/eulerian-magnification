# Milestone Y - Export Confidence Plots

Importance: High. Plots make exported evidence faster to inspect than CSV alone.

Goal: generate a quick visual confidence/signal plot for processed video exports.

## Tasks

- [x] Define the plotted signals: bandpassed signal, quality state, FPS/timing, and optional motion score.
- [x] Generate a PNG or simple HTML report beside `metadata.json` and `signal_timeline.csv`.
- [x] Link the plot path from metadata.
- [x] Add share support if the platform flow allows it cleanly.
- [x] Add tests for report data serialization or plot metadata.
- [x] Update recorded-video validation docs.

## Completed Slice

- Selected-video processing now writes `evidence_report.html` beside `metadata.json`, `signal_timeline.csv`, and `debug_processed.mp4`.
- The report includes source, mode/view/band/amplification, frame count, average FPS, quality summary, and an inline SVG bandpassed-signal plot.
- Metadata now includes `evidenceReportPath`.
- Recent exports show a `Report` share button when a report is available.
- Added JVM coverage for report HTML escaping/contents and gallery metadata parsing.

## Done When

- A processed sample video produces a visual report that is easy to inspect.
- The report is discoverable from metadata or recent exports.
- Relevant tests/build checks pass, and the task is committed and pushed to `main`.
