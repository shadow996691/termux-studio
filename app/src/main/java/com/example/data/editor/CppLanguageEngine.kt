package com.example.data.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.data.model.AutocompleteSuggestion
import com.example.data.model.CodeFileType
import com.example.data.model.CompilerDiagnostic
import com.example.data.model.StandardHeaderDoc
import com.example.data.model.SuggestionKind
import com.example.data.model.SystemLibrarySpec
import com.example.ui.theme.FrostText
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxFunction
import com.example.ui.theme.SyntaxHeaderPath
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxOperator
import com.example.ui.theme.SyntaxPreprocessor
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxType

object CppLanguageEngine {

    val standardHeaders: List<StandardHeaderDoc> = listOf(
        StandardHeaderDoc("raylib.h", "C/C++ (Built-in)", "Raylib 2D/3D hardware-accelerated graphics, audio & input", listOf("InitWindow", "CloseWindow", "WindowShouldClose", "BeginDrawing", "EndDrawing", "ClearBackground", "DrawText", "DrawCircle", "DrawRectangle", "SetTargetFPS")),
        StandardHeaderDoc("app.h", "C/C++ (Built-in)", "Termux studio App library for Android/Linux/Window/macOS builds & GPU", listOf("AppConfig", "app_default_config", "app_init", "app_enable_gpu_drivers", "app_get_platform_name", "app_get_gpu_backend", "app_run", "app_shutdown")),
        StandardHeaderDoc("GLES3/gl3.h", "GPU Driver", "OpenGL ES 3.0 hardware GPU shader & rendering API", listOf("glClear", "glClearColor", "glDrawArrays", "glCreateShader")),
        StandardHeaderDoc("vulkan/vulkan.h", "GPU Driver", "Vulkan low-overhead cross-platform GPU driver API", listOf("VkInstance", "vkCreateInstance", "vkEnumeratePhysicalDevices")),
        StandardHeaderDoc("EGL/egl.h", "GPU Driver", "Native platform GPU surface & context interface", listOf("eglGetDisplay", "eglInitialize", "eglCreateContext", "eglSwapBuffers")),
        StandardHeaderDoc("stdio.h", "C", "Standard buffered input/output (printf, scanf, fopen)", listOf("printf", "scanf", "fprintf", "fopen", "fclose", "fgets", "FILE")),
        StandardHeaderDoc("stdlib.h", "C", "Memory allocation, process control & conversions", listOf("malloc", "calloc", "realloc", "free", "exit", "atoi", "rand", "system")),
        StandardHeaderDoc("stdint.h", "C", "Fixed-width integer types (uint8_t, int32_t, int64_t)", listOf("int8_t", "uint8_t", "int32_t", "uint32_t", "int64_t", "uint64_t", "size_t")),
        StandardHeaderDoc("stdbool.h", "C", "Boolean type and true/false macros", listOf("bool", "true", "false")),
        StandardHeaderDoc("string.h", "C", "C-string & raw memory operations", listOf("strlen", "strcpy", "strncpy", "strcmp", "memcpy", "memset", "memmove")),
        StandardHeaderDoc("math.h", "C (-lm)", "Floating-point mathematical functions", listOf("sqrt", "pow", "sin", "cos", "tan", "abs", "fabs", "floor", "ceil")),
        StandardHeaderDoc("time.h", "C", "Time and date utilities", listOf("time", "clock", "localtime", "strftime", "difftime", "timespec")),
        StandardHeaderDoc("errno.h", "C", "System error number definitions", listOf("errno", "EINVAL", "ENOMEM", "ENOENT")),
        StandardHeaderDoc("unistd.h", "POSIX", "POSIX OS API (sleep, fork, read, write, pipe)", listOf("read", "write", "close", "fork", "execvp", "sleep", "usleep", "getpid")),
        StandardHeaderDoc("fcntl.h", "POSIX", "File descriptor control & open flags", listOf("open", "O_RDONLY", "O_WRONLY", "O_CREAT", "fcntl")),
        StandardHeaderDoc("pthread.h", "POSIX (-lpthread)", "POSIX threads, mutexes & condition variables", listOf("pthread_t", "pthread_create", "pthread_join", "pthread_mutex_t", "pthread_mutex_lock")),
        StandardHeaderDoc("dlfcn.h", "POSIX (-ldl)", "Dynamic linking loader for .so shared libraries", listOf("dlopen", "dlsym", "dlclose", "dlerror", "RTLD_LAZY", "RTLD_NOW")),
        StandardHeaderDoc("sys/socket.h", "POSIX", "BSD network sockets API", listOf("socket", "bind", "listen", "accept", "connect", "send", "recv", "AF_INET", "SOCK_STREAM")),
        StandardHeaderDoc("arpa/inet.h", "POSIX", "Internet address conversion utilities", listOf("htons", "htonl", "inet_pton", "inet_ntop", "sockaddr_in")),
        StandardHeaderDoc("iostream", "C++", "Standard stream I/O (cin, cout, cerr, endl)", listOf("std::cout", "std::cin", "std::cerr", "std::endl")),
        StandardHeaderDoc("vector", "C++", "Dynamic contiguous array container", listOf("std::vector", "push_back", "emplace_back", "size", "reserve", "clear")),
        StandardHeaderDoc("string", "C++", "Mutable std::string & string_view helpers", listOf("std::string", "std::getline", "std::to_string", "substr", "find")),
        StandardHeaderDoc("string_view", "C++17", "Non-owning string reference", listOf("std::string_view")),
        StandardHeaderDoc("memory", "C++", "Smart pointers and allocators", listOf("std::unique_ptr", "std::shared_ptr", "std::make_unique", "std::make_shared")),
        StandardHeaderDoc("algorithm", "C++", "Sorting, searching & range algorithms", listOf("std::sort", "std::find", "std::transform", "std::for_each", "std::min", "std::max")),
        StandardHeaderDoc("map", "C++", "Ordered key-value associative tree", listOf("std::map", "emplace", "contains", "find")),
        StandardHeaderDoc("unordered_map", "C++", "Hash-table key-value container", listOf("std::unordered_map", "emplace", "find", "bucket_count")),
        StandardHeaderDoc("set", "C++", "Ordered unique-element container", listOf("std::set", "insert", "erase", "find")),
        StandardHeaderDoc("thread", "C++11 (-lpthread)", "Standard C++ threading support", listOf("std::thread", "std::this_thread::sleep_for", "join", "detach")),
        StandardHeaderDoc("mutex", "C++11", "Mutual exclusion locks & RAII guards", listOf("std::mutex", "std::lock_guard", "std::unique_lock", "std::scoped_lock")),
        StandardHeaderDoc("future", "C++11", "Asynchronous futures & std::async", listOf("std::future", "std::promise", "std::async")),
        StandardHeaderDoc("chrono", "C++11", "High-resolution clocks & durations", listOf("std::chrono::steady_clock", "std::chrono::milliseconds", "duration_cast")),
        StandardHeaderDoc("filesystem", "C++17", "Cross-platform paths & directory traversal", listOf("std::filesystem::path", "exists", "directory_iterator", "create_directories")),
        StandardHeaderDoc("fstream", "C++", "File input/output streams", listOf("std::ifstream", "std::ofstream", "std::fstream")),
        StandardHeaderDoc("sstream", "C++", "In-memory string streams", listOf("std::stringstream", "std::istringstream", "std::ostringstream")),
        StandardHeaderDoc("optional", "C++17", "Nullable value wrapper", listOf("std::optional", "std::nullopt", "value_or")),
        StandardHeaderDoc("variant", "C++17", "Type-safe discriminated union", listOf("std::variant", "std::visit", "std::get_if")),
        StandardHeaderDoc("span", "C++20", "Non-owning view over contiguous sequence", listOf("std::span")),
        StandardHeaderDoc("cstdio", "C++", "C++ wrapped <stdio.h>", listOf("std::printf", "std::snprintf", "std::FILE")),
        StandardHeaderDoc("cstdlib", "C++", "C++ wrapped <stdlib.h>", listOf("std::malloc", "std::free", "std::exit")),
        StandardHeaderDoc("cmath", "C++", "C++ math functions in std::", listOf("std::sqrt", "std::pow", "std::sin", "std::cos")),
        StandardHeaderDoc("cstdint", "C++", "C++ fixed-width integer types", listOf("std::int32_t", "std::uint64_t", "std::size_t"))
    )

