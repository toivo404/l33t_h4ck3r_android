package com.example.l33t_h4ck3r_android

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.l33t_h4ck3r_android.ui.theme.L33t_h4ck3r_androidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            L33t_h4ck3r_androidTheme {
                val context = LocalContext.current
                val viewModel: ProjectViewModel = viewModel(
                    factory = ProjectViewModel.Factory(
                        repository = SafProjectRepository(context.applicationContext),
                        preferences = AppPreferences(context.applicationContext),
                        contentResolver = context.contentResolver
                    )
                )
                val folderLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    if (uri != null) {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        } catch (_: SecurityException) {
                            // Some providers reject persisted grants even after selection; the ViewModel reports this.
                        }
                        viewModel.onProjectFolderPicked(uri)
                    }
                }
                IdeScreen(
                    viewModel = viewModel,
                    onChooseFolder = { folderLauncher.launch(null) }
                )
            }
        }
    }
}

@Composable
fun IdeScreen(viewModel: ProjectViewModel, onChooseFolder: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        viewModel.onErrorShown()
    }

    if (state.isPreviewOpen) {
        PreviewScreen(
            fileName = state.selectedFileName.orEmpty(),
            html = state.previewHtml,
            onBack = viewModel::onPreviewClosed
        )
        return
    }

    Scaffold(
        topBar = {
            IdeTopBar(
                state = state,
                onChooseFolder = onChooseFolder,
                onSave = viewModel::onSaveClicked,
                onNewFile = viewModel::onCreateNewFileClicked,
                onRun = viewModel::onRunClicked
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {
            if (state.projectFolderUri == null) {
                ChooseFolderEmptyState(onChooseFolder)
            } else {
                IdeContent(
                    state = state,
                    onFileClicked = viewModel::onFileClicked,
                    onEditorTextChanged = viewModel::onEditorTextChanged
                )
            }
        }
    }

    if (state.showNewFileDialog) {
        NewFileDialog(
            onConfirm = viewModel::onNewFileConfirmed,
            onDismiss = viewModel::onNewFileDismissed
        )
    }

    if (state.showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onSave = viewModel::onUnsavedChangesSaveConfirmed,
            onDiscard = viewModel::onUnsavedChangesDiscardConfirmed,
            onCancel = viewModel::onUnsavedChangesCancelled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeTopBar(
    state: IdeState,
    onChooseFolder: () -> Unit,
    onSave: () -> Unit,
    onNewFile: () -> Unit,
    onRun: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text("L33t_h4ck3r", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        state.projectFolderName ?: "No project",
                        state.selectedFileName?.let { if (state.isDirty) "$it *" else it }
                    ).joinToString(" / "),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            TextButton(onClick = onChooseFolder) { Text("Folder") }
            TextButton(
                onClick = onSave,
                enabled = state.selectedFileUri != null && state.isDirty && !state.isSaving
            ) {
                Text(if (state.isSaving) "Saving" else "Save")
            }
            TextButton(onClick = onNewFile, enabled = state.projectFolderUri != null) { Text("New") }
            TextButton(onClick = onRun, enabled = state.selectedFileUri != null) { Text("Run") }
        }
    )
}

@Composable
fun ChooseFolderEmptyState(onChooseFolder: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onChooseFolder) {
            Text("Choose project folder")
        }
    }
}

@Composable
fun IdeContent(
    state: IdeState,
    onFileClicked: (SafFileNode) -> Unit,
    onEditorTextChanged: (String) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < 720.dp) {
            Column(Modifier.fillMaxSize()) {
                FileExplorer(
                    state = state,
                    onFileClicked = onFileClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
                EditorPane(
                    state = state,
                    onEditorTextChanged = onEditorTextChanged,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                FileExplorer(
                    state = state,
                    onFileClicked = onFileClicked,
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                )
                EditorPane(
                    state = state,
                    onEditorTextChanged = onEditorTextChanged,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun FileExplorer(
    state: IdeState,
    onFileClicked: (SafFileNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val expanded = remember(state.projectFolderUri) { mutableStateMapOf<String, Boolean>() }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            Text(
                state.projectFolderName ?: "Project",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            when {
                state.isLoadingTree -> Text("Loading files...")
                state.files.isEmpty() -> Text("This folder is empty.")
                else -> state.files.forEach { node ->
                    FileTreeItem(
                        node = node,
                        depth = 0,
                        expanded = expanded,
                        selectedUri = state.selectedFileUri?.toString(),
                        onFileClicked = onFileClicked
                    )
                }
            }
        }
    }
}

@Composable
fun FileTreeItem(
    node: SafFileNode,
    depth: Int,
    expanded: MutableMap<String, Boolean>,
    selectedUri: String?,
    onFileClicked: (SafFileNode) -> Unit
) {
    val key = node.uri.toString()
    val isExpanded = expanded[key] == true
    val isSelected = selectedUri == key
    val background = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(4.dp))
            .clickable {
                if (node.isDirectory) expanded[key] = !isExpanded else onFileClicked(node)
            }
            .padding(start = (depth * 16).dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when {
                node.isDirectory && isExpanded -> "v"
                node.isDirectory -> ">"
                else -> "-"
            },
            modifier = Modifier.width(18.dp),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = node.name.ifBlank { "(unnamed)" },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    if (node.isDirectory && isExpanded) {
        if (node.children.isEmpty()) {
            Text(
                "This folder is empty.",
                modifier = Modifier.padding(start = ((depth + 1) * 16).dp, top = 4.dp, bottom = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            node.children.forEach { child ->
                FileTreeItem(child, depth + 1, expanded, selectedUri, onFileClicked)
            }
        }
    }
}

@Composable
fun EditorPane(
    state: IdeState,
    onEditorTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                state.selectedFileName ?: "No file selected",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (state.isDirty) Text("Unsaved", style = MaterialTheme.typography.bodySmall)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                .padding(12.dp)
        ) {
            if (state.selectedFileUri == null) {
                Text("Select a text file from the project tree.")
            } else {
                val verticalScroll = rememberScrollState()
                BasicTextField(
                    value = state.editorText,
                    onValueChange = onEditorTextChanged,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScroll)
                )
            }
        }
    }
}

@Composable
fun NewFileDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var fileName by remember { mutableStateOf("index.html") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New file") },
        text = {
            TextField(
                value = fileName,
                onValueChange = { fileName = it },
                singleLine = true,
                label = { Text("Filename") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(fileName) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Unsaved changes") },
        text = { Text("Save changes before opening another file?") },
        confirmButton = {
            TextButton(onClick = onSave) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) { Text("Discard") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(fileName: String, html: String, onBack: () -> Unit) {
    var refreshKey by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(fileName.ifBlank { "Preview" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
                actions = {
                    TextButton(onClick = { refreshKey++ }) { Text("Refresh") }
                }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                }
            },
            update = { webView ->
                refreshKey
                // MVP limitation: relative CSS, JS, images, and nested paths from SAF are best-effort only.
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        )
    }
}
