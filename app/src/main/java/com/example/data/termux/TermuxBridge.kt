package com.example.data.termux

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.content.ContextCompat
import com.example.data.gpu.GpuDriverDetector
import com.example.data.local.CdnLibraryEntity
import com.example.data.model.CompilerChoice
import com.example.data.model.TermuxCompilerSpec
import com.example.data.model.TermuxEnvironmentState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger

data class TermuxCommandResult(
    val executionId: Int,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val err: Int = 0,
    val errmsg: String? = null
)

object TermuxExecutionBus {
    private val _results = MutableSharedFlow<TermuxCommandResult>(extraBufferCapacity = 32)
    val results: SharedFlow<TermuxCommandResult> = _results.asSharedFlow()

    private val _activeExecutionIds = MutableStateFlow<Set<Int>>(emptySet())
    val activeExecutionIds: StateFlow<Set<Int>> = _activeExecutionIds.asStateFlow()

    private val _lastStatusText = MutableStateFlow("Ready")
    val lastStatusText: StateFlow<String> = _lastStatusText.asStateFlow()

    fun markStarted(executionId: Int, label: String) {
        _activeExecutionIds.update { it + executionId }
        _lastStatusText.value = label
    }

    fun emitResult(result: TermuxCommandResult) {
        _activeExecutionIds.update { it - result.executionId }
        _lastStatusText.value = if (result.exitCode == 0 && result.err == 0) {
            "Completed (exit 0)"
        } else {
            "Exited with code ${result.exitCode}"
        }
        _results.tryEmit(result)
    }
}

object TermuxBridge {
    const val TERMUX_PACKAGE = "com.termux"
    const val TERMUX_API_PACKAGE = "com.termux.api"
    const val TERMUX_X11_PACKAGE = "com.termux.x11"
    const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"
    const val PERMISSION_RUN_COMMAND = "com.termux.permission.RUN_COMMAND"

    const val TERMUX_HOME_PATH = "/data/data/com.termux/files/home"
    const val TERMUX_STUDIO_HOME = "/data/data/com.termux/files/home/.termux_studio"
    const val TERMUX_BASH_PATH = "/data/data/com.termux/files/usr/bin/bash"

    const val FDROID_TERMUX_URL = "https://f-droid.org/en/packages/com.termux/"
    const val FDROID_TERMUX_API_URL = "https://f-droid.org/en/packages/com.termux.api/"
    const val TERMUX_X11_URL = "https://github.com/termux/termux-x11/releases"

    const val BOOTSTRAP_SETUP_COMMAND =
        "termux-setup-storage && pkg update -y && pkg install -y clang gcc git make curl wget unzip tar && " +
            "pkg install -y openjdk-17 vulkan-loader-android mesa-zink ; " +
            "ln -sfn /storage/emulated/0/TermuxStudio ~/TermuxStudio && " +
            "mkdir -p ~/.termux && " +
            "grep -q 'allow-external-apps' ~/.termux/termux.properties 2>/dev/null || " +
            "echo 'allow-external-apps=true' >> ~/.termux/termux.properties"

    val compilerCatalog: List<TermuxCompilerSpec> = listOf(
        TermuxCompilerSpec("clang", "Clang (C Compiler)", "clang", "clang", "LLVM C11/C17/C23 compiler", CompilerChoice.CLANG),
        TermuxCompilerSpec("clangpp", "Clang++ (C++ Compiler)", "clang++", "clang", "LLVM C++17/C++20/C++23 compiler", CompilerChoice.CLANG_CPP),
        TermuxCompilerSpec("gcc", "GCC / G (C Compiler)", "gcc", "gcc", "GNU C compiler (via Termux CCTools/gcc)", CompilerChoice.GCC),
        TermuxCompilerSpec("gpp", "G++ (GNU C++ Compiler)", "g++", "gcc", "GNU C++ compiler with libstdc++", CompilerChoice.GPP),
        TermuxCompilerSpec("javac", "OpenJDK (Java 17)", "javac", "openjdk-17", "javac + java JVM compiler and runtime", CompilerChoice.OPENJDK),
        TermuxCompilerSpec("rustc", "Rust (rustc + cargo)", "rustc", "rust", "Systems compiler with memory safety", CompilerChoice.RUST),
        TermuxCompilerSpec("go", "Go (golang)", "go", "golang", "Fast natively compiled systems language", CompilerChoice.GO),
        TermuxCompilerSpec("zig", "Zig Compiler", "zig", "zig", "Modern C-ABI & cross-compiling toolchain", CompilerChoice.ZIG),
        TermuxCompilerSpec("nasm", "NASM Assembler", "nasm", "nasm", "Assembly compiler & ELF object builder", CompilerChoice.NASM),
        TermuxCompilerSpec("gfortran", "GFortran", "gfortran", "gcc-gfortran", "GNU Fortran scientific compiler", CompilerChoice.GFORTRAN),
        TermuxCompilerSpec("kotlinc", "Kotlin Compiler", "kotlinc", "kotlin", "JVM Kotlin compiler", CompilerChoice.KOTLIN),
        TermuxCompilerSpec("make", "Make & CMake Build Tools", "make", "make cmake", "Native build automation for C/C++ libs", null)
    )

