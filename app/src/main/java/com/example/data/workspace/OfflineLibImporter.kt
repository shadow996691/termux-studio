package com.example.data.workspace

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipInputStream

data class OfflineImportReport(
    val libName: String,
    val destPath: String,
    val importedCount: Int
)

class OfflineLibImporter(private val context: Context) {

    suspend fun importFiles(
        workspaceRoot: File,
        rawLibName: String,
        uris: List<Uri>
    ): Result<OfflineImportReport> = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No files selected"))
        }
        val cleanName = sanitizeLibName(rawLibName.ifBlank { inferNameFromUri(uris.first()) })
        val libDir = File(workspaceRoot, "libs/$cleanName").apply { mkdirs() }
        var totalImported = 0

        try {
            for (uri in uris) {
                val fileName = queryDisplayName(uri) ?: "imported_file_${totalImported + 1}"
                val lower = fileName.lowercase()
                when {
                    lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val extracted = TarGzExtractor.extractTarGz(
                                inputStream = stream,
                                destDir = libDir,
                                stripFirstComponent = true
                            ).getOrThrow()
                            totalImported += maxOf(extracted, 1)
                        }
                    }
                    lower.endsWith(".zip") -> {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            totalImported += extractZip(stream, libDir)
                        }
                    }
                    lower.endsWith(".h") || lower.endsWith(".hpp") ||
                        lower.endsWith(".hh") || lower.endsWith(".hxx") -> {
                        val incDir = File(libDir, "include").apply { mkdirs() }
                        copyUriToFile(uri, File(incDir, fileName))
                        totalImported++
                    }
                    lower.endsWith(".so") || ".so." in lower || lower.endsWith(".a") -> {
                        val binDir = File(libDir, "lib").apply { mkdirs() }
                        copyUriToFile(uri, File(binDir, fileName))
                        totalImported++
                    }
                    else -> {
                        copyUriToFile(uri, File(libDir, fileName))
                        totalImported++
                    }
                }
            }
            Result.success(
                OfflineImportReport(
                    libName = cleanName,
                    destPath = "libs/$cleanName",
                    importedCount = totalImported
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFolderTree(
        workspaceRoot: File,
        rawLibName: String,
        treeUri: Uri
    ): Result<OfflineImportReport> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val inferredFolderName = rootDocId.substringAfterLast('/').substringAfterLast(':')
            val cleanName = sanitizeLibName(rawLibName.ifBlank { inferredFolderName.ifBlank { "custom_lib" } })
            val destRoot = File(workspaceRoot, "libs/$cleanName").apply { mkdirs() }
            var copiedCount = 0

            fun walk(parentDocId: String, currentDestDir: File, depth: Int) {
                if (depth > 8 || copiedCount > 1500) return
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                )
                resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idCol) ?: continue
                        val name = cursor.getString(nameCol) ?: continue
                        if (name == ".git" || name.contains("..")) continue
                        val mime = cursor.getString(mimeCol) ?: ""
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            val subDir = File(currentDestDir, name).apply { mkdirs() }
                            walk(docId, subDir, depth + 1)
                        } else {
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            copyUriToFile(fileUri, File(currentDestDir, name))
                            copiedCount++
                        }
                    }
                }
            }

            walk(rootDocId, destRoot, 0)
            Result.success(
                OfflineImportReport(
                    libName = cleanName,
                    destPath = "libs/$cleanName",
                    importedCount = copiedCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractZip(inputStream: java.io.InputStream, destDir: File): Int {
        var count = 0
        ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val cleanRel = entry.name.trimStart('/', '\\')
                if (cleanRel.isNotBlank() && !cleanRel.contains("..")) {
                    val out = File(destDir, cleanRel)
                    if (entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { output ->
                            zip.copyTo(output)
                        }
                        count++
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return count
    }

    private fun copyUriToFile(uri: Uri, destFile: File) {
        destFile.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Cannot read selected URI: $uri")
    }

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                return c.getString(0)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun inferNameFromUri(uri: Uri): String {
        val raw = queryDisplayName(uri) ?: "custom_lib"
        return raw.substringBefore('.').ifBlank { "custom_lib" }
    }

    private fun sanitizeLibName(name: String): String {
        return name.trim()
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
            .trim('.')
            .ifBlank { "custom_lib" }
    }
}
