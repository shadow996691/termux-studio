package com.example.data.model

enum class CodeFileType(val badge: String, val isEditableText: Boolean, val languageName: String) {
    C_SOURCE("C", true, "C"),
    CPP_SOURCE("C++", true, "C++"),
    HEADER("H", true, "C/C++ Header"),
    JAVA("JAVA", true, "Java"),
    KOTLIN("KT", true, "Kotlin"),
    RUST("RS", true, "Rust"),
    GO("GO", true, "Go"),
    ZIG("ZIG", true, "Zig"),
    ASSEMBLY("ASM", true, "Assembly"),
    FORTRAN("F90", true, "Fortran"),
    D_LANG("D", true, "D"),
    NIM("NIM", true, "Nim"),
    PASCAL("PAS", true, "Pascal"),
    CSHARP("C#", true, "C#"),
    SHELL("SH", true, "Shell"),
    MAKEFILE("MK", true, "Makefile"),
    BUILD_CONFIG("CFG", true, "build.config"),
    SHARED_OBJECT(".so", false, "Shared Object"),
    STATIC_ARCHIVE(".a", false, "Static Archive"),
    OBJECT_FILE(".o", false, "Object File"),
    PACKAGE_ARTIFACT("PKG", false, "App Package"),
    OTHER("FILE", true, "Text"),
    DIRECTORY("DIR", false, "Directory");

    companion object {
        fun fromFileName(name: String, isDirectory: Boolean): CodeFileType {
            if (isDirectory) return DIRECTORY
            val lower = name.lowercase()
            return when {
                lower.endsWith(".c") -> C_SOURCE
                lower.endsWith(".cpp") || lower.endsWith(".cc") || lower.endsWith(".cxx") -> CPP_SOURCE
                lower.endsWith(".h") || lower.endsWith(".hpp") || lower.endsWith(".hh") || lower.endsWith(".hxx") -> HEADER
                lower.endsWith(".java") -> JAVA
                lower.endsWith(".kt") || lower.endsWith(".kts") -> KOTLIN
                lower.endsWith(".rs") -> RUST
                lower.endsWith(".go") -> GO
                lower.endsWith(".zig") -> ZIG
                lower.endsWith(".s") || lower.endsWith(".asm") -> ASSEMBLY
                lower.endsWith(".f90") || lower.endsWith(".f95") || lower.endsWith(".f") -> FORTRAN
                lower.endsWith(".d") -> D_LANG
                lower.endsWith(".nim") -> NIM
                lower.endsWith(".pas") -> PASCAL
                lower.endsWith(".cs") -> CSHARP
                lower.endsWith(".sh") -> SHELL
                lower == "build.config" || lower.endsWith(".config") -> BUILD_CONFIG
                lower == "makefile" || lower == "cmakelists.txt" || lower.endsWith(".mk") -> MAKEFILE
                lower.endsWith(".so") || ".so." in lower -> SHARED_OBJECT
                lower.endsWith(".a") -> STATIC_ARCHIVE
                lower.endsWith(".o") || lower.endsWith(".class") -> OBJECT_FILE
                lower.endsWith(".apk") || lower.endsWith(".exe") || lower.endsWith(".deb") ||
                    lower.endsWith(".rpm") || lower.endsWith(".jar") || lower.endsWith(".app") -> PACKAGE_ARTIFACT
                else -> OTHER
            }
        }
    }
}

data class WorkspaceNode(
    val uriString: String,
    val name: String,
    val relativePath: String,
    val termuxPath: String,
    val fileType: CodeFileType,
    val sizeBytes: Long = 0L,
    val lastModified: Long = 0L,
    val isDirectory: Boolean = false,
    val depth: Int = 0
)

data class OpenEditorTab(
    val uriString: String,
    val name: String,
    val relativePath: String,
    val termuxPath: String,
    val fileType: CodeFileType,
    val content: String,
    val savedContent: String,
    val cursorOffset: Int = 0
) {
    val isDirty: Boolean get() = content != savedContent
}

enum class CompilerChoice(
    val label: String,
    val cBin: String,
    val cppBin: String,
    val languageHint: String = "C/C++"
) {
    AUTO("Auto (Clang/GCC)", "clang", "clang++", "C/C++"),
    CLANG("Clang (C)", "clang", "clang++", "C"),
    CLANG_CPP("Clang++ (C++)", "clang", "clang++", "C++"),
    GCC("G / GCC (C)", "gcc", "g++", "C"),
    GPP("G++ (C++)", "gcc", "g++", "C++"),
    OPENJDK("OpenJDK (javac/java)", "javac", "javac", "Java"),
    RUST("Rust (rustc)", "rustc", "rustc", "Rust"),
    GO("Go (go)", "go", "go", "Go"),
    ZIG("Zig (zig)", "zig", "zig", "Zig"),
    NASM("NASM / ld", "nasm", "nasm", "Assembly"),
    GFORTRAN("GFortran", "gfortran", "gfortran", "Fortran"),
    KOTLIN("Kotlin (kotlinc)", "kotlinc", "kotlinc", "Kotlin")
}