    val systemLibraries: List<SystemLibrarySpec> = listOf(
        SystemLibrarySpec("-Ilibs/raylib/include -Llibs/raylib/lib -lraylib -lGLESv2 -lEGL", "Raylib 6.2 (User Folder)", "<raylib.h>", "ARM64 2D/3D graphics, audio & input in libs/raylib/", "built-in + pkg install raylib"),
        SystemLibrarySpec("-lapp", "App Cross-Packager (Built-in)", "<app.h>", "Build C/C++ into Android APK, Linux DEB, Window EXE, or macOS .app via build.config", "built-in (bundled)"),
        SystemLibrarySpec("-lGLESv3 -lEGL", "OpenGL ES 3.0 GPU", "<GLES3/gl3.h>", "Direct Android/Mesa OpenGL ES 3.0 hardware GPU shaders", "built-in driver"),
        SystemLibrarySpec("-lvulkan", "Vulkan GPU Driver", "<vulkan/vulkan.h>", "Direct Turnip/Vulkan high-throughput GPU compute & graphics", "pkg install vulkan-loader-android"),
        SystemLibrarySpec("-lm", "libm (Math)", "<math.h> / <cmath>", "Trigonometric, logarithmic, and power math functions", "built-in"),
        SystemLibrarySpec("-lpthread", "POSIX Threads", "<pthread.h> / <thread>", "Multithreading, mutexes, and std::thread runtime support", "built-in"),
        SystemLibrarySpec("-ldl", "Dynamic Loader", "<dlfcn.h>", "Load and invoke symbols from .so shared objects via dlopen/dlsym", "built-in"),
        SystemLibrarySpec("-lz", "zlib Compression", "<zlib.h>", "DEFLATE/gzip compression & decompression library", "pkg install zlib"),
        SystemLibrarySpec("-lcurl", "libcurl HTTP", "<curl/curl.h>", "HTTP/HTTPS networking and REST client library", "pkg install libcurl"),
        SystemLibrarySpec("-lsqlite3", "SQLite 3", "<sqlite3.h>", "Embedded relational SQL database C API", "pkg install sqlite"),
        SystemLibrarySpec("-lssl -lcrypto", "OpenSSL", "<openssl/ssl.h>", "TLS networking and cryptographic primitives", "pkg install openssl"),
        SystemLibrarySpec("-lncurses", "ncurses TUI", "<ncurses.h>", "Terminal UI windows, colors, and raw keyboard handling", "pkg install ncurses"),
        SystemLibrarySpec("-lreadline", "GNU Readline", "<readline/readline.h>", "Interactive line editing and history for CLI tools", "pkg install readline")
    )

