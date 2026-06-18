# L33t_h4ck3r - Android Nano Web IDE Spec

## 1. Overview

Build a simple native Android app for editing small HTML, CSS, JavaScript, JSON, Markdown, and plain text projects directly on-device.

The app is a minimal “nano-style” mobile IDE for static web projects. It lets the user choose a project folder, browse files in that folder, edit text files, save changes, create new files, and preview HTML files.

But allow user to create / edit other file types. Who knows maybe in future it might be extended to some other purposes.

This is not intended to be a full IDE. The first version should be small, reliable, and easy to extend.

## 2. Goals

The app should support this basic workflow:

1. User opens the app.
2. User chooses a project folder using Android’s folder picker.
3. App displays the folder contents in a file tree.
4. User selects a text file.
5. File content opens in the editor.
6. User edits and saves the file.
7. User can create new files in the selected folder.
8. User can preview the currently selected `.html` file in an in-app WebView.
9. App remembers the last selected folder and can reopen it after restart if permission is still valid.

## 3. Non-Goals for MVP

Do not implement these in the first version:

* Syntax highlighting
* Autocomplete
* LSP support
* Git integration
* Terminal
* Search across files
* Multiple editor tabs
* Code formatting
* Cloud sync
* Accounts or login
* Project templates
* Custom themes
* Advanced file icons
* Build tools
* Package managers
* Live reload

## 4. Tech Stack

Use native Android.

Preferred stack:

* Kotlin
* Jetpack Compose
* Android Studio
* Android Storage Access Framework
* `DocumentFile` for SAF file tree access
* WebView for HTML preview
* No backend
* No account system
* No database unless later requirements justify one

Minimum Android target decision:

* The app should support Android versions where `ACTION_OPEN_DOCUMENT_TREE` is available.
* Use SAF URIs instead of direct filesystem paths.
* Do not request broad storage permissions for MVP.

## 5. Core UI

The app has three main areas:

1. Top bar
2. File explorer sidebar
3. Main editor

On narrow screens, the sidebar may be collapsible or shown as a drawer. On larger screens, it can be permanently visible.

## 6. Top Bar

The top bar contains:

* App title
* Current project folder name
* Current file name, if a file is open
* Dirty state indicator, if the current file has unsaved changes
* Save button
* New File button
* Run button
* Optional overflow menu for later actions

### Save Button

Enabled when:

* A file is selected
* The editor has unsaved changes
* The file is writable

Behavior:

1. Write the editor text back to the selected file URI.
2. Clear dirty state on success.
3. Show an error message on failure.

### New File Button

Opens a dialog for creating a new file.

Default filename:

```text
index.html
```

The user can change the name and extension manually.

Examples:

```text
style.css
script.js
notes.txt
data.json
README.md
```

If a file with the same name already exists in the target folder, show an error and do not overwrite it.

### Run Button

Behavior:

1. If the current file is dirty, save it first.
2. If the selected file has `.html` extension, open it in preview.
3. If the selected file is not `.html`, show:

```text
Run is only supported for HTML files right now.
```

For MVP, only the currently selected `.html` file is runnable.

## 7. File Explorer Sidebar

The sidebar is a VS Code-style project tree.

Requirements:

* Shows files and folders inside the selected project folder.
* Supports nested folders.
* Supports expand and collapse for folders.
* Shows file names and extensions.
* Highlights the currently open file.
* Allows selecting files.
* Does not allow selecting unsupported binary files into the text editor.
* Shows an empty-folder state when there are no files.

Supported text file extensions for MVP:

* `.html`
* `.css`
* `.js`
* `.json`
* `.txt`
* `.md`
* `.xml`
* `.csv`

The app should not hardcode HTML as the only editable file type. HTML is only the default runnable type.

### File Tree Ordering

Use a simple deterministic order:

1. Folders first
2. Files second
3. Alphabetical by name, case-insensitive

### Folder Expansion State

For MVP:

* Expansion state can be kept in memory.

Optional later:

* Persist expanded and collapsed folder state.

## 8. Main Editor