data class TermuxCompilerSpec(
    val id: String,
    val displayName: String,
    val binaryName: String,
    val termuxPkg: String,
    val description: String,
    val compilerChoice: CompilerChoice? = null
)

data class CompilerProbeStatus(
    val isInstalled: Boolean = false,
    val versionLine: String = ""
)

data class FolderLibraryItem(
    val id: String,
    val name: String,
    val relativePath: String,
    val includeDirs: List<String>,
    val libDirs: List<String>,
    val binaryFiles: List<String>,
    val headerFiles: List<String>,
    val suggestedFlags: String,
    val isBuiltIn: Boolean = false
)

enum class OutputTargetType(val label: String, val extension: String) {
    EXECUTABLE("Executable", ""),
    SHARED_SO("Shared Lib (.so)", ".so"),
    STATIC_A("Static Lib (.a)", ".a")
}

enum class AppPlatformTarget(val cliArg: String, val label: String, val packageExt: String) {
    ANDROID("Android", "Android", ".apk"),
    LINUX("Linux", "Linux", ".deb / .rpm / AppImage"),
    WINDOW("Window", "Window", ".exe"),
    MACOS("macOS", "macOS", ".app bundle")
}

enum class LinuxDistroOption(val id: String, val label: String, val pkgExt: String) {
    DEBIAN("debian", "Debian / Ubuntu (.deb)", ".deb"),
    FEDORA("fedora", "Fedora / RHEL (.rpm)", ".rpm"),
    ARCH("arch", "Arch Linux (.pkg.tar.zst)", ".pkg.tar.zst"),
    APPIMAGE("appimage", "Universal AppImage", ".AppImage")
}

enum class SuggestionKind(val tag: String) {
    STD_HEADER("STD"),
    LOCAL_HEADER("HDR"),
    KEYWORD("KW"),
    SNIPPET("SNP"),
    SYMBOL("SYM"),
    LIBRARY_API("LIB")
}

data class AutocompleteSuggestion(
    val insertText: String,
    val displayLabel: String,
    val detail: String,
    val kind: SuggestionKind,
    val replaceStart: Int,
    val replaceEnd: Int
)

enum class JobStatus {
    IDLE,
    RUNNING,
    SUCCEEDED,
    FAILED
}

data class TerminalEntry(
    val id: Long,
    val timestampLabel: String,
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int?,
    val isRunning: Boolean = false,
    val internalError: String? = null
)

data class CompilerDiagnostic(
    val fileName: String,
    val line: Int,
    val column: Int,
    val severity: String,
    val message: String
)

data class GitFileChange(
    val statusCode: String,
    val filePath: String
) {
    val statusLabel: String
        get() = when {
            statusCode.contains("?") -> "Untracked"
            statusCode.contains("M") -> "Modified"
            statusCode.contains("A") -> "Added"
            statusCode.contains("D") -> "Deleted"
            statusCode.contains("R") -> "Renamed"
            else -> statusCode.trim()
        }
}

data class GitRepoState(
    val isGitRepo: Boolean = false,
    val branch: String = "main",
    val remoteUrl: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val changes: List<GitFileChange> = emptyList(),
    val diffPreview: String = "",
    val statusMessage: String = "Tap Status to query git via Termux."
)

data class TermuxEnvironmentState(
    val isTermuxInstalled: Boolean = false,
    val isTermuxApiInstalled: Boolean = false,
    val isTermuxX11Installed: Boolean = false,
    val hasRunCommandPermission: Boolean = false,
    val termuxVersionName: String? = null,
    val connectedSafTreeUri: String? = null,
    val resolvedTermuxWorkdir: String = "/storage/emulated/0/TermuxStudio",
    val isSharedStorageGranted: Boolean = false,
    val isTermuxStudioPkgInstalled: Boolean = false,
    val isTermuxListenerActive: Boolean = false
)

data class GpuDriverInfo(
    val vendor: String = "Probing...",
    val renderer: String = "OpenGL ES / Vulkan",
    val glesVersion: String = "OpenGL ES 3.0",
    val hasVulkan: Boolean = false,
    val vulkanVersion: String = "Checking...",
    val recommendedTermuxDriver: String = "Turnip / Mesa Zink"
)