    private val cppKeywords = setOf(
        "alignas", "alignof", "and", "asm", "auto", "break", "case", "catch", "class",
        "const", "constexpr", "consteval", "constinit", "continue", "co_await", "co_return",
        "co_yield", "decltype", "default", "delete", "do", "else", "enum", "explicit",
        "export", "extern", "false", "for", "friend", "goto", "if", "inline", "mutable",
        "namespace", "new", "noexcept", "nullptr", "operator", "override", "final",
        "private", "protected", "public", "register", "reinterpret_cast", "return",
        "sizeof", "static", "static_assert", "static_cast", "dynamic_cast", "const_cast",
        "struct", "switch", "template", "this", "thread_local", "throw", "true", "try",
        "typedef", "typeid", "typename", "union", "using", "virtual", "volatile", "while",
        "concept", "requires", "module", "import"
    )

    private val polyglotKeywords = setOf(
        // Java / Kotlin / C#
        "package", "import", "public", "private", "protected", "class", "interface", "extends",
        "implements", "static", "final", "void", "return", "new", "try", "catch", "finally",
        "throw", "throws", "if", "else", "for", "while", "switch", "case", "break", "continue",
        "fun", "val", "var", "object", "when", "override", "suspend", "data", "sealed",
        // Rust / Go / Zig
        "fn", "let", "mut", "pub", "use", "mod", "crate", "impl", "trait", "match", "loop",
        "func", "defer", "go", "chan", "select", "range", "type", "const", "comptime", "errdefer",
        // Fortran / Pascal / Nim
        "program", "subroutine", "function", "module", "implicit", "none", "begin", "end",
        "procedure", "proc",
        // Assembly
        "global", "extern", "section", "mov", "add", "sub", "mul", "jmp", "call", "ret",
        "syscall", "svc", "ldr", "str", "stp", "ldp", "adr"
    )

