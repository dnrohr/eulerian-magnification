# Sample Video Sources

Use public recorded clips before asking for a phone recording. Keep downloaded media in `sample-videos/`, which is ignored by Git.

## Pulse Color

Primary source: UBFC-rPPG

- URL: <https://sites.google.com/view/ybenezeth/ubfcrppg>
- Use for face pulse-color testing with pulse-ox reference data.
- Prefer one short clip first, then expand once the decoder and metrics are stable.
- Expected follow-up: feed decoded RGB frames into `RecordedVideoAnalyzer` with a fixed or detected face ROI.

## EVM Visual Sanity

Primary source: MIT Eulerian Video Magnification examples

- URL: <https://people.csail.mit.edu/mrub/evm/>
- Use for qualitative color and motion magnification checks.
- These are useful for visual behavior, but less useful as quantitative pulse benchmarks unless raw source clips and frame timing are available.

## Local Recording

Ask for a phone recording only after public or synthetic recorded-video validation covers the path under test. A local recording is most useful for:

- Pixel camera compression and exposure behavior
- skin-tone and lighting conditions from the target environment
- comparing app preview against encoded output
- sustained device FPS and thermal behavior
