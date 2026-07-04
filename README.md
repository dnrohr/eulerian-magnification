# Eulerian Magnification

Native Android prototype for real-time Eulerian video and motion magnification on Pixel 8a-class hardware.

## Current Scope

- Kotlin Android app with Jetpack Compose UI.
- CameraX preview shell and camera permission flow.
- Capability reporting utilities for cameras, encoders, battery, and thermal state.
- Research notes and task backlog split from the roadmap.

The app is a visualization prototype. It must not make medical-grade heart-rate or diagnostic claims without separate validation.

## Build

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Open the repo in Android Studio, connect a Pixel 8a, and run the `app` configuration.