    private val types = setOf(
        "void", "bool", "char", "wchar_t", "char8_t", "char16_t", "char32_t",
        "short", "int", "long", "signed", "unsigned", "float", "double",
        "size_t", "ssize_t", "ptrdiff_t", "int8_t", "uint8_t", "int16_t", "uint16_t",
        "int32_t", "uint32_t", "int64_t", "uint64_t", "intptr_t", "uintptr_t",
        "FILE", "pthread_t", "pthread_mutex_t", "string", "vector", "map",
        "unordered_map", "set", "unique_ptr", "shared_ptr", "optional", "variant", "span",
        "String", "System", "List", "ArrayList", "HashMap", "i32", "i64", "u32", "u64",
        "usize", "f32", "f64", "Vec", "Option", "Result", "anyerror", "Color", "Vector2",
        "Vector3", "AppConfig", "AppPlatform"
    )

    private val snippets = listOf(
        AutocompleteSuggestion("int main(int argc, char* argv[]) {\n    \n    return 0;\n}", "main()", "Standard C/C++ entry point", SuggestionKind.SNIPPET, 0, 0),
        AutocompleteSuggestion("#include \"libs/raylib/include/raylib.h\"\n", "#include raylib", "Include Raylib 6.2 from user folder", SuggestionKind.LIBRARY_API, 0, 0),
        AutocompleteSuggestion("#include \"libs/app/app.h\"\n", "#include app.h", "Include custom App cross-platform packager API", SuggestionKind.LIBRARY_API, 0, 0),
        AutocompleteSuggestion("InitWindow(1280, 720, \"Termux studio\");\nSetTargetFPS(60);\nwhile (!WindowShouldClose()) {\n    BeginDrawing();\n    ClearBackground(RAYWHITE);\n    DrawFPS(10, 10);\n    EndDrawing();\n}\nCloseWindow();", "raylib loop", "Raylib window & GPU frame loop", SuggestionKind.SNIPPET, 0, 0),
        AutocompleteSuggestion("AppConfig cfg = app_default_config(\"MyApp\");\napp_init(&cfg);", "app_init()", "Initialize App library & GPU drivers", SuggestionKind.LIBRARY_API, 0, 0),
        AutocompleteSuggestion("#ifndef HEADER_H\n#define HEADER_H\n\n#endif // HEADER_H", "#ifndef guard", "Classic C/C++ header include guard", SuggestionKind.SNIPPET, 0, 0),
        AutocompleteSuggestion("#pragma once\n", "#pragma once", "Single-inclusion header directive", SuggestionKind.SNIPPET, 0, 0),
        AutocompleteSuggestion("std::cout << \"\" << std::endl;", "std::cout", "Print formatted output to stdout", SuggestionKind.SNIPPET, 0, 0),
        AutocompleteSuggestion("printf(\"%s\\n\", );", "printf()", "C formatted stdout output", SuggestionKind.SNIPPET, 0, 0),
        AutocompleteSuggestion("void* handle = dlopen(\"./libplugin.so\", RTLD_LAZY);", "dlopen()", "Load a compiled .so shared object at runtime", SuggestionKind.LIBRARY_API, 0, 0)
    )

    private val stdNamespaces = listOf(
        "std::cout", "std::cin", "std::cerr", "std::endl", "std::string", "std::string_view",
        "std::vector", "std::map", "std::unordered_map", "std::set", "std::unique_ptr",
        "std::shared_ptr", "std::make_unique", "std::make_shared", "std::sort",
        "std::thread", "std::mutex", "std::lock_guard", "std::filesystem::path",
        "std::chrono::milliseconds", "std::optional", "std::variant", "std::span"
    )

    fun highlightCode(text: String, fileType: CodeFileType = CodeFileType.CPP_SOURCE): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        val allKeywords = cppKeywords + polyglotKeywords
        val hashIsComment = fileType == CodeFileType.BUILD_CONFIG ||
            fileType == CodeFileType.MAKEFILE ||
            fileType == CodeFileType.SHELL ||
            fileType == CodeFileType.NIM

