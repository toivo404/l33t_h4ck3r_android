# L33t_h4ck3r_Android

L33t_h4ck3r_Android is a small native Android web project editor. It is built for editing lightweight HTML, CSS, JavaScript, JSON, Markdown, XML, CSV, and plain text files directly on-device.

The app uses Android Storage Access Framework, so projects are opened through the system folder picker instead of broad storage permissions.

## Features

- Pick and remember a project folder with persisted SAF access.
- Browse nested files and folders in a simple project tree.
- Open supported text files in a monospace editor.
- Track unsaved changes and save back to the same document URI.
- Prompt before switching files with unsaved edits.
- Create new files in the project root.
- Preview the current HTML file in an in-app WebView.
- Handle unsupported binary-looking files without crashing.

## Current Scope

This is an MVP nano-style editor, not a full IDE. It intentionally does not include syntax highlighting, autocomplete, Git integration, a terminal, project templates, package managers, or build tools.

HTML preview supports inline HTML, inline CSS, and inline JavaScript. External relative files such as `style.css`, `script.js`, and images are best-effort only for now because SAF resources are not yet resolved through a custom WebView loader.

## Build

From the project root:

```powershell
.\gradlew.bat :app:assembleDebug
```

The debug APK is written to:

```text
app\build\outputs\apk\debug\
```

Run unit tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Install on a connected device or emulator:

```powershell
.\gradlew.bat :app:installDebug
```

On macOS or Linux, use `./gradlew` instead of `.\gradlew.bat`.

## Requirements

- Android Studio or a compatible Android SDK/JDK setup.
- Android device or emulator.
- Android version with `ACTION_OPEN_DOCUMENT_TREE` support.

## Architecture

The app is a small MVVM-style Compose project:

- `MainActivity` hosts Compose UI and the folder picker.
- `ProjectViewModel` owns UI state and editor actions.
- `SafProjectRepository` handles `DocumentFile` tree access and file IO.
- `AppPreferences` stores the last project and selected file URIs.
- `FileTextDetector` handles basic editable-file detection and MIME inference.

## Notes

Project files stay in the folder chosen by the user. Editor content is not persisted separately; changes are saved only by writing to the selected document URI.
