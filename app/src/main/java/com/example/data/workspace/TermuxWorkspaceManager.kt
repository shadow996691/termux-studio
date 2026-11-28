package com.example.data.workspace

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat
import com.example.data.model.CodeFileType
import com.example.data.model.CompilerProbeStatus
import com.example.data.model.FolderLibraryItem
import com.example.data.model.WorkspaceNode
import com.example.data.termux.TermuxAptRepoBuilder
import com.example.data.termux.TermuxBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class TermuxWorkspaceManager(private val context: Context) {

    fun hasSharedStorageAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getSharedStorageRoot(): File {
        return File(Environment.getExternalStorageDirectory(), "TermuxStudio")
    }

    fun getActiveLocalRoot(): File {
        return if (hasSharedStorageAccess()) {
            getSharedStorageRoot()
        } else {
            context.getExternalFilesDir("TermuxStudio")
                ?: File(context.filesDir, "workspace/cpp_starter")
        }
    }

    fun ensureStarterWorkspace(): File {
        val root = getActiveLocalRoot()
        if (!root.exists()) {
            root.mkdirs()
        }
        val includeDir = File(root, "include").apply { mkdirs() }
        val srcDir = File(root, "src").apply { mkdirs() }

        val mainCpp = File(root, "main.cpp")
        if (!mainCpp.exists()) {
            File(includeDir, "vector_math.h").writeText(
                """
                #pragma once
                #include <cmath>
                #include <string>

                namespace vmath {
                    struct Vec2 {
                        double x;
                        double y;
                    };

                    inline double magnitude(const Vec2& v) {
                        return std::sqrt(v.x * v.x + v.y * v.y);
                    }

                    Vec2 add(const Vec2& a, const Vec2& b);
                    std::string format_vec(const Vec2& v);
                }
                """.trimIndent() + "\n"
            )

            File(srcDir, "vector_math.cpp").writeText(
                """
                #include "vector_math.h"
                #include <sstream>

                namespace vmath {
                    Vec2 add(const Vec2& a, const Vec2& b) {
                        return Vec2{a.x + b.x, a.y + b.y};
                    }

                    std::string format_vec(const Vec2& v) {
                        std::ostringstream oss;
                        oss << "(" << v.x << ", " << v.y << ")";
                        return oss.str();
                    }
                }
                """.trimIndent() + "\n"
            )

            mainCpp.writeText(
                """
                #include <iostream>
                #include "include/vector_math.h"
                #include "libs/app/app.h"

                int main() {
                    AppConfig cfg = app_default_config("TermuxStudioDemo");
                    app_init(&cfg);

                    vmath::Vec2 a{3.0, 4.0};
                    vmath::Vec2 b{1.5, -2.0};
                    auto sum = vmath::add(a, b);

                    std::cout << "a + b = " << vmath::format_vec(sum) << std::endl;
                    std::cout << "|a| = " << vmath::magnitude(a) << std::endl;
                    app_shutdown();
                    return 0;
                }
                """.trimIndent() + "\n"
            )
        }

        val raylibDemo = File(root, "raylib_demo.c")
        if (!raylibDemo.exists()) {
            raylibDemo.writeText(
                """
                #include "libs/raylib/include/raylib.h"
                #include "libs/app/app.h"

                /* Built with Raylib 6.2 in user folder libs/raylib/
                 * Run via Termux:X11 or package with: app Android | app Linux | app Window | app macOS -> app build
                 */
                int main(void) {
                    AppConfig cfg = app_default_config("RaylibDemo");
                    cfg.enable_raylib = true;
                    app_init(&cfg);

                    InitWindow(1280, 720, "Termux studio + Raylib 6.2 GPU");
                    SetTargetFPS(60);

                    while (!WindowShouldClose()) {
                        BeginDrawing();
                        ClearBackground(RAYWHITE);
                        DrawText("Hello from Raylib 6.2 + GPU Drivers!", 240, 320, 32, DARKBLUE);
                        DrawFPS(20, 20);
                        EndDrawing();
                    }

                    CloseWindow();
                    app_shutdown();
                    return 0;
                }
                """.trimIndent() + "\n"
            )
        }

        val helloJava = File(root, "Hello.java")
        if (!helloJava.exists()) {
            helloJava.writeText(
                """
                public class Hello {
                    public static void main(String[] args) {
                        System.out.println("Hello from OpenJDK in Termux studio!");
                    }
                }
                """.trimIndent() + "\n"
            )
        }

        val makefile = File(root, "Makefile")
        if (!makefile.exists()) {
            makefile.writeText(
                """
                CXX := clang++
                CXXFLAGS := -std=c++20 -Wall -Wextra -Iinclude -Ilibs/app -Ilibs/raylib/include

                all: app_out

                app_out: main.cpp src/vector_math.cpp libs/app/app_runtime.c
                	${'$'}(CXX) ${'$'}(CXXFLAGS) main.cpp src/vector_math.cpp libs/app/app_runtime.c -o app_out -lm

                clean:
                	rm -f app_out *.o *.a *.so
                """.trimIndent() + "\n"
            )
        }

        syncBundledLibrariesToDir(root)
        return root
    }

    fun syncBundledLibrariesToDir(root: File) {
        val libsDir = File(root, "libs").apply { mkdirs() }
        val raylibDir = File(libsDir, "raylib").apply { mkdirs() }
        val raylibInclude = File(raylibDir, "include")
        val raylibLib = File(raylibDir, "lib")

        // Extract user-supplied Raylib 6.2 ARM64 .tar.gz directly into <userFolder>/libs/raylib/
        if (!File(raylibInclude, "raylib.h").exists() || !File(raylibLib, "libraylib.a").exists()) {
            runCatching {
                context.assets.open("libs/raylib-6.2_linux_arm64.tar.gz").use { stream ->
                    TarGzExtractor.extractTarGz(
                        inputStream = stream,
                        destDir = raylibDir,
                        stripFirstComponent = true
                    )
                }
            }
            // Fallback single header if extraction fails for any reason
            if (!File(raylibInclude, "raylib.h").exists()) {
                copyAssetIfMissingOrUpdated("libs/raylib/raylib.h", File(raylibInclude, "raylib.h"))
            }
        }

        // Put bundled App library directly into user's folder <userFolder>/libs/app/
        val appDir = File(libsDir, "app").apply { mkdirs() }
        copyAssetIfMissingOrUpdated("libs/app/app.h", File(appDir, "app.h"))
        copyAssetIfMissingOrUpdated("libs/app/app_runtime.c", File(appDir, "app_runtime.c"))
        val cliDest = File(appDir, "app")
        copyAssetIfMissingOrUpdated("libs/app/app_cli.sh", cliDest)
        cliDest.setExecutable(true)

        // Generate real termux-studio .deb + GitHub APT repo in <userFolder>/termux-repo/
        val repoDir = File(root, "termux-repo")
        val debMarker = File(repoDir, "pool/main/t/termux-studio/${TermuxAptRepoBuilder.DEB_FILE_NAME}")
        if (!debMarker.exists()) {
            TermuxAptRepoBuilder.generateAptRepository(context, repoDir)
        }

        File(root, ".termux-ipc").mkdirs()
    }

    fun forceRegenerateAptRepo(root: File): Result<File> {
        return TermuxAptRepoBuilder.generateAptRepository(context, File(root, "termux-repo"))
    }

    private fun copyAssetIfMissingOrUpdated(assetPath: String, dest: File) {
        try {
            val bytes = context.assets.open(assetPath).readBytes()
            if (!dest.exists() || dest.length() != bytes.size.toLong()) {
                dest.parentFile?.mkdirs()
                dest.writeBytes(bytes)
            }
        } catch (_: Exception) {
        }
    }

    fun resolveTermuxWorkdirFromTreeUri(treeUri: Uri?): String {
        if (treeUri == null) return getActiveLocalRoot().absolutePath
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            when {
                treeUri.authority == "com.termux.documents" && docId.startsWith("/") -> docId
                treeUri.authority == "com.android.externalstorage.documents" && docId.startsWith("primary:") -> {
                    "/storage/emulated/0/" + docId.removePrefix("primary:")
                }
                else -> getActiveLocalRoot().absolutePath
            }
        } catch (_: Exception) {
            getActiveLocalRoot().absolutePath
        }
    }

    suspend fun listWorkspaceNodes(treeUriString: String?): List<WorkspaceNode> = withContext(Dispatchers.IO) {
        if (treeUriString != null) {
            val safNodes = listSafTreeNodes(Uri.parse(treeUriString))
            if (safNodes.isNotEmpty()) return@withContext safNodes
        }
        val localRoot = ensureStarterWorkspace()
        listLocalNodes(localRoot)
    }

    private fun listLocalNodes(root: File): List<WorkspaceNode> {
        val output = mutableListOf<WorkspaceNode>()
        fun walk(dir: File, depth: Int) {
            if (depth > 5 || output.size > 600) return
            val children = dir.listFiles()?.sortedWith(
                compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }
            ) ?: return
            for (child in children) {
                if (child.name == ".git" || child.name == ".termux-ipc") continue
                val rel = child.relativeTo(root).path
                val type = CodeFileType.fromFileName(child.name, child.isDirectory)
                output.add(
                    WorkspaceNode(
                        uriString = Uri.fromFile(child).toString(),
                        name = child.name,
                        relativePath = rel,
                        termuxPath = "${root.absolutePath}/$rel",
                        fileType = type,
                        sizeBytes = if (child.isDirectory) 0L else child.length(),
                        lastModified = child.lastModified(),
                        isDirectory = child.isDirectory,
                        depth = depth
                    )
                )
                if (child.isDirectory) walk(child, depth + 1)
            }
        }
        walk(root, 0)
        return output
    }

    private fun listSafTreeNodes(treeUri: Uri): List<WorkspaceNode> {
        val resolver = context.contentResolver
        val rootDocId = try {
            DocumentsContract.getTreeDocumentId(treeUri)
        } catch (_: Exception) {
            return emptyList()
        }
        val baseTermuxPath = resolveTermuxWorkdirFromTreeUri(treeUri)
        val result = mutableListOf<WorkspaceNode>()

        fun walk(parentDocId: String, relPrefix: String, depth: Int) {
            if (depth > 5 || result.size > 500) return
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
            val entries = mutableListOf<WorkspaceNode>()
            val dirsToVisit = mutableListOf<Pair<String, String>>()

            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idCol) ?: continue
                    val name = cursor.getString(nameCol) ?: continue
                    if (name == ".git" || name == ".termux" || name == ".termux-ipc") continue
                    val mime = cursor.getString(mimeCol) ?: ""
                    val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val mod = if (modCol >= 0) cursor.getLong(modCol) else 0L
                    val relPath = if (relPrefix.isEmpty()) name else "$relPrefix/$name"
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    val termuxPath = if (docId.startsWith("/")) docId else "$baseTermuxPath/$relPath"

                    val node = WorkspaceNode(
                        uriString = docUri.toString(),
                        name = name,
                        relativePath = relPath,
                        termuxPath = termuxPath,
                        fileType = CodeFileType.fromFileName(name, isDir),
                        sizeBytes = size,
                        lastModified = mod,
                        isDirectory = isDir,
                        depth = depth
                    )
                    entries.add(node)
                    if (isDir) dirsToVisit.add(docId to relPath)
                }
            }

            entries.sortedWith(compareBy<WorkspaceNode> { !it.isDirectory }.thenBy { it.name.lowercase() })
                .forEach { result.add(it) }
            dirsToVisit.sortedBy { it.second.lowercase() }.forEach { (childDocId, childRel) ->
                walk(childDocId, childRel, depth + 1)
            }
        }

        walk(rootDocId, "", 0)
        return result
    }

    /**
     * Scans the current user's workspace folder for libraries inside `libs/`, plus any standalone
     * `.so` shared objects, `.a` static archives, or include directories for the Add Library panel.
     */
    fun scanFolderLibraries(nodes: List<WorkspaceNode>): List<FolderLibraryItem> {
        val discovered = linkedMapOf<String, FolderLibraryItem>()

        // 1. Subdirectories directly inside libs/<libName>
        val libSubdirs = nodes.filter { node ->
            node.isDirectory &&
                node.relativePath.startsWith("libs/") &&
                node.relativePath.count { it == '/' } == 1
        }

        libSubdirs.forEach { dirNode ->
            val libName = dirNode.name
            val prefix = "${dirNode.relativePath}/"
            val children = nodes.filter { !it.isDirectory && it.relativePath.startsWith(prefix) }
            val headers = children.filter { it.fileType == CodeFileType.HEADER }.map { it.relativePath }
            val binaries = children.filter {
                it.fileType == CodeFileType.SHARED_OBJECT || it.fileType == CodeFileType.STATIC_ARCHIVE
            }.map { it.relativePath }

            val incDirs = buildList {
                if (nodes.any { it.isDirectory && it.relativePath == "${dirNode.relativePath}/include" }) {
                    add("${dirNode.relativePath}/include")
                }
                add(dirNode.relativePath)
            }.distinct()

            val libDirs = buildList {
                if (nodes.any { it.isDirectory && it.relativePath == "${dirNode.relativePath}/lib" }) {
                    add("${dirNode.relativePath}/lib")
                } else if (binaries.isNotEmpty()) {
                    add(dirNode.relativePath)
                }
            }.distinct()

            val suggestedFlags = when (libName.lowercase()) {
                "raylib" -> "-Ilibs/raylib/include -Llibs/raylib/lib -lraylib -lGLESv2 -lEGL -lm -lpthread -ldl"
                "app" -> "-Ilibs/app libs/app/app_runtime.c -lm -lpthread -ldl"
                else -> buildString {
                    incDirs.forEach { append("-I$it ") }
                    libDirs.forEach { append("-L$it ") }
                    binaries.map { it.substringAfterLast('/') }
                        .distinct()
                        .forEach { bin ->
                            val clean = bin.removePrefix("lib")
                                .substringBefore(".so")
                                .substringBefore(".a")
                            if (clean.isNotBlank()) append("-l$clean ")
                        }
                }.trim()
            }

            discovered["dir:${dirNode.relativePath}"] = FolderLibraryItem(
                id = "dir:${dirNode.relativePath}",
                name = when (libName.lowercase()) {
                    "raylib" -> "Raylib 6.2 (ARM64 in user folder)"
                    "app" -> "app (C/C++/Zig/ASM/Rust Packager)"
                    else -> libName
                },
                relativePath = dirNode.relativePath,
                includeDirs = incDirs,
                libDirs = libDirs,
                binaryFiles = binaries,
                headerFiles = headers,
                suggestedFlags = suggestedFlags,
                isBuiltIn = libName.lowercase() in setOf("raylib", "app")
            )
        }

        // 2. Standalone .so / .a archives outside libs/<name>/lib
        nodes.filter {
            !it.isDirectory &&
                (it.fileType == CodeFileType.SHARED_OBJECT || it.fileType == CodeFileType.STATIC_ARCHIVE) &&
                !it.relativePath.startsWith("libs/raylib/")
        }.forEach { binNode ->
            val parentDir = binNode.relativePath.substringBeforeLast('/', ".")
            val libFlag = if (binNode.name.startsWith("lib")) {
                "-l" + binNode.name.removePrefix("lib").substringBefore(".so").substringBefore(".a")
            } else {
                "-l:${binNode.name}"
            }
            val flags = if (parentDir == ".") libFlag else "-L$parentDir $libFlag"
            discovered["bin:${binNode.relativePath}"] = FolderLibraryItem(
                id = "bin:${binNode.relativePath}",
                name = binNode.name,
                relativePath = binNode.relativePath,
                includeDirs = emptyList(),
                libDirs = listOf(parentDir),
                binaryFiles = listOf(binNode.relativePath),
                headerFiles = emptyList(),
                suggestedFlags = flags,
                isBuiltIn = false
            )
        }

        return discovered.values.toList()
    }

    fun readIpcCompilerProbe(): Map<String, CompilerProbeStatus> {
        val ipcDir = File(getActiveLocalRoot(), ".termux-ipc")
        val statusFile = File(ipcDir, "compilers.status")
        if (!statusFile.exists()) return emptyMap()
        return runCatching {
            statusFile.readLines()
                .mapNotNull { line ->
                    val eq = line.indexOf('=')
                    if (eq <= 0) return@mapNotNull null
                    val key = line.substring(0, eq).trim()
                    val rest = line.substring(eq + 1)
                    val installed = rest.startsWith("1|")
                    val ver = rest.substringAfter('|', "").trim()
                    key to CompilerProbeStatus(isInstalled = installed, versionLine = ver)
                }
                .toMap()
        }.getOrDefault(emptyMap())
    }

    fun isListenerAlive(): Boolean {
        val ipcDir = File(getActiveLocalRoot(), ".termux-ipc")
        val hb = File(ipcDir, "listener.heartbeat")
        if (!hb.exists()) return false
        val last = hb.readText().trim().toLongOrNull() ?: return false
        val nowSec = System.currentTimeMillis() / 1000L
        return (nowSec - last) in 0..180
    }

    suspend fun readFileText(uriString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: return@withContext Result.failure(IllegalArgumentException("Invalid file path")))
                Result.success(file.readText())
            } else {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    Result.success(reader.readText())
                } ?: Result.failure(IllegalStateException("Could not open input stream for $uriString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun writeFileText(uriString: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val file = File(uri.path ?: return@withContext Result.failure(IllegalArgumentException("Invalid file path")))
                file.parentFile?.mkdirs()
                file.writeText(content)
                Result.success(Unit)
            } else {
                context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
                    writer.write(content)
                } ?: return@withContext Result.failure(IllegalStateException("Could not open output stream"))
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFile(
        treeUriString: String?,
        relativeName: String,
        initialContent: String
    ): Result<WorkspaceNode> = withContext(Dispatchers.IO) {
        try {
            val cleanName = relativeName.trim().removePrefix("/")
            if (cleanName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("File name cannot be empty"))
            }
            if (treeUriString != null) {
                val treeUri = Uri.parse(treeUriString)
                val parts = cleanName.split("/").filter { it.isNotBlank() }
                var currentDocId = DocumentsContract.getTreeDocumentId(treeUri)
                for (folderName in parts.dropLast(1)) {
                    val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, currentDocId)
                    val existing = findChildDocId(treeUri, currentDocId, folderName)
                    currentDocId = existing ?: run {
                        val created = DocumentsContract.createDocument(
                            context.contentResolver,
                            parentUri,
                            DocumentsContract.Document.MIME_TYPE_DIR,
                            folderName
                        ) ?: throw IllegalStateException("Failed to create folder $folderName")
                        DocumentsContract.getDocumentId(created)
                    }
                }
                val fileName = parts.last()
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, currentDocId)
                val createdUri = DocumentsContract.createDocument(
                    context.contentResolver,
                    parentUri,
                    "text/plain",
                    fileName
                ) ?: return@withContext Result.failure(IllegalStateException("Failed to create $fileName"))

                if (initialContent.isNotEmpty()) {
                    writeFileText(createdUri.toString(), initialContent)
                }
                val basePath = resolveTermuxWorkdirFromTreeUri(treeUri)
                Result.success(
                    WorkspaceNode(
                        uriString = createdUri.toString(),
                        name = fileName,
                        relativePath = cleanName,
                        termuxPath = "$basePath/$cleanName",
                        fileType = CodeFileType.fromFileName(fileName, false)
                    )
                )
            } else {
                val root = ensureStarterWorkspace()
                val target = File(root, cleanName)
                target.parentFile?.mkdirs()
                target.writeText(initialContent)
                Result.success(
                    WorkspaceNode(
                        uriString = Uri.fromFile(target).toString(),
                        name = target.name,
                        relativePath = cleanName,
                        termuxPath = "${root.absolutePath}/$cleanName",
                        fileType = CodeFileType.fromFileName(target.name, false)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(uriString: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.deleteRecursively()) Result.success(Unit)
                else Result.failure(IllegalStateException("Unable to delete ${f.name}"))
            } else {
                val deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
                if (deleted) Result.success(Unit)
                else Result.failure(IllegalStateException("Unable to delete document"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findChildDocId(treeUri: Uri, parentDocId: String, name: String): String? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        context.contentResolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == name) return c.getString(0)
            }
        }
        return null
    }

    fun extractHeaderSymbols(headerSource: String): List<String> {
        val symbols = mutableSetOf<String>()
        val defineRegex = Regex("""^\s*#\s*define\s+([A-Za-z_][A-Za-z0-9_]*)""", RegexOption.MULTILINE)
        val typeRegex = Regex("""\b(?:struct|class|enum|namespace)\s+([A-Za-z_][A-Za-z0-9_]*)""")
        val fnRegex = Regex("""^\s*(?:[A-Za-z_][A-Za-z0-9_:*&<>\s]+)\s+([A-Za-z_][A-Za-z0-9_]*)\s*\([^;{}]*\)\s*(?:const)?\s*[;{]""", RegexOption.MULTILINE)

        defineRegex.findAll(headerSource).forEach { symbols.add(it.groupValues[1]) }
        typeRegex.findAll(headerSource).forEach { symbols.add(it.groupValues[1]) }
        fnRegex.findAll(headerSource).forEach {
            val fn = it.groupValues[1]
            if (fn !in setOf("if", "for", "while", "switch", "return")) symbols.add(fn)
        }
        return symbols.sorted()
    }
}
