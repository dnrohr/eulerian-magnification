# ROI Device Validation

Use this procedure to verify that analysis ROI coordinates match the visible preview on the Pixel 8a.

## Device

- Device: Pixel 8a-class Android phone
- App package: `com.dnrohr.eulerianmagnification`
- Orientation: portrait
- Preview path: CameraX first; repeat with GL preview when the GL path is under test

## Manual ROI Procedure

1. Install the latest debug APK.
2. Launch the app and keep the phone in portrait orientation.
3. Place a non-sensitive known target in frame, such as a high-contrast paper rectangle or wall fixture.
4. Drag a manual ROI tightly around the visible target.
5. Confirm the compact label says `Manual ROI`.
6. Confirm the yellow ROI outline and tint region overlap the same target with no horizontal flip, vertical offset, or stretch.
7. Repeat in `Raw`, `Amp`, `Diff`, and `Split` where supported.

## Automatic Face ROI Procedure

1. Install the latest debug APK.
2. Launch the app in portrait orientation with the front camera facing a visible face.
3. Use `Pulse` mode.
4. Wait for the compact ROI label to change from `Center ROI` or `Frozen ROI` to `Tracking`.
5. Confirm the automatic ROI outline lands on the visible face/skin region rather than the room, shoulder, or center fallback.
6. Move slightly and confirm short detection misses show `Frozen ROI` without visible wandering.
7. If the ROI remains far from the face while `Tracking`, capture non-sensitive evidence and inspect `PreviewRoiMapper`.

## Current Unattended Probe

Date: 2026-07-05

- Connected device: `47091JEKB05516`
- Latest debug APK installed successfully.
- The unattended front-camera screenshot did not contain a visible face target.
- The app showed `Center ROI`, which is expected when automatic face detection has no face to track.
- This probe does not complete automatic face ROI validation.

## Pass Criteria

- Manual ROI: selected preview rectangle maps back to the same analysis region and displays over the same visible target.
- Automatic ROI: face detector output maps to the same visible face/skin region in portrait/front-camera preview.
- No duplicate ROI boxes are visible during normal manual ROI use.
- CameraX and GL preview paths document any intentional differences.