The editor is a simple plain text editor.

Requirements:

* Load selected file content.
* Edit text.
* Save text back to the same SAF URI.
* Use monospace font.
* Support line wrapping.
* Track dirty state.
* Show selected filename.
* Handle empty files.
* Handle large-ish files gracefully, but the app is only intended for small files.

No syntax highlighting is required for MVP.

### Dirty State

Dirty state means the editor content differs from the last loaded or saved file content.

The state should become dirty when:

* A file is loaded.
* User changes editor text.
* New editor text differs from the saved snapshot.

The state should become clean when:

* File is saved successfully.
* User discards changes.
* A new file is created and saved successfully.

### Switching Files with Unsaved Changes

If the user selects another file while the current file has unsaved changes, show a dialog:

Title:

```text
Unsaved changes
```

Message:

```text
Save changes before opening another file?
```

Actions:

* Save
* Discard
* Cancel

Behavior:

* Save: save current file, then open selected file if save succeeds.
* Discard: discard current editor changes, then open selected file.
* Cancel: keep current file open and do nothing.

## 9. Folder Handling

The user chooses a project folder through Android’s system folder picker.

Requirements:

* Use `ACTION_OPEN_DOCUMENT_TREE`.
* Request read and write access.
* Persist URI permission after the user selects a folder.
* Store the selected folder URI in preferences.
* On app restart, attempt to reopen the last selected folder.
* If permission is missing or invalid, clear stored folder URI and ask the user to choose a folder again.
* Use SAF APIs and `content://` URIs.
* Do not assume normal filesystem paths.
* Do not store project files only inside app-private storage.

Default state when no folder is selected:

```text
Choose project folder
```

### Folder Picker Restrictions

Some Android versions and document providers restrict which folders can be selected. The app should handle picker failure or missing permission gracefully.

If the selected folder cannot be opened, show:

```text
Could not open this folder. Please choose another folder.
```

## 10. File Actions

MVP file actions:

* Open file
* Save file
* Create new file

Optional later file actions:

* Rename file
* Delete file
* Create folder
* Duplicate file
* Move file

### Create New File Flow

1. User taps New File.
2. App shows filename dialog.
3. Default filename is `index.html`.
4. User confirms.
5. App validates filename.
6. App checks whether a file with that name already exists in the selected folder.
7. If it does not exist, app creates the file.
8. App opens the new file in the editor.
9. New file starts with empty content, unless later templates are added.

### Filename Validation

Reject filenames that are:

* Empty
* Only whitespace
* Contain `/`
* Contain `\`
* Contain null characters
* Duplicate an existing file in the target folder

Keep validation simple for MVP. Let the document provider handle provider-specific restrictions.

## 11. Run and Preview

The Run feature previews only HTML files in MVP.

### MVP Preview Behavior

When the selected file is `.html`:

1. Save current file if dirty.
2. Read the selected HTML content.
3. Open `PreviewScreen`.
4. Render the HTML in a WebView.

### Preview Screen

The preview screen contains:

* Top bar with Back button
* Current file name
* Optional Refresh button
* WebView filling the remaining screen

### WebView Loading Strategy

MVP strategy:

* Use `loadDataWithBaseURL`.
* Pass the selected HTML content as the HTML data.
* Use a best-effort base URL.
* Enable JavaScript for previewing local JS examples.
* Do not expose Android JS bridges in MVP.

Known MVP limitation:

* Relative files such as `style.css`, `script.js`, images, and nested paths may not load correctly from SAF URIs yet.

Acceptable MVP behavior:

* Inline HTML works.
* Inline `<style>` works.
* Inline `<script>` works.
* External relative files are best-effort only.
* Document this limitation in code comments and issue tracker.

### Later Preview Improvement

A later version may add a custom WebView resource resolver:

* Intercept WebView resource requests.
* Map relative URLs to SAF documents under the selected project folder.
* Serve files with correct MIME types.
* Block access outside the selected project tree.

## 12. Text and Binary File Handling

The editor should only open text-like files.

A file is considered text-like if:

* Its extension is in the supported text extension list, or
* Its MIME type starts with `text/`, or
* Its content passes a simple binary detection check.

For MVP, use extension-based detection first. Add content sniffing if needed.

If the user selects a binary-looking file, do not open it. Show:

```text
This file does not look like a text file.
```

Examples of files that should not open in the editor:

* Images
* Videos
* Audio files
* Archives
* APKs
* PDFs
* Unknown large binary files

## 13. Persistence

Persist using simple key-value storage.

Persist:

* Last selected project folder URI
* Last selected file URI, if still valid
* Optional: last opened file name
* Optional: sidebar expanded folder URIs

Do not persist editor content separately. Editor content is saved only through actual file writes.

On app startup:

1. Load stored project folder URI.
2. Check whether the app still has persisted permission.
3. If valid, rebuild file tree.
4. Try to reopen the last selected file.
5. If the last file no longer exists, clear selected file state and show the project tree only.

## 14. Error Handling

Handle these cases:

### Folder permission missing

Show:

```text
Folder permission is missing. Please choose the project folder again.
```

### File cannot be read

Show:

```text
Could not read this file.
```

### File cannot be written

Show:

```text
Could not save this file.
```

### File was deleted outside the app

Show:

```text
This file no longer exists.
```

Then clear selected file state and refresh the tree.

### Unsupported binary file

Show:

```text
This file does not look like a text file.
```

### Empty folder

Show:

```text
This folder is empty.
```

### Failed preview

Show:

```text
Could not preview this HTML file.
```

### Permission revoked after restart

Clear stored folder URI and return to folder picker state.

## 15. Suggested Architecture

Use a small MVVM-style structure.

Main classes:

* `MainActivity`
* `ProjectViewModel`
* `ProjectRepository`
* `SafFileNode`
* `FileTextDetector`
* `PreviewScreen`
* `AppPreferences`

### Responsibilities

#### MainActivity

* Hosts Compose UI.
* Registers folder picker launcher.
* Passes selected folder URI to ViewModel.
* Handles app-level setup.

#### ProjectViewModel

* Owns UI state.
* Handles user intents.
* Tracks selected file.
* Tracks editor text.
* Tracks dirty state.
* Coordinates repository calls.
* Emits error messages.

#### ProjectRepository

* Reads SAF folder tree.
* Reads text files.
* Writes text files.
* Creates new files.
* Checks file existence.
* Resolves display names.
* Checks basic file metadata.

#### AppPreferences

* Stores last folder URI.
* Stores last selected file URI.
* Stores optional UI state.

## 16. Data Model

```kotlin
data class SafFileNode(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val children: List<SafFileNode> = emptyList()
)
```

```kotlin
data class IdeState(
    val projectFolderUri: Uri? = null,
    val projectFolderName: String? = null,
    val files: List<SafFileNode> = emptyList(),

    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    val selectedFileExtension: String? = null,

    val editorText: String = "",
    val savedEditorText: String = "",
    val isDirty: Boolean = false,

    val isLoadingTree: Boolean = false,
    val isSaving: Boolean = false,
    val isPreviewOpen: Boolean = false,

    val pendingFileToOpen: SafFileNode? = null,
    val showUnsavedChangesDialog: Boolean = false,
    val showNewFileDialog: Boolean = false,

    val errorMessage: String? = null
)
```

## 17. Main Composables

Use these composables:

* `IdeScreen`
* `IdeTopBar`
* `ChooseFolderEmptyState`
* `FileExplorer`
* `FileTreeItem`
* `EditorPane`
* `PreviewScreen`
* `NewFileDialog`
* `UnsavedChangesDialog`
* `ErrorSnackbar`

Suggested layout:

```text
IdeScreen
├── IdeTopBar
├── Content
│   ├── FileExplorer
│   └── EditorPane
├── NewFileDialog
├── UnsavedChangesDialog
└── ErrorSnackbar
```

## 18. ViewModel Events

Suggested event methods:

```kotlin
fun onProjectFolderPicked(uri: Uri)

fun onChooseFolderClicked()

fun onFileClicked(node: SafFileNode)