    private val idCounter = AtomicInteger(100)

    fun detectEnvironment(
        context: Context,
        connectedSafTreeUri: String?,
        resolvedWorkdir: String,
        isSharedStorageGranted: Boolean = false,
        isTermuxStudioPkgInstalled: Boolean = false,
        isListenerActive: Boolean = false
    ): TermuxEnvironmentState {
        val pm = context.packageManager
        val termuxInfo = getPackageInfoSafe(pm, TERMUX_PACKAGE)
        val termuxApiInfo = getPackageInfoSafe(pm, TERMUX_API_PACKAGE)
        val termuxX11Info = getPackageInfoSafe(pm, TERMUX_X11_PACKAGE)
        val hasPerm = ContextCompat.checkSelfPermission(context, PERMISSION_RUN_COMMAND) ==
            PackageManager.PERMISSION_GRANTED

        return TermuxEnvironmentState(
            isTermuxInstalled = termuxInfo != null,
            isTermuxApiInstalled = termuxApiInfo != null,
            isTermuxX11Installed = termuxX11Info != null,
            hasRunCommandPermission = hasPerm,
            termuxVersionName = termuxInfo?.versionName,
            connectedSafTreeUri = connectedSafTreeUri,
            resolvedTermuxWorkdir = resolvedWorkdir,
            isSharedStorageGranted = isSharedStorageGranted,
            isTermuxStudioPkgInstalled = isTermuxStudioPkgInstalled,
            isTermuxListenerActive = isListenerActive
        )
    }