        return buildAnnotatedString {
            append(text)
            addStyle(SpanStyle(color = FrostText), 0, text.length)

            var i = 0
            val n = text.length
            while (i < n) {
                val ch = text[i]
                when {
                    // Single-line // comment or ; assembly comment
                    (ch == '/' && i + 1 < n && text[i + 1] == '/') ||
                        (fileType == CodeFileType.ASSEMBLY && ch == ';') -> {
                        val end = text.indexOf('\n', i).let { if (it == -1) n else it }
                        addStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), i, end)
                        i = end
                    }
                    // # comment for build.config / Makefile / shell
                    ch == '#' && hashIsComment -> {
                        val end = text.indexOf('\n', i).let { if (it == -1) n else it }
                        addStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), i, end)
                        i = end
                    }
                    // Block comment
                    ch == '/' && i + 1 < n && text[i + 1] == '*' -> {
                        val close = text.indexOf("*/", i + 2)
                        val end = if (close == -1) n else close + 2
                        addStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), i, end)
                        i = end
                    }
                    // C/C++ Preprocessor line
                    ch == '#' && !hashIsComment && isLineStartWhitespaceOnly(text, i) -> {
                        val lineEnd = text.indexOf('\n', i).let { if (it == -1) n else it }
                        var wordEnd = i + 1
                        while (wordEnd < lineEnd && (text[wordEnd].isLetterOrDigit() || text[wordEnd] == '_')) {
                            wordEnd++
                        }
                        addStyle(SpanStyle(color = SyntaxPreprocessor, fontWeight = FontWeight.SemiBold), i, wordEnd)
                        val rest = text.substring(wordEnd, lineEnd)
                        val angleOpen = rest.indexOf('<')
                        val angleClose = rest.indexOf('>')
                        if (angleOpen != -1 && angleClose > angleOpen) {
                            addStyle(
                                SpanStyle(color = SyntaxHeaderPath),
                                wordEnd + angleOpen,
                                wordEnd + angleClose + 1
                            )
                        } else {
                            val qOpen = rest.indexOf('"')
                            val qClose = if (qOpen != -1) rest.indexOf('"', qOpen + 1) else -1
                            if (qOpen != -1 && qClose > qOpen) {
                                addStyle(
                                    SpanStyle(color = SyntaxHeaderPath),
                                    wordEnd + qOpen,
                                    wordEnd + qClose + 1
                                )
                            }
                        }
                        i = lineEnd
                    }
                    // String or char literal
                    ch == '"' || ch == '\'' -> {
                        val quote = ch
                        var j = i + 1
                        while (j < n) {
                            if (text[j] == '\\' && j + 1 < n) {
                                j += 2
                            } else if (text[j] == quote || text[j] == '\n') {
                                if (text[j] == quote) j++
                                break
                            } else {
                                j++
                            }
                        }
                        addStyle(SpanStyle(color = SyntaxString), i, j)
                        i = j
                    }
                    // Numbers
                    ch.isDigit() && (i == 0 || !isIdentChar(text[i - 1])) -> {
                        var j = i
                        while (j < n && (text[j].isLetterOrDigit() || text[j] == '.' || text[j] == '\'')) {
                            j++
                        }
                        addStyle(SpanStyle(color = SyntaxNumber), i, j)
                        i = j
                    }
                    // Identifiers / keywords / types / functions
                    ch.isLetter() || ch == '_' -> {
                        var j = i + 1
                        while (j < n && isIdentChar(text[j])) j++
                        val word = text.substring(i, j)
                        when {
                            word in allKeywords -> addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Medium), i, j)
                            word in types -> addStyle(SpanStyle(color = SyntaxType), i, j)
                            j < n && text[j] == '(' -> addStyle(SpanStyle(color = SyntaxFunction), i, j)
                            fileType == CodeFileType.BUILD_CONFIG && j < n && text[j] == '=' -> {
                                addStyle(SpanStyle(color = SyntaxHeaderPath, fontWeight = FontWeight.SemiBold), i, j)
                            }
                        }
                        i = j
                    }
                    // Operators & braces
                    ch in "{}()[]<>+-*/%=&|!~^:;.," -> {
                        addStyle(SpanStyle(color = SyntaxOperator), i, i + 1)
                        i++
                    }
                    else -> i++
                }
            }
        }
    }

    fun highlightCppCode(text: String): AnnotatedString = highlightCode(text, CodeFileType.CPP_SOURCE)

    fun computeSuggestions(
        code: String,
        cursorOffset: Int,
        localHeaders: List<String>,
        localSymbols: List<String>,
        fileType: CodeFileType = CodeFileType.CPP_SOURCE
    ): List<AutocompleteSuggestion> {
        val safeCursor = cursorOffset.coerceIn(0, code.length)
        val lineStart = code.lastIndexOf('\n', (safeCursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val linePrefix = code.substring(lineStart, safeCursor)

        // Language-specific package/import/include helpers
        if (fileType == CodeFileType.JAVA || fileType == CodeFileType.KOTLIN) {
            val impMatch = Regex("""^\s*import\s+([A-Za-z0-9_.*]*)$""").find(linePrefix)
            if (impMatch != null) {
                val query = impMatch.groupValues[1]
                val replaceStart = safeCursor - query.length
                val pkgs = listOf(
                    "java.util.*", "java.io.*", "java.nio.file.*", "java.net.*",
                    "java.lang.Math", "java.util.concurrent.*"
                )
                return pkgs.filter { it.contains(query, ignoreCase = true) }.map { pkg ->
                    AutocompleteSuggestion(
                        insertText = if (fileType == CodeFileType.JAVA) "$pkg;" else pkg,
                        displayLabel = pkg,
                        detail = "Standard Java/JVM Package",
                        kind = SuggestionKind.LIBRARY_API,
                        replaceStart = replaceStart,
                        replaceEnd = safeCursor
                    )
                }
            }
        }

        // 1. Check #include <...> or #include "..."
        val includeAngleMatch = Regex("""^\s*#\s*include\s*<([^>\n]*)$""").find(linePrefix)
        if (includeAngleMatch != null) {
            val query = includeAngleMatch.groupValues[1]
            val replaceStart = safeCursor - query.length
            return standardHeaders
                .filter { it.header.contains(query, ignoreCase = true) }
                .take(12)
                .map { doc ->
                    val suffix = if (safeCursor < code.length && code[safeCursor] == '>') "" else ">"
                    AutocompleteSuggestion(
                        insertText = "${doc.header}$suffix",
                        displayLabel = "<${doc.header}>",
                        detail = "${doc.language} • ${doc.summary}",
                        kind = SuggestionKind.STD_HEADER,
                        replaceStart = replaceStart,
                        replaceEnd = safeCursor
                    )
                }
        }

        val includeQuoteMatch = Regex("""^\s*#\s*include\s*"([^"\n]*)$""").find(linePrefix)
        if (includeQuoteMatch != null) {
            val query = includeQuoteMatch.groupValues[1]
            val replaceStart = safeCursor - query.length
            val allLocal = (localHeaders + listOf(
                "libs/raylib/include/raylib.h",
                "libs/raylib/include/raymath.h",
                "libs/raylib/include/rlgl.h",
                "libs/app/app.h"
            )).distinct()
            return allLocal
                .filter { it.contains(query, ignoreCase = true) }
                .take(12)
                .map { hdr ->
                    val suffix = if (safeCursor < code.length && code[safeCursor] == '"') "" else "\""
                    AutocompleteSuggestion(
                        insertText = "$hdr$suffix",
                        displayLabel = "\"$hdr\"",
                        detail = if (hdr.startsWith("libs/")) "Built-in Termux studio library" else "Workspace header",
                        kind = SuggestionKind.LOCAL_HEADER,
                        replaceStart = replaceStart,
                        replaceEnd = safeCursor
                    )
                }
        }

        // 2. Check #inc preprocessor typing
        if (linePrefix.trimStart().startsWith("#")) {
            val token = linePrefix.trimStart()
            val replaceStart = safeCursor - token.length
            val directives = listOf(
                "#include <" to "Include standard library header",
                "#include \"" to "Include local or built-in library header",
                "#define " to "Define preprocessor macro",
                "#pragma once" to "Header single-include guard",
                "#ifdef " to "Conditional compilation block",
                "#ifndef " to "Negated conditional compilation",
                "#endif" to "Close preprocessor conditional"
            )
            return directives
                .filter { it.first.startsWith(token, ignoreCase = true) }
                .map { (dir, desc) ->
                    AutocompleteSuggestion(
                        insertText = dir,
                        displayLabel = dir.trim(),
                        detail = desc,
                        kind = SuggestionKind.KEYWORD,
                        replaceStart = replaceStart,
                        replaceEnd = safeCursor
                    )
                }
        }

        // 3. Check std:: or identifier token
        val tokenMatch = Regex("""([A-Za-z_][A-Za-z0-9_:]*)$""").find(linePrefix) ?: return emptyList()
        val token = tokenMatch.groupValues[1]
        if (token.length < 2 && !token.contains("::")) return emptyList()
        val replaceStart = safeCursor - token.length

        val results = mutableListOf<AutocompleteSuggestion>()

        if (token.startsWith("std", ignoreCase = true)) {
            stdNamespaces
                .filter { it.contains(token, ignoreCase = true) }
                .forEach { sym ->
                    results.add(
                        AutocompleteSuggestion(
                            insertText = sym,
                            displayLabel = sym,
                            detail = "C++ Standard Library",
                            kind = SuggestionKind.LIBRARY_API,
                            replaceStart = replaceStart,
                            replaceEnd = safeCursor
                        )
                    )
                }
        }

        localSymbols
            .filter { it.contains(token, ignoreCase = true) }
            .distinct()
            .take(6)
            .forEach { sym ->
                results.add(
                    AutocompleteSuggestion(
                        insertText = sym,
                        displayLabel = sym,
                        detail = "Header / Project symbol",
                        kind = SuggestionKind.SYMBOL,
                        replaceStart = replaceStart,
                        replaceEnd = safeCursor
                    )
                )
            }

        standardHeaders
            .flatMap { hdr -> hdr.commonSymbols.map { it to hdr.header } }
            .filter { (sym, _) -> sym.contains(token, ignoreCase = true) }
            .distinctBy { it.first }
            .take(6)
            .forEach { (sym, hdr) ->
                results.add(
                    AutocompleteSuggestion(
                        insertText = sym,
                        displayLabel = sym,
                        detail = "From <$hdr>",
                        kind = SuggestionKind.LIBRARY_API,
                        replaceStart = replaceStart,
                        replaceEnd = safeCursor
                    )
                )
            }

        snippets
            .filter { it.displayLabel.contains(token, ignoreCase = true) }
            .forEach { snp ->
                results.add(snp.copy(replaceStart = replaceStart, replaceEnd = safeCursor))
            }

        (cppKeywords + polyglotKeywords + types)
            .filter { it.startsWith(token, ignoreCase = true) && it != token }
            .sorted()
            .take(6)
            .forEach { kw ->
                results.add(
                    AutocompleteSuggestion(
                        insertText = kw,
                        displayLabel = kw,
                        detail = if (kw in types) "Type" else "Keyword",
                        kind = SuggestionKind.KEYWORD,
                        replaceStart = replaceStart,
                        replaceEnd = safeCursor
                    )
                )
            }

        return results.distinctBy { it.displayLabel }.take(12)
    }

    fun parseCompilerDiagnostics(output: String): List<CompilerDiagnostic> {
        if (output.isBlank()) return emptyList()
        val gccRegex = Regex("""^([^\s:][^:\n]*):(\d+):(\d+):\s*(fatal error|error|warning|note):\s*(.+)$""")
        val javacRegex = Regex("""^([^\s:][^:\n]*):(\d+):\s*(error|warning):\s*(.+)$""")
        return output.lines().mapNotNull { line ->
            val trimmed = line.trim()
            gccRegex.find(trimmed)?.let { m ->
                return@mapNotNull CompilerDiagnostic(
                    fileName = m.groupValues[1].substringAfterLast('/'),
                    line = m.groupValues[2].toIntOrNull() ?: 1,
                    column = m.groupValues[3].toIntOrNull() ?: 1,
                    severity = m.groupValues[4],
                    message = m.groupValues[5]
                )
            }
            javacRegex.find(trimmed)?.let { m ->
                return@mapNotNull CompilerDiagnostic(
                    fileName = m.groupValues[1].substringAfterLast('/'),
                    line = m.groupValues[2].toIntOrNull() ?: 1,
                    column = 1,
                    severity = m.groupValues[3],
                    message = m.groupValues[4]
                )
            }
            null
        }.distinct()
    }

    private fun isIdentChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

    private fun isLineStartWhitespaceOnly(text: String, index: Int): Boolean {
        var k = index - 1
        while (k >= 0 && text[k] != '\n') {
            if (!text[k].isWhitespace()) return false
            k--
        }
        return true
    }
}