data class PermissionItemState(
    val id: String,
    val title: String,
    val description: String,
    val permissionKey: String,
    val isGranted: Boolean,
    val category: String
)

data class StandardHeaderDoc(
    val header: String,
    val language: String,
    val summary: String,
    val commonSymbols: List<String>
)

data class SystemLibrarySpec(
    val flag: String,
    val name: String,
    val headerHint: String,
    val description: String,
    val termuxPkg: String
)

enum class StudioThemePreset(
    val label: String,
    val backgroundHex: Long,
    val surfaceHex: Long,
    val elevatedHex: Long,
    val highHex: Long,
    val borderHex: Long,
    val textHex: Long,
    val mutedTextHex: Long,
    val gutterHex: Long
) {
    DEEP_OBSIDIAN(
        label = "Deep Obsidian",
        backgroundHex = 0xFF0B0E14,
        surfaceHex = 0xFF121722,
        elevatedHex = 0xFF1A2130,
        highHex = 0xFF222B3D,
        borderHex = 0xFF2B364D,
        textHex = 0xFFE6EDF3,
        mutedTextHex = 0xFF8B949E,
        gutterHex = 0xFF566070
    ),
    MIDNIGHT_NORD(
        label = "Midnight Nord",
        backgroundHex = 0xFF1A1F29,
        surfaceHex = 0xFF222834,
        elevatedHex = 0xFF2B3342,
        highHex = 0xFF343E50,
        borderHex = 0xFF404C61,
        textHex = 0xFFECEFF4,
        mutedTextHex = 0xFF9AA5B4,
        gutterHex = 0xFF627084
    ),
    DRACULA_SLATE(
        label = "Dracula Slate",
        backgroundHex = 0xFF191A21,
        surfaceHex = 0xFF21222C,
        elevatedHex = 0xFF2A2C39,
        highHex = 0xFF343746,
        borderHex = 0xFF44475A,
        textHex = 0xFFF8F8F2,
        mutedTextHex = 0xFF9BA3C4,
        gutterHex = 0xFF6272A4
    ),
    SOLARIZED_DARK(
        label = "Solarized Dark",
        backgroundHex = 0xFF00212B,
        surfaceHex = 0xFF072B36,
        elevatedHex = 0xFF0D3642,
        highHex = 0xFF14424F,
        borderHex = 0xFF205363,
        textHex = 0xFFEBE4D1,
        mutedTextHex = 0xFF93A1A1,
        gutterHex = 0xFF586E75
    ),
    TERMINAL_MATRIX(
        label = "Terminal Matrix",
        backgroundHex = 0xFF060C08,
        surfaceHex = 0xFF0D1710,
        elevatedHex = 0xFF132218,
        highHex = 0xFF1A2D20,
        borderHex = 0xFF25422E,
        textHex = 0xFFE0F7E6,
        mutedTextHex = 0xFF7FA88C,
        gutterHex = 0xFF4A7157
    )
}

enum class AccentPreset(
    val label: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val tertiaryHex: Long,
    val containerHex: Long
) {
    ELECTRIC_CYAN(
        label = "Electric Cyan",
        primaryHex = 0xFF38BDF8,
        secondaryHex = 0xFF34D399,
        tertiaryHex = 0xFFA78BFA,
        containerHex = 0xFF0F2D42
    ),
    MINT_EMERALD(
        label = "Mint Emerald",
        primaryHex = 0xFF34D399,
        secondaryHex = 0xFF38BDF8,
        tertiaryHex = 0xFFFBBF24,
        containerHex = 0xFF0E3026
    ),
    WARM_AMBER(
        label = "Warm Amber",
        primaryHex = 0xFFFBBF24,
        secondaryHex = 0xFF34D399,
        tertiaryHex = 0xFFFB923C,
        containerHex = 0xFF382809
    ),
    SOFT_VIOLET(
        label = "Soft Violet",
        primaryHex = 0xFFA78BFA,
        secondaryHex = 0xFF38BDF8,
        tertiaryHex = 0xFFF472B6,
        containerHex = 0xFF281C46
    ),
    NEON_CORAL(
        label = "Neon Coral",
        primaryHex = 0xFFFB7185,
        secondaryHex = 0xFF38BDF8,
        tertiaryHex = 0xFFFBBF24,
        containerHex = 0xFF3E1520
    )
}

data class IdeLanguageSpec(
    val id: String,
    val displayName: String,
    val defaultExt: String,
    val defaultCompiler: CompilerChoice
)