fun onEditorTextChanged(text: String)

fun onSaveClicked()

fun onRunClicked()

fun onCreateNewFileClicked()

fun onNewFileConfirmed(fileName: String)

fun onUnsavedChangesSaveConfirmed()

fun onUnsavedChangesDiscardConfirmed()

fun onUnsavedChangesCancelled()

fun onPreviewClosed()

fun onErrorShown()
```

## 19. Repository Interface

Suggested repository API:

```kotlin
interface ProjectRepository {
    suspend fun loadTree(folderUri: Uri): List<SafFileNode>

    suspend fun readTextFile(fileUri: Uri): String

    suspend fun writeTextFile(fileUri: Uri, text: String)

    suspend fun createFile(
        parentFolderUri: Uri,
        fileName: String,
        mimeType: String
    ): Uri

    suspend fun fileExists(
        parentFolderUri: Uri,
        fileName: String
    ): Boolean

    suspend fun getDisplayName(uri: Uri): String?

    suspend fun canWrite(uri: Uri): Boolean
}
```

## 20. MIME Types

Use simple MIME inference for created files.

Suggested mapping:

```text
.html -> text/html
.css  -> text/css
.js   -> application/javascript
.json -> application/json
.md   -> text/markdown
.txt  -> text/plain
.csv  -> text/csv
.xml  -> application/xml
other -> text/plain
```

For MVP, unknown extensions created through New File can use `text/plain`.

## 21. MVP Scope

Implement only:

* Pick project folder
* Persist folder URI permission
* Remember last folder
* Show file tree sidebar
* Expand and collapse folders
* Open supported text files
* Edit text
* Track dirty state
* Save current file
* Create new file in project root
* Preview current `.html` file in WebView
* Basic error messages

Creating files inside nested folders can be added later unless it is easy to support during MVP.

## 22. Acceptance Criteria

The app is done when all of these pass:

1. User can launch the app with no folder selected.
2. App shows a clear `Choose project folder` button.
3. User can choose a folder through Android’s folder picker.
4. App persists access to the selected folder.
5. Files and folders appear in the sidebar.
6. Nested folders can be expanded and collapsed.
7. User can tap `index.html`.
8. File content appears in the editor.
9. User can edit the file.
10. Save writes the edited content back to the same file.
11. Dirty state appears after editing.
12. Dirty state clears after saving.
13. If user switches files with unsaved changes, app asks Save, Discard, or Cancel.
14. User can create `test.html`.
15. User can edit and save `test.html`.
16. User can press Run on `test.html`.
17. HTML opens in an in-app WebView preview.
18. Pressing Run on a non-HTML file shows a clear unsupported message.
19. User can restart the app and reopen the same folder without picking it again, assuming permission is still valid.
20. If permission is gone, app asks the user to choose a folder again.
21. Selecting a binary-looking unsupported file does not crash the app.
22. Empty folders show a useful empty state.

## 23. Implementation Notes

Prefer simple, boring implementation choices for MVP.

Use `DocumentFile.fromTreeUri` to turn the selected folder URI into a traversable document tree.

Use `ContentResolver.openInputStream` and `openOutputStream` for file read and write operations.

Use coroutines for file IO.

Keep file tree loading off the main thread.

Keep the editor simple. A Compose `TextField` or `BasicTextField` is acceptable for MVP, as long as it can edit small files comfortably.

Do not optimize for huge files. If a file is too large for comfortable editing, show a friendly error later.

Recommended soft limit for MVP:

```text
1 MB per opened text file
```

If a file is larger than the limit, show:

```text
This file is too large for the editor.
```

## 24. Future Improvements

Possible later features:

* Relative CSS and JS loading in preview through WebView request interception
* Live preview refresh
* Syntax highlighting
* Multiple tabs
* Search in file
* Search across files
* Rename file
* Delete file
* Create folders
* Create files inside selected folders
* Project templates
* Simple file icons
* Recent projects
* Dark/light editor theme
* Markdown preview
* Basic formatting
* External browser launch option
* Export project as ZIP
