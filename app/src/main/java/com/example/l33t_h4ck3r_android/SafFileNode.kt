package com.example.l33t_h4ck3r_android

import android.net.Uri

data class SafFileNode(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val children: List<SafFileNode> = emptyList()
)

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
    val previewHtml: String = "",
    val pendingFileToOpen: SafFileNode? = null,
    val showUnsavedChangesDialog: Boolean = false,
    val showNewFileDialog: Boolean = false,
    val errorMessage: String? = null
)
