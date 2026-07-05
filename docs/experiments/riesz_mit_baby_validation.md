# Riesz MIT Baby Sample Validation

Date: 2026-07-05

## Source

- MIT CSAIL Eulerian Video Magnification source clip:
  <https://people.csail.mit.edu/mrub/evm/video/baby.mp4>
- Local media path during validation:
  `sample-videos/mit-evm-baby.mp4`
- The MP4 and decoded frame JSON are not committed.

## Method

1. Downloaded the MIT source MP4 into ignored local media storage.
2. Copied the MP4 into ignored `app/src/androidTest/assets/` for the validation
   run.
3. Ran `RecordedVideoFrameExportInstrumentedTest` on Pixel 8a. The test used
   Android `MediaMetadataRetriever` through `RecordedVideoFrameDecoder`,
   decoded 60 frames at 10 fps, and exported 24x16 luminance frames.
4. Pulled the exported JSON with `adb exec-out run-as`.
5. Ran `tools/riesz_reference/validate_decoded_sample.py` over adjacent decoded
   frame pairs.

## Result

`python tools\riesz_reference\validate_decoded_sample.py sample-videos\exports\mit-evm-baby-riesz-frames.json`

```text
PASS mit-evm-baby.mp4: 60 frames, 59 pairs, linear_delta=0.001602, phase_delta=0.000472, ratio=0.294765, linear_roughness=0.071753, phase_roughness=0.071722
```

The decoded public sample produced finite, nonzero linear and phase response
metrics through the Riesz reference path. This is a sample-video smoke
validation of the offline/reference phase path, not a claim that the live GPU
Riesz renderer is complete.

## Commands

```powershell
Invoke-WebRequest -Uri 'https://people.csail.mit.edu/mrub/evm/video/baby.mp4' -OutFile 'sample-videos\mit-evm-baby.mp4'
Copy-Item sample-videos\mit-evm-baby.mp4 app\src\androidTest\assets\mit-evm-baby.mp4 -Force
.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.sampleAssetName=mit-evm-baby.mp4' '-Pandroid.testInstrumentationRunnerArguments.class=com.dnrohr.eulerianmagnification.analysis.RecordedVideoFrameExportInstrumentedTest'
.\gradlew.bat assembleDebug assembleDebugAndroidTest
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w -e sampleAssetName mit-evm-baby.mp4 -e class com.dnrohr.eulerianmagnification.analysis.RecordedVideoFrameExportInstrumentedTest com.dnrohr.eulerianmagnification.test/androidx.test.runner.AndroidJUnitRunner
adb exec-out run-as com.dnrohr.eulerianmagnification cat files/validation/mit-evm-baby-riesz-frames.json > sample-videos\exports\mit-evm-baby-riesz-frames.json
python tools\riesz_reference\validate_decoded_sample.py sample-videos\exports\mit-evm-baby-riesz-frames.json
```
