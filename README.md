# ThumsUp AR App

Production-oriented Android app (Kotlin + ARCore) that launches AR from an App Link QR campaign URL, then tracks a can label using ARCore Augmented Images and overlays a cylinder model using fixed can dimensions.

## Features

- Android App Link deep-link handling (`https://yourdomain.com/campaign?product=thumsup300`)
- ARCore session lifecycle management with camera permission handling
- Augmented Image tracking using `augmented_image.imgdb`
- Runtime fallback: if `.imgdb` cannot be deserialized, builds DB from `assets/can_label.png`
- Cylinder pose anchored at detected label pose, updated each frame
- Overlay with tracking state, position, quaternion, and Euler angles

## Can Dimensions

- Height: `0.1063 m`
- Diameter: `0.066 m`
- Radius: `0.033 m`

## Build

1. Open project in Android Studio (latest stable).
2. Sync Gradle.
3. Build and run on an ARCore-supported Android device.

## Generate Augmented Image Database

Use ARCore tool (`arcoreimg`) to create `augmented_image.imgdb` from the label image.

Example:

```bash
arcoreimg build-db \
  --input_image_list_path=/absolute/path/images.txt \
  --output_db_path=/absolute/path/app/src/main/assets/augmented_image.imgdb
```

`images.txt` format:

```text
thumsup_can_label|/absolute/path/app/src/main/assets/can_label.png|0.20
```

- Third field (`0.20`) is physical width in meters.

## Test Deep Link Via ADB

```bash
adb shell am start -a android.intent.action.VIEW \
-d "https://yourdomain.com/campaign?product=thumsup300"
```

## Notes

- QR is used for activation only (deep link), not tracking.
- Tracking is based on can label image features via ARCore Augmented Images.
