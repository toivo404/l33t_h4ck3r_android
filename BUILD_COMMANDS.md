# Build Commands

Basic console commands for building and checking this Android project.

## From the Project Root

Open a terminal in:
`

## Build Debug APK

```powershell
.\gradlew.bat :app:assembleDebug
```

The debug APK is created under:

```text
app\build\outputs\apk\debug\
```

## Run Unit Tests

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Clean Build Outputs

```powershell
.\gradlew.bat clean
```

## Rebuild From Clean State

```powershell
.\gradlew.bat clean :app:assembleDebug
```

## Install Debug APK On Connected Device

Make sure a device or emulator is connected, then run:

```powershell
.\gradlew.bat :app:installDebug
```

## Check Connected Devices

If Android platform tools are available on your `PATH`:

```powershell
adb devices
```

## Useful Notes

- Use `gradlew.bat` on Windows.
- Use `./gradlew` on macOS or Linux.
- The app uses Android Storage Access Framework, so it does not need broad storage permissions.
- If Gradle fails because no JDK is found, install or configure the JDK used by Android Studio.