    private fun getPackageInfoSafe(pm: PackageManager, pkg: String) = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0)
        }
    } catch (_: Exception) {
        null
    }

    fun openTermuxFDroidPage(context: Context, url: String = FDROID_TERMUX_URL) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun launchTermuxApp(context: Context): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        return true
    }

    fun buildGitHubAptInstallCommand(repoUrl: String, sharedDir: String): String {
        val cleanUrl = repoUrl.trim().removeSuffix("/")
        return "echo \"deb [trusted=yes] $cleanUrl stable main\" > \"\$PREFIX/etc/apt/sources.list.d/termux-studio.list\" && " +
            "pkg update -y && pkg install -y termux-studio && " +
            "termux-studio link-storage \"$sharedDir\" && termux-studio probe"
    }

    fun buildLocalDebInstallCommand(sharedDir: String): String {
        val debPath = "$sharedDir/termux-repo/pool/main/t/termux-studio/${TermuxAptRepoBuilder.DEB_FILE_NAME}"
        return "pkg install -y \"$debPath\" && termux-studio link-storage \"$sharedDir\" && termux-studio probe"
    }

    fun buildBundledLibsInstallPrefix(context: Context, sharedDir: String): String {
        val assets = context.assets
        fun b64(assetPath: String): String {
            val bytes = assets.open(assetPath).readBytes()
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
        val studioCliB64 = b64("termux_pkg/termux-studio")
        val appHB64 = b64("libs/app/app.h")
        val appCB64 = b64("libs/app/app_runtime.c")
        val appCliB64 = b64("libs/app/app_cli.sh")

        return "export TERMUX_STUDIO_SHARED=\"$sharedDir\" && " +
            "mkdir -p \"$TERMUX_STUDIO_HOME/bin\" \"$TERMUX_STUDIO_HOME/include/app\" && " +
            "echo '$studioCliB64' | base64 -d > \"$TERMUX_STUDIO_HOME/bin/termux-studio\" && " +
            "echo '$appCliB64' | base64 -d > \"$TERMUX_STUDIO_HOME/bin/app\" && " +
            "echo '$appHB64' | base64 -d > \"$TERMUX_STUDIO_HOME/include/app/app.h\" && " +
            "echo '$appCB64' | base64 -d > \"$TERMUX_STUDIO_HOME/include/app/app_runtime.c\" && " +
            "chmod +x \"$TERMUX_STUDIO_HOME/bin/termux-studio\" \"$TERMUX_STUDIO_HOME/bin/app\" && " +
            "if [ -d \"$sharedDir\" ] && [ ! -e \"\$HOME/TermuxStudio\" ]; then ln -sfn \"$sharedDir\" \"\$HOME/TermuxStudio\" 2>/dev/null || true; fi && " +
            "export PATH=\"\$PREFIX/bin:$TERMUX_STUDIO_HOME/bin:\$PATH\""
    }

    fun buildCdnResolvePrefix(workdir: String, enabledCdnLibs: List<CdnLibraryEntity>): String {
        if (enabledCdnLibs.isEmpty()) {
            return "rm -f \"$workdir/.termux-ipc/cdn_flags.env\""
        }
        val args = enabledCdnLibs.joinToString(" ") { lib ->
            val safeName = lib.name.replace("\"", "")
            val safeUrl = lib.url.replace("\"", "")
            "\"$safeName::$safeUrl\""
        }
        return "termux-studio cdn-resolve \"$workdir\" $args"
    }

    fun buildTermuxLibFetchCommand(sharedDir: String, tool: String, libName: String, target: String): String {
        val safeTool = tool.lowercase().trim()
        val safeName = libName.trim().replace("\"", "")
        val safeTarget = target.trim().replace("\"", "")
        return "termux-studio lib-fetch \"$sharedDir\" \"$safeTool\" \"$safeName\" \"$safeTarget\""
    }

    fun wrapCompileWithTermuxStudio(workdir: String, command: String, runBinaryName: String?): String {
        val b64 = Base64.encodeToString(command.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val binArg = if (!runBinaryName.isNullOrBlank()) " \"$runBinaryName\"" else ""
        return "termux-studio compile \"$workdir\" \"$b64\"$binArg"
    }

    fun dispatchBashCommand(
        context: Context,
        script: String,
        workdir: String = "/storage/emulated/0/TermuxStudio",
        sharedDir: String = workdir,
        label: String = "Running Termux task",
        keepAliveService: Boolean = true,
        injectGpuDrivers: Boolean = true
    ): Result<Int> {
        val env = detectEnvironment(context, null, workdir)
        if (!env.isTermuxInstalled) {
            return Result.failure(IllegalStateException("Termux is not installed. Download Termux from F-Droid first."))
        }
        if (!env.hasRunCommandPermission) {
            return Result.failure(SecurityException("Grant Termux RUN_COMMAND permission in the Permissions tab."))
        }

        val executionId = idCounter.incrementAndGet()
        TermuxExecutionBus.markStarted(executionId, label)

        if (keepAliveService) {
            BuildPersistenceService.startOrUpdate(context, label)
        }

        val libsPrefix = buildBundledLibsInstallPrefix(context, sharedDir)
        val gpuExports = if (injectGpuDrivers) GpuDriverDetector.buildTermuxGpuExports() else ""
        val fullScript = listOf(libsPrefix, gpuExports, script)
            .filter { it.isNotBlank() }
            .joinToString("; ")

        val resultIntent = Intent(context, TermuxResultService::class.java).apply {
            setPackage(context.packageName)
            putExtra("execution_id", executionId)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getService(context, executionId, resultIntent, flags)

        val safeWorkdir = if (workdir.isBlank()) TERMUX_HOME_PATH else workdir
        val termuxIntent = Intent().apply {
            setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
            action = ACTION_RUN_COMMAND
            putExtra(EXTRA_COMMAND_PATH, TERMUX_BASH_PATH)
            putExtra(EXTRA_ARGUMENTS, arrayOf("-lc", fullScript))
            putExtra(EXTRA_WORKDIR, safeWorkdir)
            putExtra(EXTRA_BACKGROUND, true)
            putExtra(EXTRA_PENDING_INTENT, pendingIntent)
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(termuxIntent)
            } else {
                context.startService(termuxIntent)
            }
            Result.success(executionId)
        } catch (e: Exception) {
            TermuxExecutionBus.emitResult(
                TermuxCommandResult(
                    executionId = executionId,
                    stdout = "",
                    stderr = "",
                    exitCode = -1,
                    err = -1,
                    errmsg = "Failed to invoke Termux RunCommandService: ${e.message}"
                )
            )
            Result.failure(e)
        }
    }
}
