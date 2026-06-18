package com.example.l33t_h4ck3r_android

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

interface ProjectRepository {
    suspend fun loadTree(folderUri: Uri): List<SafFileNode>
    suspend fun readTextFile(fileUri: Uri): String
    suspend fun writeTextFile(fileUri: Uri, text: String)
    suspend fun createFile(parentFolderUri: Uri, fileName: String, mimeType: String): Uri
    suspend fun fileExists(parentFolderUri: Uri, fileName: String): Boolean
    suspend fun getDisplayName(uri: Uri): String?
    suspend fun canWrite(uri: Uri): Boolean
    suspend fun fileExists(uri: Uri): Boolean
}

class SafProjectRepository(private val context: Context) : ProjectRepository {
    private val resolver: ContentResolver = context.contentResolver

    override suspend fun loadTree(folderUri: Uri): List<SafFileNode> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, folderUri)
            ?: throw FileNotFoundException("Folder not found")
        if (!root.exists() || !root.isDirectory) throw FileNotFoundException("Folder not found")
        root.listFiles().map { it.toNode() }.sortedForTree()
    }

    override suspend fun readTextFile(fileUri: Uri): String = withContext(Dispatchers.IO) {
        resolver.openInputStream(fileUri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw FileNotFoundException("File not found")
    }

    override suspend fun writeTextFile(fileUri: Uri, text: String) = withContext(Dispatchers.IO) {
        resolver.openOutputStream(fileUri, "wt")?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        } ?: throw FileNotFoundException("File not found")
    }

    override suspend fun createFile(parentFolderUri: Uri, fileName: String, mimeType: String): Uri =
        withContext(Dispatchers.IO) {
            val parent = DocumentFile.fromTreeUri(context, parentFolderUri)
                ?: throw FileNotFoundException("Folder not found")
            parent.createFile(mimeType, fileName)?.uri
                ?: throw FileNotFoundException("Could not create file")
        }

    override suspend fun fileExists(parentFolderUri: Uri, fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            val parent = DocumentFile.fromTreeUri(context, parentFolderUri) ?: return@withContext false
            parent.listFiles().any { it.name.equals(fileName, ignoreCase = true) }
        }

    override suspend fun getDisplayName(uri: Uri): String? = withContext(Dispatchers.IO) {
        DocumentFile.fromTreeUri(context, uri)?.name ?: DocumentFile.fromSingleUri(context, uri)?.name
    }

    override suspend fun canWrite(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        DocumentFile.fromSingleUri(context, uri)?.canWrite() ?: true
    }

    override suspend fun fileExists(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        DocumentFile.fromSingleUri(context, uri)?.exists() == true
    }

    private fun DocumentFile.toNode(): SafFileNode {
        val children = if (isDirectory) listFiles().map { it.toNode() }.sortedForTree() else emptyList()
        return SafFileNode(
            name = name.orEmpty(),
            uri = uri,
            isDirectory = isDirectory,
            mimeType = type,
            sizeBytes = if (isFile) length() else null,
            children = children
        )
    }

    private fun List<SafFileNode>.sortedForTree(): List<SafFileNode> =
        sortedWith(compareBy<SafFileNode> { !it.isDirectory }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
}
