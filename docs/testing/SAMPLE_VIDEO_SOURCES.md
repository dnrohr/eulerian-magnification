# Sample Video Sources

Use public recorded clips before asking for a phone recording. Keep downloaded media in `sample-videos/`, which is ignored by Git.

Decision: do not bundle sample videos in the app or repository for now. The
checked-in source of truth is `SampleVideoCatalog`, which records local paths,
source URLs when available, SHA-256 hashes, recommended app modes, and
redistribution notes.

## Known Samples

| ID | Local path | Source | SHA-256 | Recommended use |
| --- | --- | --- | --- | --- |
| `mit-baby` | `sample-videos/mit-evm-baby.mp4` | <https://people.csail.mit.edu/mrub/evm/video/baby.mp4> | `2C5E744384AB88FCCD3AA4883959B33EB4CDB7384C3E46E788CEDE821B2478EE` | `Breathing` mode, `Split` view, slow visible motion sanity check |
| `local-euler` | `sample-videos/euler.mp4` | Local user-provided file | `BF549FEAA994104817A6AFCC39037FB80A013D4074E0AC00EC167F4471B0ACBF` | `Pulse` mode, `Split` view, qualitative regression sample |

Retrieve the MIT sample locally:

```powershell
New-Item -ItemType Directory -Force sample-videos
Invoke-WebRequest -Uri 'https://people.csail.mit.edu/mrub/evm/video/baby.mp4' -OutFile 'sample-videos\mit-evm-baby.mp4'
Get-FileHash sample-videos\mit-evm-baby.mp4 -Algorithm SHA256
```

For phone validation, copy the local sample into Downloads, Photos, or another
location visible to the Android system picker, then use `Process Video` and the
same evidence export path as user-recorded videos.

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
