package com.example.l33t_h4ck3r_android

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileNotFoundException

class ProjectViewModel(
    private val repository: ProjectRepository,
    private val preferences: AppPreferences,
    private val contentResolver: ContentResolver
) : ViewModel() {
    private val _state = MutableStateFlow(IdeState())
    val state: StateFlow<IdeState> = _state

    init {
        restoreLastProject()
    }

    fun onProjectFolderPicked(uri: Uri) {
        val hasPermission = contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
        if (!hasPermission) {
            showError("Could not open this folder. Please choose another folder.")
            return
        }
        preferences.setLastFolderUri(uri)
        preferences.setLastFileUri(null)
        loadProject(uri, reopenLastFile = false)
    }

    fun onFileClicked(node: SafFileNode) {
        if (node.isDirectory) return
        if (_state.value.isDirty) {
            _state.update { it.copy(pendingFileToOpen = node, showUnsavedChangesDialog = true) }
        } else {
            openFile(node)
        }
    }

    fun onEditorTextChanged(text: String) {
        _state.update { it.copy(editorText = text, isDirty = text != it.savedEditorText) }
    }

    fun onSaveClicked() {
        saveCurrentFile()
    }

    fun onRunClicked() {
        viewModelScope.launch {
            val current = _state.value
            val uri = current.selectedFileUri
            if (uri == null) {
                showError("Run is only supported for HTML files right now.")
                return@launch
            }
            if (current.selectedFileExtension != "html" && current.selectedFileExtension != "htm") {
                showError("Run is only supported for HTML files right now.")
                return@launch
            }
            if (current.isDirty && !saveCurrentFileSuspend()) return@launch
            try {
                val html = repository.readTextFile(uri)
                _state.update { it.copy(isPreviewOpen = true, previewHtml = html, errorMessage = null) }
            } catch (_: Exception) {
                showError("Could not preview this HTML file.")
            }
        }
    }

    fun onCreateNewFileClicked() {
        _state.update { it.copy(showNewFileDialog = true) }
    }

    fun onNewFileDismissed() {
        _state.update { it.copy(showNewFileDialog = false) }
    }

    fun onNewFileConfirmed(fileName: String) {
        val trimmed = fileName.trim()
        if (trimmed.isEmpty() || trimmed.contains('/') || trimmed.contains('\\') || trimmed.contains('\u0000')) {
            showError("Invalid filename.")
            return
        }
        val folderUri = _state.value.projectFolderUri ?: return
        viewModelScope.launch {
            try {
                if (repository.fileExists(folderUri, trimmed)) {
                    showError("A file with this name already exists.")
                    return@launch
                }
                val uri = repository.createFile(folderUri, trimmed, FileTextDetector.mimeTypeFor(trimmed))
                _state.update { it.copy(showNewFileDialog = false) }
                loadProject(folderUri, reopenLastFile = false, thenOpenUri = uri)
            } catch (_: Exception) {
                showError("Could not save this file.")
            }
        }
    }

    fun onUnsavedChangesSaveConfirmed() {
        viewModelScope.launch {
            if (saveCurrentFileSuspend()) {
                val pending = _state.value.pendingFileToOpen
                _state.update { it.copy(showUnsavedChangesDialog = false, pendingFileToOpen = null) }
                pending?.let { openFile(it) }
            }
        }
    }

    fun onUnsavedChangesDiscardConfirmed() {
        val pending = _state.value.pendingFileToOpen
        _state.update { it.copy(showUnsavedChangesDialog = false, pendingFileToOpen = null) }
        pending?.let { openFile(it) }
    }

    fun onUnsavedChangesCancelled() {
        _state.update { it.copy(showUnsavedChangesDialog = false, pendingFileToOpen = null) }
    }

    fun onPreviewClosed() {
        _state.update { it.copy(isPreviewOpen = false, previewHtml = "") }
    }

    fun onErrorShown() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun restoreLastProject() {
        val folderUri = preferences.getLastFolderUri() ?: return
        val hasPermission = contentResolver.persistedUriPermissions.any {
            it.uri == folderUri && it.isReadPermission && it.isWritePermission
        }
        if (!hasPermission) {
            preferences.clearProject()
            showError("Folder permission is missing. Please choose the project folder again.")
            return
        }
        loadProject(folderUri, reopenLastFile = true)
    }

    private fun loadProject(folderUri: Uri, reopenLastFile: Boolean, thenOpenUri: Uri? = null) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    projectFolderUri = folderUri,
                    projectFolderName = repository.getDisplayName(folderUri),
                    isLoadingTree = true,
                    errorMessage = null
                )
            }
            try {
                val files = repository.loadTree(folderUri)
                _state.update { it.copy(files = files, isLoadingTree = false) }
                val targetUri = thenOpenUri ?: if (reopenLastFile) preferences.getLastFileUri() else null
                targetUri?.let { uri -> files.findByUri(uri)?.let { openFile(it) } }
            } catch (_: Exception) {
                preferences.clearProject()
                _state.value = IdeState(errorMessage = "Could not open this folder. Please choose another folder.")
            }
        }
    }

    private fun openFile(node: SafFileNode) {
        viewModelScope.launch {
            if (!FileTextDetector.isTextLike(node)) {
                showError("This file does not look like a text file.")
                return@launch
            }
            if ((node.sizeBytes ?: 0L) > MAX_FILE_SIZE_BYTES) {
                showError("This file is too large for the editor.")
                return@launch
            }
            try {
                val text = repository.readTextFile(node.uri)
                preferences.setLastFileUri(node.uri)
                _state.update {
                    it.copy(
                        selectedFileUri = node.uri,
                        selectedFileName = node.name,
                        selectedFileExtension = FileTextDetector.extensionOf(node.name),
                        editorText = text,
                        savedEditorText = text,
                        isDirty = false,
                        errorMessage = null
                    )
                }
            } catch (_: FileNotFoundException) {
                preferences.setLastFileUri(null)
                _state.update {
                    it.copy(
                        selectedFileUri = null,
                        selectedFileName = null,
                        selectedFileExtension = null,
                        editorText = "",
                        savedEditorText = "",
                        isDirty = false,
                        errorMessage = "This file no longer exists."
                    )
                }
                _state.value.projectFolderUri?.let { loadProject(it, reopenLastFile = false) }
            } catch (_: Exception) {
                showError("Could not read this file.")
            }
        }
    }

    private fun saveCurrentFile() {
        viewModelScope.launch {
            saveCurrentFileSuspend()
        }
    }

    private suspend fun saveCurrentFileSuspend(): Boolean {
        val current = _state.value
        val uri = current.selectedFileUri ?: return false
        return try {
            if (!repository.canWrite(uri)) {
                showError("Could not save this file.")
                false
            } else {
                _state.update { it.copy(isSaving = true) }
                repository.writeTextFile(uri, current.editorText)
                _state.update {
                    it.copy(savedEditorText = current.editorText, isDirty = false, isSaving = false, errorMessage = null)
                }
                true
            }
        } catch (_: Exception) {
            _state.update { it.copy(isSaving = false, errorMessage = "Could not save this file.") }
            false
        }
    }

    private fun showError(message: String) {
        _state.update { it.copy(errorMessage = message) }
    }

    private fun List<SafFileNode>.findByUri(uri: Uri): SafFileNode? {
        for (node in this) {
            if (node.uri == uri) return node
            node.children.findByUri(uri)?.let { return it }
        }
        return null
    }

    class Factory(
        private val repository: ProjectRepository,
        private val preferences: AppPreferences,
        private val contentResolver: ContentResolver
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProjectViewModel(repository, preferences, contentResolver) as T
    }

    private companion object {
        const val MAX_FILE_SIZE_BYTES = 1_048_576L
    }
}
