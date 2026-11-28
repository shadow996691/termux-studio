package com.example.viewmodel

import android.Manifest
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.editor.CppLanguageEngine
import com.example.data.gpu.GpuDriverDetector
import com.example.data.local.BuildProfileEntity
import com.example.data.local.CdnLibraryEntity
import com.example.data.local.CommandHistoryEntity
import com.example.data.local.EditorPreferences
import com.example.data.local.HeaderCacheEntity
import com.example.data.local.IdeDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.model.AccentPreset
import com.example.data.model.AppPlatformTarget
import com.example.data.model.AutocompleteSuggestion
import com.example.data.model.CodeFileType
import com.example.data.model.CompilerChoice
import com.example.data.model.CompilerDiagnostic
import com.example.data.model.CompilerProbeStatus
import com.example.data.model.FolderLibraryItem
import com.example.data.model.GitFileChange
import com.example.data.model.GitRepoState
import com.example.data.model.GpuDriverInfo
import com.example.data.model.JobStatus
import com.example.data.model.LinuxDistroOption
import com.example.data.model.OpenEditorTab
import com.example.data.model.OutputTargetType
import com.example.data.model.PermissionItemState
import com.example.data.model.StudioThemePreset
import com.example.data.model.TerminalEntry
import com.example.data.model.TermuxEnvironmentState
import com.example.data.model.WorkspaceNode
import com.example.data.termux.BuildPersistenceService
import com.example.data.termux.TermuxAptRepoBuilder
import com.example.data.termux.TermuxBridge
import com.example.data.termux.TermuxCommandResult
import com.example.data.termux.TermuxExecutionBus
import com.example.data.workspace.OfflineLibImporter
import com.example.data.workspace.TermuxWorkspaceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

enum class IdeSection {
    EDITOR,
    COMPILER,
    TERMINAL,
    LIBRARIES,
    GIT,
    PERMISSIONS,
    SETTINGS
}

private enum class ExecutionPurpose {
    BUILD_RUN,
    TERMINAL,
    GIT_STATUS,
    GIT_DIFF,
    GIT_MUTATION,
    APP_CONFIG_BUILD,
    COMPILER_PROBE,
    PKG_INSTALL,
    LIB_FETCH
}

data class IdeUiState(
    val activeSection: IdeSection = IdeSection.EDITOR,
    val activeCompilerPanel: Int = 0,
    val termuxEnv: TermuxEnvironmentState = TermuxEnvironmentState(),
    val gpuDriverInfo: GpuDriverInfo = GpuDriverInfo(),
    val autoGpuDrivers: Boolean = true,
    val selectedAppPlatform: AppPlatformTarget = AppPlatformTarget.ANDROID,
    val selectedLinuxDistro: LinuxDistroOption = LinuxDistroOption.DEBIAN,
    val showTermuxMissingDialog: Boolean = false,
    val showEditorBuildDrawer: Boolean = false,
    val workspaceNodes: List<WorkspaceNode> = emptyList(),
    val folderLibraries: List<FolderLibraryItem> = emptyList(),
    val compilerProbeMap: Map<String, CompilerProbeStatus> = emptyMap(),
    val githubAptRepoUrl: String = "https://YOUR_GITHUB_USER.github.io/termux-studio-repo",
    val openTabs: List<OpenEditorTab> = emptyList(),
    val activeTabIndex: Int = -1,
    val autocompleteSuggestions: List<AutocompleteSuggestion> = emptyList(),
    val localHeaderPaths: List<String> = emptyList(),
    val localHeaderSymbols: List<String> = emptyList(),
    val buildProfile: BuildProfileEntity = BuildProfileEntity(projectKey = "default"),
    val terminalEntries: List<TerminalEntry> = emptyList(),
    val diagnostics: List<CompilerDiagnostic> = emptyList(),
    val jobStatus: JobStatus = JobStatus.IDLE,
    val gitState: GitRepoState = GitRepoState(),
    val permissions: List<PermissionItemState> = emptyList(),
    val editorFontSizeSp: Int = 13,
    val keepBackgroundAlive: Boolean = true,
    val themePreset: StudioThemePreset = StudioThemePreset.DEEP_OBSIDIAN,
    val accentPreset: AccentPreset = AccentPreset.ELECTRIC_CYAN,
    val indentWidth: Int = 4,
    val useTabs: Boolean = false,
    val autoIndent: Boolean = true,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val compactSuggestions: Boolean = true,
    val compilerOptLevel: String = "-O2",
    val defaultLanguage: String = "C++",
    val enabledLanguagesCsv: String = "C,C++,Zig,ASM,Rust,Go,Java,Kotlin",
    val snackbarMessage: String? = null
) {
    val activeTab: OpenEditorTab?
        get() = openTabs.getOrNull(activeTabIndex)
}

class IdeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = IdeDatabase.getInstance(context)
    private val dao = db.ideDao()
    private val prefsRepo = PreferencesRepository(context)
    private val workspaceManager = TermuxWorkspaceManager(context)
    private val offlineLibImporter = OfflineLibImporter(context)

    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState: StateFlow<IdeUiState> = _uiState.asStateFlow()

    val commandHistory: StateFlow<List<CommandHistoryEntity>> = dao.observeCommandHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cdnLibraries: StateFlow<List<CdnLibraryEntity>> = dao.observeCdnLibraries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val pendingPurposes = ConcurrentHashMap<Int, Pair<ExecutionPurpose, Long>>()
    private val undoStacks = ConcurrentHashMap<String, ArrayDeque<String>>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var terminalEntryCounter = 1L

    init {
        viewModelScope.launch {
            val prefs = prefsRepo.preferences.first()
            val gpuInfo = GpuDriverDetector.probeGpuDrivers(context)
            _uiState.update {
                it.copy(
                    editorFontSizeSp = prefs.editorFontSizeSp,
                    keepBackgroundAlive = prefs.keepBackgroundServiceAlive,
                    githubAptRepoUrl = prefs.githubAptRepoUrl,
                    themePreset = prefs.themePreset,
                    accentPreset = prefs.accentPreset,
                    indentWidth = prefs.indentWidth,
                    useTabs = prefs.useTabs,
                    autoIndent = prefs.autoIndent,
                    showLineNumbers = prefs.showLineNumbers,
                    wordWrap = prefs.wordWrap,
                    compactSuggestions = prefs.compactSuggestions,
                    compilerOptLevel = prefs.compilerOptLevel,
                    defaultLanguage = prefs.defaultLanguage,
                    enabledLanguagesCsv = prefs.enabledLanguagesCsv,
                    gpuDriverInfo = gpuInfo
                )
            }
            refreshEnvironmentAndPermissions(prefs)
            loadWorkspace(prefs.termuxTreeUri, prefs.lastOpenedFileUri)
            loadBuildProfile(prefs.termuxWorkdirPath)
        }
        observeTermuxResults()
    }

    private fun observeTermuxResults() {
        viewModelScope.launch {
            TermuxExecutionBus.results.collect { result ->
                handleTermuxResult(result)
            }
        }
    }

    fun selectSection(section: IdeSection) {
        _uiState.update { it.copy(activeSection = section) }
        if (section == IdeSection.PERMISSIONS) {
            refreshPermissionsOnly()
        } else if (section == IdeSection.COMPILER) {
            refreshIpcProbeFromDisk()
        }
    }

    fun selectCompilerPanel(index: Int) {
        _uiState.update { it.copy(activeCompilerPanel = index.coerceIn(0, 2)) }
    }

    fun dismissTermuxMissingDialog() {
        _uiState.update { it.copy(showTermuxMissingDialog = false) }
    }

    fun showTermuxSetupDialog() {
        _uiState.update { it.copy(showTermuxMissingDialog = true) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun setAutoGpuDrivers(enabled: Boolean) {
        _uiState.update { it.copy(autoGpuDrivers = enabled) }
    }

    fun selectAppPlatform(platform: AppPlatformTarget) {
        _uiState.update { it.copy(selectedAppPlatform = platform) }
    }

    fun selectLinuxDistro(distro: LinuxDistroOption) {
        _uiState.update { it.copy(selectedLinuxDistro = distro) }
    }

    fun updateGithubAptRepoUrl(url: String) {
        _uiState.update { it.copy(githubAptRepoUrl = url) }
        viewModelScope.launch { prefsRepo.setGithubAptRepoUrl(url) }
    }

    fun dismissEditorBuildDrawer() {
        _uiState.update { it.copy(showEditorBuildDrawer = false) }
    }

    fun openEditorBuildDrawer() {
        _uiState.update { it.copy(showEditorBuildDrawer = true) }
    }

    fun openTerminalFromBuildDrawer() {
        _uiState.update { it.copy(showEditorBuildDrawer = false, activeSection = IdeSection.TERMINAL) }
    }

    fun dismissAutocompleteSuggestions() {
        _uiState.update { it.copy(autocompleteSuggestions = emptyList()) }
    }

    fun updateThemePreset(preset: StudioThemePreset) {
        _uiState.update { it.copy(themePreset = preset) }
        viewModelScope.launch { prefsRepo.setThemePreset(preset) }
    }

    fun updateAccentPreset(preset: AccentPreset) {
        _uiState.update { it.copy(accentPreset = preset) }
        viewModelScope.launch { prefsRepo.setAccentPreset(preset) }
    }

    fun updateIndentWidth(width: Int) {
        val clamped = width.coerceIn(2, 8)
        _uiState.update { it.copy(indentWidth = clamped) }
        viewModelScope.launch { prefsRepo.setIndentWidth(clamped) }
    }

    fun setUseTabs(useTabs: Boolean) {
        _uiState.update { it.copy(useTabs = useTabs) }
        viewModelScope.launch { prefsRepo.setUseTabs(useTabs) }
    }

    fun setAutoIndent(enabled: Boolean) {
        _uiState.update { it.copy(autoIndent = enabled) }
        viewModelScope.launch { prefsRepo.setAutoIndent(enabled) }
    }

    fun setShowLineNumbers(show: Boolean) {
        _uiState.update { it.copy(showLineNumbers = show) }
        viewModelScope.launch { prefsRepo.setShowLineNumbers(show) }
    }

    fun setWordWrap(wrap: Boolean) {
        _uiState.update { it.copy(wordWrap = wrap) }
        viewModelScope.launch { prefsRepo.setWordWrap(wrap) }
    }

    fun setCompactSuggestions(compact: Boolean) {
        _uiState.update { it.copy(compactSuggestions = compact) }
        viewModelScope.launch { prefsRepo.setCompactSuggestions(compact) }
    }

    fun updateCompilerOptLevel(optLevel: String) {
        _uiState.update { it.copy(compilerOptLevel = optLevel) }
        viewModelScope.launch { prefsRepo.setCompilerOptLevel(optLevel) }
    }

    fun updateDefaultLanguage(language: String) {
        _uiState.update { it.copy(defaultLanguage = language) }
        viewModelScope.launch { prefsRepo.setDefaultLanguage(language) }
    }

    fun toggleEnabledLanguage(language: String) {
        val current = _uiState.value.enabledLanguagesCsv
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()
        if (language in current) {
            if (current.size > 1) current.remove(language)
        } else {
            current.add(language)
        }
        val nextCsv = current.joinToString(",")
        _uiState.update { it.copy(enabledLanguagesCsv = nextCsv) }
        viewModelScope.launch { prefsRepo.setEnabledLanguagesCsv(nextCsv) }
    }

    fun refreshEnvironment() {
        viewModelScope.launch {
            val prefs = prefsRepo.preferences.first()
            val prevShared = _uiState.value.termuxEnv.isSharedStorageGranted
            refreshEnvironmentAndPermissions(prefs)
            val nowShared = _uiState.value.termuxEnv.isSharedStorageGranted
            if (!prevShared && nowShared && prefs.termuxTreeUri == null) {
                loadWorkspace(null, null)
            }
        }
    }

    private fun refreshIpcProbeFromDisk() {
        val probeMap = workspaceManager.readIpcCompilerProbe()
        val pkgInstalled = probeMap["termux_studio"]?.isInstalled == true
        val listenerAlive = workspaceManager.isListenerAlive()
        _uiState.update { state ->
            state.copy(
                compilerProbeMap = probeMap,
                termuxEnv = state.termuxEnv.copy(
                    isTermuxStudioPkgInstalled = pkgInstalled || state.termuxEnv.isTermuxStudioPkgInstalled,
                    isTermuxListenerActive = listenerAlive || state.termuxEnv.isTermuxListenerActive
                )
            )
        }
    }

    private fun refreshEnvironmentAndPermissions(prefs: EditorPreferences) {
        val hasSharedStorage = workspaceManager.hasSharedStorageAccess()
        val resolvedWorkdir = if (prefs.termuxTreeUri != null) {
            prefs.termuxWorkdirPath
        } else {
            workspaceManager.getActiveLocalRoot().absolutePath
        }
        val probeMap = workspaceManager.readIpcCompilerProbe()
        val listenerAlive = workspaceManager.isListenerAlive()
        val env = TermuxBridge.detectEnvironment(
            context = context,
            connectedSafTreeUri = prefs.termuxTreeUri,
            resolvedWorkdir = resolvedWorkdir,
            isSharedStorageGranted = hasSharedStorage,
            isTermuxStudioPkgInstalled = probeMap["termux_studio"]?.isInstalled == true,
            isListenerActive = listenerAlive
        )
        _uiState.update {
            it.copy(
                termuxEnv = env,
                compilerProbeMap = probeMap,
                showTermuxMissingDialog = !env.isTermuxInstalled && it.terminalEntries.isEmpty()
            )
        }
        refreshPermissionsOnly()
    }

    fun refreshPermissionsOnly() {
        val current = _uiState.value.termuxEnv
        val storageAllFiles = workspaceManager.hasSharedStorageAccess()
        val env = TermuxBridge.detectEnvironment(
            context = context,
            connectedSafTreeUri = current.connectedSafTreeUri,
            resolvedWorkdir = if (current.connectedSafTreeUri != null) {
                current.resolvedTermuxWorkdir
            } else {
                workspaceManager.getActiveLocalRoot().absolutePath
            },
            isSharedStorageGranted = storageAllFiles,
            isTermuxStudioPkgInstalled = current.isTermuxStudioPkgInstalled,
            isListenerActive = current.isTermuxListenerActive
        )

        val items = buildList {
            add(
                PermissionItemState(
                    id = "all_files",
                    title = "Shared TermuxStudio Folder (/sdcard/TermuxStudio)",
                    description = "Share projects, Raylib 6.2, App, CDN libs & termux-repo directly between Termux studio and Termux ~/TermuxStudio",
                    permissionKey = "MANAGE_EXTERNAL_STORAGE",
                    isGranted = storageAllFiles,
                    category = "Termux Addon & Folder Sharing"
                )
            )
            add(
                PermissionItemState(
                    id = "termux_run",
                    title = "Termux RUN_COMMAND",
                    description = "Execute termux-studio, Clang, Clang++, GCC, G++, OpenJDK, Rust, Go & Git inside Termux",
                    permissionKey = TermuxBridge.PERMISSION_RUN_COMMAND,
                    isGranted = env.hasRunCommandPermission,
                    category = "Termux Addon & Folder Sharing"
                )
            )
            add(
                PermissionItemState(
                    id = "termux_saf",
                    title = "Optional Termux \$HOME SAF Tree",
                    description = "Browse private Termux \$HOME folders via DocumentsProvider when needed",
                    permissionKey = "SAF_TERMUX_TREE",
                    isGranted = env.connectedSafTreeUri != null,
                    category = "Termux Addon & Folder Sharing"
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    PermissionItemState(
                        id = "notifications",
                        title = "Background Build Notifications",
                        description = "Keep Termux build & listener persistence visible when minimized",
                        permissionKey = Manifest.permission.POST_NOTIFICATIONS,
                        isGranted = isGranted(Manifest.permission.POST_NOTIFICATIONS),
                        category = "Persistence & Network"
                    )
                )
            }
            add(
                PermissionItemState(
                    id = "internet",
                    title = "Internet & Network State",
                    description = "GitHub sync, APT repo, CDN library downloads & socket binaries",
                    permissionKey = Manifest.permission.INTERNET,
                    isGranted = isGranted(Manifest.permission.INTERNET),
                    category = "Persistence & Network"
                )
            )
            add(
                PermissionItemState(
                    id = "camera",
                    title = "Camera Hardware",
                    description = "Available for user vision binaries or Termux:API camera calls",
                    permissionKey = Manifest.permission.CAMERA,
                    isGranted = isGranted(Manifest.permission.CAMERA),
                    category = "Device Hardware"
                )
            )
            add(
                PermissionItemState(
                    id = "microphone",
                    title = "Microphone & Audio Capture",
                    description = "Available for user DSP, Raylib audio & speech binaries",
                    permissionKey = Manifest.permission.RECORD_AUDIO,
                    isGranted = isGranted(Manifest.permission.RECORD_AUDIO),
                    category = "Device Hardware"
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(
                    PermissionItemState(
                        id = "bluetooth_connect",
                        title = "Bluetooth Connect & Scan",
                        description = "Available for user RFCOMM/BLE hardware tools",
                        permissionKey = Manifest.permission.BLUETOOTH_CONNECT,
                        isGranted = isGranted(Manifest.permission.BLUETOOTH_CONNECT) &&
                            isGranted(Manifest.permission.BLUETOOTH_SCAN),
                        category = "Device Hardware"
                    )
                )
            } else {
                add(
                    PermissionItemState(
                        id = "bluetooth_legacy",
                        title = "Bluetooth Adapter",
                        description = "Available for user Bluetooth hardware tools",
                        permissionKey = Manifest.permission.BLUETOOTH,
                        isGranted = isGranted(Manifest.permission.BLUETOOTH),
                        category = "Device Hardware"
                    )
                )
            }
            add(
                PermissionItemState(
                    id = "location",
                    title = "Fine Location",
                    description = "Required on Android for BLE scanning & GPS Termux:API tools",
                    permissionKey = Manifest.permission.ACCESS_FINE_LOCATION,
                    isGranted = isGranted(Manifest.permission.ACCESS_FINE_LOCATION),
                    category = "Device Hardware"
                )
            )
        }

        _uiState.update { it.copy(termuxEnv = env, permissions = items) }
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun onTermuxFolderConnected(treeUri: Uri) {
        viewModelScope.launch {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
            } catch (_: Exception) {
            }
            val resolvedPath = workspaceManager.resolveTermuxWorkdirFromTreeUri(treeUri)
            prefsRepo.setTermuxWorkspace(treeUri.toString(), resolvedPath)
            val prefs = prefsRepo.preferences.first()
            refreshEnvironmentAndPermissions(prefs)
            loadWorkspace(treeUri.toString(), null)
            loadBuildProfile(resolvedPath)
            _uiState.update { it.copy(snackbarMessage = "Connected workspace: $resolvedPath") }
        }
    }

    fun disconnectTermuxFolder() {
        viewModelScope.launch {
            val sharedPath = workspaceManager.getActiveLocalRoot().absolutePath
            prefsRepo.setTermuxWorkspace(null, sharedPath)
            val prefs = prefsRepo.preferences.first()
            refreshEnvironmentAndPermissions(prefs)
            loadWorkspace(null, null)
            _uiState.update { it.copy(snackbarMessage = "Using shared folder: $sharedPath") }
        }
    }

    fun refreshWorkspace() {
        viewModelScope.launch {
            val treeUri = _uiState.value.termuxEnv.connectedSafTreeUri
            loadWorkspace(treeUri, _uiState.value.activeTab?.uriString)
        }
    }

    private suspend fun loadWorkspace(treeUriString: String?, preferredFileUri: String?) {
        val nodes = workspaceManager.listWorkspaceNodes(treeUriString)
        val folderLibs = workspaceManager.scanFolderLibraries(nodes)
        val headerNodes = nodes.filter { !it.isDirectory && it.fileType == CodeFileType.HEADER }
        val localHeaders = headerNodes.map { it.relativePath }
        val extractedSymbols = mutableSetOf<String>()
        val cacheEntries = mutableListOf<HeaderCacheEntity>()
        val projectKey = _uiState.value.termuxEnv.resolvedTermuxWorkdir

        headerNodes.take(30).forEach { hdr ->
            workspaceManager.readFileText(hdr.uriString).onSuccess { src ->
                val syms = workspaceManager.extractHeaderSymbols(src)
                extractedSymbols.addAll(syms)
                cacheEntries.add(
                    HeaderCacheEntity(
                        headerKey = "${projectKey}::${hdr.relativePath}",
                        projectKey = projectKey,
                        headerPath = hdr.relativePath,
                        symbolsJoined = syms.joinToString("\n")
                    )
                )
            }
        }
        if (cacheEntries.isNotEmpty()) {
            dao.upsertHeaderCache(cacheEntries)
        }

        _uiState.update {
            it.copy(
                workspaceNodes = nodes,
                folderLibraries = folderLibs,
                localHeaderPaths = localHeaders,
                localHeaderSymbols = extractedSymbols.sorted()
            )
        }

        if (_uiState.value.openTabs.isEmpty()) {
            val targetNode = nodes.firstOrNull { it.uriString == preferredFileUri && it.fileType.isEditableText }
                ?: nodes.firstOrNull { it.name == "main.cpp" || it.name == "main.c" }
                ?: nodes.firstOrNull { !it.isDirectory && it.fileType.isEditableText && !it.relativePath.startsWith("termux-repo/") }
            if (targetNode != null) {
                openNode(targetNode)
            }
        }
    }

    private suspend fun loadBuildProfile(projectKey: String) {
        val existing = dao.getBuildProfile(projectKey) ?: BuildProfileEntity(projectKey = projectKey)
        _uiState.update { it.copy(buildProfile = existing) }
    }

    fun openNode(node: WorkspaceNode) {
        if (node.isDirectory) return
        if (!node.fileType.isEditableText) {
            _uiState.update {
                it.copy(
                    snackbarMessage = "${node.name} (${node.fileType.badge}) is a binary/package artifact (${node.sizeBytes} bytes)."
                )
            }
            return
        }
        val existingIndex = _uiState.value.openTabs.indexOfFirst { it.uriString == node.uriString }
        if (existingIndex != -1) {
            _uiState.update {
                it.copy(
                    activeTabIndex = existingIndex,
                    activeSection = IdeSection.EDITOR,
                    autocompleteSuggestions = emptyList()
                )
            }
            return
        }

        viewModelScope.launch {
            workspaceManager.readFileText(node.uriString).onSuccess { text ->
                val tab = OpenEditorTab(
                    uriString = node.uriString,
                    name = node.name,
                    relativePath = node.relativePath,
                    termuxPath = node.termuxPath,
                    fileType = node.fileType,
                    content = text,
                    savedContent = text
                )
                prefsRepo.setLastOpenedFileUri(node.uriString)
                _uiState.update { state ->
                    val updated = state.openTabs + tab
                    state.copy(
                        openTabs = updated,
                        activeTabIndex = updated.lastIndex,
                        activeSection = IdeSection.EDITOR,
                        autocompleteSuggestions = emptyList()
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(snackbarMessage = "Failed to open ${node.name}: ${err.message}") }
            }
        }
    }

    fun selectTab(index: Int) {
        if (index in _uiState.value.openTabs.indices) {
            _uiState.update { it.copy(activeTabIndex = index, autocompleteSuggestions = emptyList()) }
        }
    }

    fun closeTab(index: Int) {
        _uiState.update { state ->
            if (index !in state.openTabs.indices) return@update state
            val updated = state.openTabs.toMutableList().apply { removeAt(index) }
            val nextIdx = when {
                updated.isEmpty() -> -1
                state.activeTabIndex >= updated.size -> updated.lastIndex
                else -> state.activeTabIndex
            }
            state.copy(openTabs = updated, activeTabIndex = nextIdx, autocompleteSuggestions = emptyList())
        }
    }

    fun onEditorTextChanged(newText: String, cursorOffset: Int) {
        val tab = _uiState.value.activeTab ?: return
        if (newText != tab.content) {
            val stack = undoStacks.getOrPut(tab.uriString) { ArrayDeque() }
            if (stack.lastOrNull() != tab.content) {
                if (stack.size >= 40) stack.removeFirst()
                stack.addLast(tab.content)
            }
        }
        val suggestions = CppLanguageEngine.computeSuggestions(
            code = newText,
            cursorOffset = cursorOffset,
            localHeaders = _uiState.value.localHeaderPaths,
            localSymbols = _uiState.value.localHeaderSymbols,
            fileType = tab.fileType
        )
        _uiState.update { state ->
            val updated = state.openTabs.toMutableList()
            val idx = state.activeTabIndex
            if (idx in updated.indices) {
                updated[idx] = updated[idx].copy(content = newText, cursorOffset = cursorOffset)
            }
            state.copy(openTabs = updated, autocompleteSuggestions = suggestions)
        }
    }

    fun applyAutocompleteSuggestion(suggestion: AutocompleteSuggestion): Pair<String, Int>? {
        val tab = _uiState.value.activeTab ?: return null
        val start = suggestion.replaceStart.coerceIn(0, tab.content.length)
        val end = suggestion.replaceEnd.coerceIn(start, tab.content.length)
        val nextText = buildString {
            append(tab.content.substring(0, start))
            append(suggestion.insertText)
            append(tab.content.substring(end))
        }
        val nextCursor = start + suggestion.insertText.length
        onEditorTextChanged(nextText, nextCursor)
        _uiState.update { it.copy(autocompleteSuggestions = emptyList()) }
        return nextText to nextCursor
    }

    fun undoActiveEditor(): Pair<String, Int>? {
        val tab = _uiState.value.activeTab ?: return null
        val stack = undoStacks[tab.uriString] ?: return null
        if (stack.isEmpty()) return null
        val prev = stack.removeLast()
        val cursor = prev.length.coerceAtMost(tab.cursorOffset)
        _uiState.update { state ->
            val updated = state.openTabs.toMutableList()
            val idx = state.activeTabIndex
            if (idx in updated.indices) {
                updated[idx] = updated[idx].copy(content = prev, cursorOffset = cursor)
            }
            state.copy(openTabs = updated, autocompleteSuggestions = emptyList())
        }
        return prev to cursor
    }

    fun saveActiveTab() {
        val tab = _uiState.value.activeTab ?: return
        viewModelScope.launch {
            saveTabSuspend(tab).onSuccess {
                _uiState.update { it.copy(snackbarMessage = "Saved ${tab.name}") }
                if (tab.fileType == CodeFileType.HEADER) {
                    refreshWorkspace()
                }
            }.onFailure { err ->
                _uiState.update { it.copy(snackbarMessage = "Save failed: ${err.message}") }
            }
        }
    }

    private suspend fun saveTabSuspend(tab: OpenEditorTab): Result<Unit> {
        return workspaceManager.writeFileText(tab.uriString, tab.content).onSuccess {
            _uiState.update { state ->
                val updated = state.openTabs.map {
                    if (it.uriString == tab.uriString) it.copy(savedContent = it.content) else it
                }
                state.copy(openTabs = updated)
            }
        }
    }

    fun createNewFile(relativeName: String, templateContent: String) {
        viewModelScope.launch {
            val treeUri = _uiState.value.termuxEnv.connectedSafTreeUri
            workspaceManager.createFile(treeUri, relativeName, templateContent)
                .onSuccess { created ->
                    loadWorkspace(treeUri, created.uriString)
                    openNode(created)
                    _uiState.update { it.copy(snackbarMessage = "Created ${created.relativePath}") }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(snackbarMessage = "Create failed: ${err.message}") }
                }
        }
    }

    fun deleteWorkspaceNode(node: WorkspaceNode) {
        viewModelScope.launch {
            workspaceManager.deleteFile(node.uriString)
                .onSuccess {
                    _uiState.update { state ->
                        val updatedTabs = state.openTabs.filterNot { it.uriString == node.uriString }
                        val nextIdx = if (updatedTabs.isEmpty()) -1 else 0
                        state.copy(
                            openTabs = updatedTabs,
                            activeTabIndex = nextIdx,
                            snackbarMessage = "Deleted ${node.name}"
                        )
                    }
                    refreshWorkspace()
                }
                .onFailure { err ->
                    _uiState.update { it.copy(snackbarMessage = "Delete failed: ${err.message}") }
                }
        }
    }

    fun updateBuildProfile(transform: (BuildProfileEntity) -> BuildProfileEntity) {
        val next = transform(_uiState.value.buildProfile).copy(updatedAt = System.currentTimeMillis())
        _uiState.update { it.copy(buildProfile = next) }
        viewModelScope.launch {
            dao.upsertBuildProfile(next)
        }
    }

    fun toggleLibraryFlag(flag: String) {
        updateBuildProfile { profile ->
            val tokens = profile.linkedLibs.split(Regex("\\s+")).filter { it.isNotBlank() }.toMutableList()
            val flagTokens = flag.split(Regex("\\s+")).filter { it.isNotBlank() }
            val allPresent = flagTokens.all { it in tokens }
            if (allPresent) {
                tokens.removeAll(flagTokens.toSet())
            } else {
                flagTokens.forEach { if (it !in tokens) tokens.add(it) }
            }
            profile.copy(linkedLibs = tokens.joinToString(" "))
        }
    }

    /**
     * Triggered by the Add Library panel when attaching a library discovered in the user's folder
     * along with user-entered compiler/linker flags and arguments.
     */
    fun attachLibraryWithCustomFlags(lib: FolderLibraryItem, customFlagsInput: String) {
        val tokens = customFlagsInput.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val newIncDirs = mutableListOf<String>()
        val newLibDirs = mutableListOf<String>()
        val extraTokens = mutableListOf<String>()

        tokens.forEach { tok ->
            when {
                tok.startsWith("-I") && tok.length > 2 -> newIncDirs.add(tok.removePrefix("-I"))
                tok.startsWith("-L") && tok.length > 2 -> newLibDirs.add(tok.removePrefix("-L"))
                else -> extraTokens.add(tok)
            }
        }
        lib.includeDirs.forEach { if (it !in newIncDirs) newIncDirs.add(it) }
        lib.libDirs.forEach { if (it !in newLibDirs) newLibDirs.add(it) }

        updateBuildProfile { profile ->
            val mergedInc = (profile.includeDirs.split(',').map { it.trim() }.filter { it.isNotEmpty() } + newIncDirs)
                .distinct()
                .joinToString(",")
            val mergedLibDirs = (profile.libraryDirs.split(',').map { it.trim() }.filter { it.isNotEmpty() } + newLibDirs)
                .distinct()
                .joinToString(",")
            val existingLinked = profile.linkedLibs.split(Regex("\\s+")).filter { it.isNotBlank() }.toMutableList()
            extraTokens.forEach { tok -> if (tok !in existingLinked) existingLinked.add(tok) }

            profile.copy(
                includeDirs = mergedInc,
                libraryDirs = mergedLibDirs,
                linkedLibs = existingLinked.joinToString(" ")
            )
        }
        _uiState.update { it.copy(snackbarMessage = "Added ${lib.name} flags to build profile") }
    }

    fun adjustFontSize(delta: Int) {
        val next = (_uiState.value.editorFontSizeSp + delta).coerceIn(10, 22)
        _uiState.update { it.copy(editorFontSizeSp = next) }
        viewModelScope.launch { prefsRepo.setEditorFontSize(next) }
    }

    fun setKeepBackgroundAlive(enabled: Boolean) {
        _uiState.update { it.copy(keepBackgroundAlive = enabled) }
        viewModelScope.launch { prefsRepo.setKeepBackgroundServiceAlive(enabled) }
        if (!enabled) {
            BuildPersistenceService.stop(context)
        }
    }

    // --- Termux Package (`termux-studio`), Compilers & CDN Actions ---

    fun probeTermuxCompilers() {
        if (!_uiState.value.termuxEnv.isTermuxInstalled) {
            _uiState.update { it.copy(showTermuxMissingDialog = true) }
            return
        }
        val workdir = resolveExecutionWorkdir(_uiState.value)
        executeTermuxScript(
            script = "termux-studio link-storage \"$workdir\" && termux-studio probe",
            displayCommand = "termux-studio probe",
            workdir = workdir,
            purpose = ExecutionPurpose.COMPILER_PROBE,
            label = "Probing Termux compilers & libraries"
        )
    }

    fun startTermuxListenerDaemon() {
        if (!_uiState.value.termuxEnv.isTermuxInstalled) {
            _uiState.update { it.copy(showTermuxMissingDialog = true) }
            return
        }
        val workdir = resolveExecutionWorkdir(_uiState.value)
        executeTermuxScript(
            script = "termux-studio listen \"$workdir\" --once && termux-studio probe",
            displayCommand = "termux-studio listen",
            workdir = workdir,
            purpose = ExecutionPurpose.COMPILER_PROBE,
            label = "Syncing termux-studio listener daemon"
        )
    }

    fun installTermuxCompilers(termuxPackages: List<String>) {
        if (termuxPackages.isEmpty()) return
        if (!_uiState.value.termuxEnv.isTermuxInstalled) {
            _uiState.update { it.copy(showTermuxMissingDialog = true) }
            return
        }
        val pkgList = termuxPackages.flatMap { it.split(Regex("\\s+")) }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
        val cmd = "pkg update -y && pkg install -y $pkgList && termux-studio probe"
        executeTermuxScript(
            script = cmd,
            displayCommand = "pkg install -y $pkgList",
            workdir = resolveExecutionWorkdir(_uiState.value),
            purpose = ExecutionPurpose.PKG_INSTALL,
            label = "Installing compilers ($pkgList)"
        )
    }

    fun installTermuxStudioLocalDeb() {
        if (!_uiState.value.termuxEnv.isTermuxInstalled) {
            _uiState.update { it.copy(showTermuxMissingDialog = true) }
            return
        }
        val workdir = resolveExecutionWorkdir(_uiState.value)
        workspaceManager.ensureStarterWorkspace()
        val cmd = TermuxBridge.buildLocalDebInstallCommand(workdir)
        executeTermuxScript(
            script = cmd,
            displayCommand = "pkg install ./${TermuxAptRepoBuilder.DEB_FILE_NAME}",
            workdir = workdir,
            purpose = ExecutionPurpose.PKG_INSTALL,
            label = "Installing termux-studio package into Termux"
        )
    }

    fun installTermuxStudioFromGitHubApt(repoUrl: String) {
        updateGithubAptRepoUrl(repoUrl)
        if (!_uiState.value.termuxEnv.isTermuxInstalled) {
            _uiState.update { it.copy(showTermuxMissingDialog = true) }
            return
        }
        val workdir = resolveExecutionWorkdir(_uiState.value)
        val cmd = TermuxBridge.buildGitHubAptInstallCommand(repoUrl, workdir)
        executeTermuxScript(
            script = cmd,
            displayCommand = "pkg install termux-studio (from GitHub APT repo)",
            workdir = workdir,
            purpose = ExecutionPurpose.PKG_INSTALL,
            label = "Installing termux-studio via GitHub APT"
        )
    }

    fun regenerateAptRepoBundle() {
        viewModelScope.launch {
            val root = workspaceManager.getActiveLocalRoot()
            workspaceManager.forceRegenerateAptRepo(root)
                .onSuccess { deb ->
                    refreshWorkspace()
                    _uiState.update {
                        it.copy(snackbarMessage = "Generated APT repo & ${deb.name} in ${root.name}/termux-repo/")
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(snackbarMessage = "Repo export failed: ${err.message}") }
                }
        }
    }

    /**
     * Registers an online CDN API/URL provider without permanently downloading it offline.
     * When enabled, Termux fetches it live at compile time (requires Wi-Fi/Internet).
     */
    fun registerOnlineCdnLibrary(libName: String, url: String, customFlags: String) {
        val cleanName = libName.trim().replace(Regex("[^A-Za-z0-9_.-]"), "")
        val cleanUrl = url.trim()
        if (cleanName.isBlank() || cleanUrl.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Enter a CDN library name and online API/URL") }
            return
        }
        val flags = customFlags.trim()
        viewModelScope.launch {
            dao.upsertCdnLibrary(
                CdnLibraryEntity(
                    name = cleanName,
                    url = cleanUrl,
                    installRelPath = "online-live (requires Wi-Fi/Internet)",
                    compilerFlags = flags,
                    isEnabled = true
                )
            )
            _uiState.update {
                it.copy(snackbarMessage = "Added online CDN '$cleanName'. Active during builds when connected to Internet.")
            }
        }
    }

    fun toggleCdnLibraryEnabled(lib: CdnLibraryEntity) {
        viewModelScope.launch {
            dao.setCdnLibraryEnabled(lib.name, !lib.isEnabled)
        }
    }

    fun deleteCdnLibraryRecord(lib: CdnLibraryEntity) {
        viewModelScope.launch {
            dao.deleteCdnLibrary(lib.name)
            _uiState.update { it.copy(snackbarMessage = "Removed ${lib.name} from CDN providers") }
        }
    }

    /**
     * Downloads or installs a library permanently into the user's workspace or Termux prefix
     * using real Termux commands (`pkg`, `curl`, `wget`, or `git`).
     */
    fun downloadLibraryWithTermux(tool: String, libName: String, target: String) {
        val cleanTarget = target.trim()
        if (cleanTarget.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Enter a Termux package name or download URL") }
            return
        }
        if (!_uiState.value.termuxEnv.isTermuxInstalled) {
            _uiState.update { it.copy(showTermuxMissingDialog = true) }
            return
        }
        val cleanName = libName.trim().ifBlank {
            cleanTarget.substringAfterLast('/').substringBefore('.').ifBlank { "custom_lib" }
        }
        val workdir = resolveExecutionWorkdir(_uiState.value)
        val cmd = TermuxBridge.buildTermuxLibFetchCommand(workdir, tool, cleanName, cleanTarget)
        executeTermuxScript(
            script = cmd,
            displayCommand = "$tool fetch $cleanName ($cleanTarget)",
            workdir = workdir,
            purpose = ExecutionPurpose.LIB_FETCH,
            label = "Fetching library $cleanName via Termux ($tool)"
        )
    }

    fun importOfflineLibraryFiles(libName: String, uris: List<Uri>) {
        viewModelScope.launch {
            val root = workspaceManager.ensureStarterWorkspace()
            offlineLibImporter.importFiles(root, libName, uris)
                .onSuccess { report ->
                    refreshWorkspace()
                    _uiState.update {
                        it.copy(snackbarMessage = "Imported ${report.importedCount} item(s) to ${report.destPath}/")
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(snackbarMessage = "Import failed: ${err.message}") }
                }
        }
    }

    fun importOfflineLibraryFolder(libName: String, treeUri: Uri) {
        viewModelScope.launch {
            val root = workspaceManager.ensureStarterWorkspace()
            offlineLibImporter.importFolderTree(root, libName, treeUri)
                .onSuccess { report ->
                    refreshWorkspace()
                    _uiState.update {
                        it.copy(snackbarMessage = "Imported folder (${report.importedCount} files) into ${report.destPath}/")
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(snackbarMessage = "Folder import failed: ${err.message}") }
                }
        }
    }

    fun configureAppTarget(platform: AppPlatformTarget, distro: LinuxDistroOption) {
        _uiState.update {
            it.copy(selectedAppPlatform = platform, selectedLinuxDistro = distro)
        }
        val state = _uiState.value
        val args = if (platform == AppPlatformTarget.LINUX) "${platform.cliArg} ${distro.id}" else platform.cliArg
        val appCmd = "app $args"

        val configPreview = buildString {
            val pkgFmt = if (platform == AppPlatformTarget.LINUX) distro.pkgExt.removePrefix(".") else platform.packageExt.removePrefix(".")
            appendLine("# Termux studio App Builder Configuration")
            appendLine("APP_NAME=${state.buildProfile.outputName.ifBlank { "MyTermuxApp" }}")
            appendLine("PACKAGE_ID=com.termux.studio.myapp")
            appendLine("VERSION=1.0.0")
            appendLine("TARGET_PLATFORM=${platform.cliArg}")
            appendLine("LINUX_DISTRO=${if (platform == AppPlatformTarget.LINUX) distro.id else platform.cliArg.lowercase()}")
            appendLine("PACKAGE_FORMAT=$pkgFmt")
            appendLine("ENTRY_SOURCE=${state.activeTab?.relativePath ?: "main.cpp"}")
            appendLine("ENABLE_RAYLIB=${state.buildProfile.linkedLibs.contains("-lraylib")}")
            appendLine("ENABLE_GPU_DRIVERS=${state.autoGpuDrivers}")
            appendLine("OUTPUT_DIR=dist")
        }

        viewModelScope.launch {
            val treeUri = state.termuxEnv.connectedSafTreeUri
            workspaceManager.createFile(treeUri, "build.config", configPreview)
            loadWorkspace(treeUri, state.activeTab?.uriString)

            if (state.termuxEnv.isTermuxInstalled) {
                val workdir = resolveExecutionWorkdir(state)
                executeTermuxScript(
                    script = appCmd,
                    displayCommand = appCmd,
                    workdir = workdir,
                    purpose = ExecutionPurpose.APP_CONFIG_BUILD,
                    label = "Generating build.config via $appCmd"
                )
            } else {
                _uiState.update { it.copy(snackbarMessage = "Generated build.config for ${platform.label}") }
            }
        }
    }

    fun buildAppPackage() {
        val state = _uiState.value
        if (!state.termuxEnv.isTermuxInstalled) {
            _uiState.update { it.copy(showTermuxMissingDialog = true) }
            return
        }
        viewModelScope.launch {
            state.openTabs.filter { it.isDirty }.forEach { saveTabSuspend(it) }
            val workdir = resolveExecutionWorkdir(state)
            val cmd = "app build"
            executeTermuxScript(
                script = cmd,
                displayCommand = cmd,
                workdir = workdir,
                purpose = ExecutionPurpose.BUILD_RUN,
                label = "Building package from build.config"
            )
        }
    }

    fun previewActiveCompileCommand(runBinaryAfterCompile: Boolean = true): String {
        val state = _uiState.value
        val active = state.activeTab ?: return "Open a file to preview compile command"
        return buildCompileCommandForFile(state, active, runBinaryAfterCompile).first
    }

    fun compileAndRunActive(
        runBinaryAfterCompile: Boolean = true,
        stayInEditorDrawer: Boolean = _uiState.value.activeSection == IdeSection.EDITOR
    ) {
        val state = _uiState.value
        if (!state.termuxEnv.isTermuxInstalled) {
            _uiState.update { it.copy(showTermuxMissingDialog = true) }
            return
        }
        val active = state.activeTab
        if (active == null) {
            _uiState.update { it.copy(snackbarMessage = "Open a source file first") }
            return
        }

        viewModelScope.launch {
            state.openTabs.filter { it.isDirty }.forEach { saveTabSuspend(it) }

            val (compileCmd, stageBin) = buildCompileCommandForFile(state, active, runBinaryAfterCompile)
            val workdir = resolveExecutionWorkdir(state)
            val enabledCdn = cdnLibraries.value.filter { it.isEnabled }
            val cdnExtraFlags = enabledCdn.mapNotNull {
                it.compilerFlags.trim().takeIf { f -> f.isNotEmpty() }
            }.joinToString(" ")
            val fullCompileCmd = if (cdnExtraFlags.isNotBlank()) {
                "$compileCmd $cdnExtraFlags"
            } else {
                compileCmd
            }

            val cdnPrefix = TermuxBridge.buildCdnResolvePrefix(workdir, enabledCdn)
            val wrappedCompile = TermuxBridge.wrapCompileWithTermuxStudio(
                workdir = workdir,
                command = fullCompileCmd,
                runBinaryName = if (runBinaryAfterCompile) stageBin else null
            )
            val fullScript = "$cdnPrefix && $wrappedCompile"

            if (stayInEditorDrawer) {
                _uiState.update { it.copy(showEditorBuildDrawer = true) }
            }

            executeTermuxScript(
                script = fullScript,
                displayCommand = fullCompileCmd,
                workdir = workdir,
                purpose = ExecutionPurpose.BUILD_RUN,
                label = "Compiling ${active.name} via Termux",
                switchToTerminalOnBuild = !stayInEditorDrawer
            )
        }
    }

    private fun buildCompileCommandForFile(
        state: IdeUiState,
        active: OpenEditorTab,
        runAfter: Boolean
    ): Pair<String, String?> {
        val profile = state.buildProfile
        val outName = profile.outputName.ifBlank { "app_out" }
        val fileRef = "\"${active.relativePath}\""
        val compilerChoice = runCatching { CompilerChoice.valueOf(profile.compilerName) }
            .getOrDefault(CompilerChoice.AUTO)

        // Honor explicit non-C/C++ compiler selections or file extensions
        if (active.fileType == CodeFileType.JAVA || compilerChoice == CompilerChoice.OPENJDK) {
            val cls = active.name.substringBeforeLast('.')
            val dir = active.relativePath.substringBeforeLast('/', ".")
            val cmd = if (runAfter) "javac $fileRef && java -cp \"$dir\" $cls" else "javac $fileRef"
            return cmd to null
        }
        if (active.fileType == CodeFileType.KOTLIN || compilerChoice == CompilerChoice.KOTLIN) {
            val jar = "$outName.jar"
            val cmd = if (runAfter) "kotlinc $fileRef -include-runtime -d \"$jar\" && java -jar \"$jar\""
            else "kotlinc $fileRef -include-runtime -d \"$jar\""
            return cmd to null
        }
        if (active.fileType == CodeFileType.RUST || compilerChoice == CompilerChoice.RUST) {
            val cmd = if (runAfter) "rustc $fileRef -o \"$outName\" && ./\"$outName\"" else "rustc $fileRef -o \"$outName\""
            return cmd to outName
        }
        if (active.fileType == CodeFileType.GO || compilerChoice == CompilerChoice.GO) {
            val cmd = if (runAfter) "go run $fileRef" else "go build -o \"$outName\" $fileRef"
            return cmd to null
        }
        if (active.fileType == CodeFileType.ZIG || compilerChoice == CompilerChoice.ZIG) {
            val cmd = if (runAfter) "zig run $fileRef" else "zig build-exe $fileRef -femit-bin=\"$outName\""
            return cmd to null
        }
        if (active.fileType == CodeFileType.ASSEMBLY || compilerChoice == CompilerChoice.NASM) {
            return if (active.name.lowercase().endsWith(".asm") || compilerChoice == CompilerChoice.NASM) {
                val obj = active.name.substringBeforeLast('.') + ".o"
                val cmd = "nasm -f elf64 $fileRef -o \"$obj\" && ld \"$obj\" -o \"$outName\""
                (if (runAfter) "$cmd && ./\"$outName\"" else cmd) to outName
            } else {
                val cmd = if (runAfter) "clang $fileRef -o \"$outName\" && ./\"$outName\"" else "clang $fileRef -o \"$outName\""
                cmd to outName
            }
        }
        if (active.fileType == CodeFileType.FORTRAN || compilerChoice == CompilerChoice.GFORTRAN) {
            val cmd = if (runAfter) "gfortran $fileRef -o \"$outName\" && ./\"$outName\"" else "gfortran $fileRef -o \"$outName\""
            return cmd to outName
        }

        return when (active.fileType) {
            CodeFileType.D_LANG -> {
                val cmd = if (runAfter) "ldc2 $fileRef -of=\"$outName\" && ./\"$outName\"" else "ldc2 $fileRef -of=\"$outName\""
                cmd to outName
            }
            CodeFileType.NIM -> (if (runAfter) "nim c -r $fileRef" else "nim c $fileRef") to null
            CodeFileType.PASCAL -> {
                val cmd = if (runAfter) "fpc $fileRef -o\"$outName\" && ./\"$outName\"" else "fpc $fileRef -o\"$outName\""
                cmd to outName
            }
            CodeFileType.CSHARP -> {
                val cmd = if (runAfter) "mcs $fileRef -out:\"$outName.exe\" && mono \"$outName.exe\"" else "mcs $fileRef -out:\"$outName.exe\""
                cmd to null
            }
            CodeFileType.SHELL -> "bash $fileRef" to null
            CodeFileType.BUILD_CONFIG -> "app build" to null
            CodeFileType.MAKEFILE -> "make" to null
            else -> buildCOrCppCompileCommand(state, active, runAfter)
        }
    }

    private fun buildCOrCppCompileCommand(
        state: IdeUiState,
        active: OpenEditorTab,
        runBinaryAfterCompile: Boolean
    ): Pair<String, String?> {
        val profile = state.buildProfile
        val compilerChoice = runCatching { CompilerChoice.valueOf(profile.compilerName) }
            .getOrDefault(CompilerChoice.AUTO)
        val targetType = runCatching { OutputTargetType.valueOf(profile.outputType) }
            .getOrDefault(OutputTargetType.EXECUTABLE)

        val isCOnly = active.fileType == CodeFileType.C_SOURCE &&
            !active.content.contains("iostream") &&
            !active.content.contains("std::") &&
            compilerChoice != CompilerChoice.CLANG_CPP &&
            compilerChoice != CompilerChoice.GPP

        val compilerBin = when (compilerChoice) {
            CompilerChoice.CLANG -> "clang"
            CompilerChoice.CLANG_CPP -> "clang++"
            CompilerChoice.GCC -> "gcc"
            CompilerChoice.GPP -> "g++"
            else -> if (isCOnly) compilerChoice.cBin else compilerChoice.cppBin
        }
        val stdFlag = if (compilerBin in setOf("gcc", "clang")) "-std=${profile.cStandard}" else "-std=${profile.cppStandard}"

        val baseIncludes = profile.includeDirs.split(',')
            .map { it.trim() }.filter { it.isNotEmpty() } +
            listOf("include", "libs/app", "libs/raylib/include", "libs/raylib")
        val includeFlags = baseIncludes.distinct().joinToString(" ") { "-I\"$it\"" }

        val baseLibDirs = profile.libraryDirs.split(',')
            .map { it.trim() }.filter { it.isNotEmpty() } +
            listOf("libs/raylib/lib", "libs")
        val libDirFlags = baseLibDirs.distinct().joinToString(" ") { "-L\"$it\"" }
        val baseOutName = profile.outputName.ifBlank { "app_out" }

        val usesAppLib = active.content.contains("app.h") || profile.linkedLibs.contains("-lapp")
        val usesRaylib = active.content.contains("raylib.h") || profile.linkedLibs.contains("-lraylib")
        val usesGpu = state.autoGpuDrivers || active.content.contains("GLES") || active.content.contains("vulkan")

        val extraLibs = buildString {
            append(profile.linkedLibs.replace("-lapp", "").trim())
            if (usesRaylib && !profile.linkedLibs.contains("-lraylib")) {
                append(" -lraylib -lGLESv2 -lEGL")
            } else if (usesGpu && (active.content.contains("GLES") || active.content.contains("EGL"))) {
                append(" -lGLESv3 -lEGL")
            }
            if (usesGpu && active.content.contains("vulkan") && !profile.linkedLibs.contains("-lvulkan")) {
                append(" -lvulkan")
            }
        }.trim()

        val appRuntimeSrc = if (usesAppLib) "\"libs/app/app_runtime.c\"" else ""

        return when (targetType) {
            OutputTargetType.EXECUTABLE -> {
                val srcList = if (active.fileType == CodeFileType.HEADER) {
                    state.workspaceNodes
                        .filter { !it.isDirectory && (it.fileType == CodeFileType.C_SOURCE || it.fileType == CodeFileType.CPP_SOURCE) && !it.relativePath.startsWith("libs/") }
                        .joinToString(" ") { "\"${it.relativePath}\"" }
                } else {
                    if (active.name == "main.cpp" && state.workspaceNodes.any { it.relativePath == "src/vector_math.cpp" }) {
                        "\"${active.relativePath}\" \"src/vector_math.cpp\""
                    } else {
                        "\"${active.relativePath}\""
                    }
                }
                val srcs = listOf(srcList, appRuntimeSrc).filter { it.isNotBlank() }.joinToString(" ")
                val runPart = if (runBinaryAfterCompile) " && ./\"$baseOutName\"" else ""
                "$compilerBin $stdFlag ${state.compilerOptLevel} ${profile.extraFlags} $includeFlags $srcs $libDirFlags $extraLibs -o \"$baseOutName\"$runPart" to baseOutName
            }
            OutputTargetType.SHARED_SO -> {
                val soName = if (baseOutName.endsWith(".so")) baseOutName else "lib${baseOutName}.so"
                val srcs = listOf("\"${active.relativePath}\"", appRuntimeSrc).filter { it.isNotBlank() }.joinToString(" ")
                "$compilerBin $stdFlag -shared -fPIC ${profile.extraFlags} $includeFlags $srcs $libDirFlags $extraLibs -o \"$soName\" && ls -lh \"$soName\"" to null
            }
            OutputTargetType.STATIC_A -> {
                val objName = active.name.substringBeforeLast('.') + ".o"
                val aName = if (baseOutName.endsWith(".a")) baseOutName else "lib${baseOutName}.a"
                "$compilerBin $stdFlag -c ${profile.extraFlags} $includeFlags \"${active.relativePath}\" -o \"$objName\" && ar rcs \"$aName\" \"$objName\" && ar -t \"$aName\"" to null
            }
        }
    }

    private fun resolveExecutionWorkdir(state: IdeUiState): String {
        return if (state.termuxEnv.connectedSafTreeUri != null) {
            state.termuxEnv.resolvedTermuxWorkdir
        } else {
            workspaceManager.getActiveLocalRoot().absolutePath
        }
    }

    fun runTerminalCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return
        if (!_uiState.value.termuxEnv.isTermuxInstalled) {
            _uiState.update { it.copy(showTermuxMissingDialog = true) }
            return
        }
        executeTermuxScript(
            script = trimmed,
            displayCommand = trimmed,
            workdir = resolveExecutionWorkdir(_uiState.value),
            purpose = ExecutionPurpose.TERMINAL,
            label = "Termux: $trimmed"
        )
    }

    fun clearTerminal() {
        _uiState.update { it.copy(terminalEntries = emptyList(), diagnostics = emptyList()) }
    }

    fun refreshGitStatus() {
        val script = """
            if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
              echo "__IS_REPO__=1"
              echo "__BRANCH__=${'$'}(git branch --show-current 2>/dev/null || echo HEAD)"
              echo "__REMOTE__=${'$'}(git config --get remote.origin.url 2>/dev/null)"
              echo "__NAME__=${'$'}(git config --get user.name 2>/dev/null)"
              echo "__EMAIL__=${'$'}(git config --get user.email 2>/dev/null)"
              echo "__STATUS_START__"
              git status --porcelain
            else
              echo "__IS_REPO__=0"
            fi
        """.trimIndent()
        executeTermuxScript(
            script = script,
            displayCommand = "git status --porcelain",
            workdir = resolveExecutionWorkdir(_uiState.value),
            purpose = ExecutionPurpose.GIT_STATUS,
            label = "Checking Git repository status"
        )
    }

    fun loadGitDiff() {
        executeTermuxScript(
            script = "git diff --no-color",
            displayCommand = "git diff",
            workdir = resolveExecutionWorkdir(_uiState.value),
            purpose = ExecutionPurpose.GIT_DIFF,
            label = "Fetching Git diff"
        )
    }

    fun gitInitRepo() {
        executeTermuxScript(
            script = "git init -b main",
            displayCommand = "git init -b main",
            workdir = resolveExecutionWorkdir(_uiState.value),
            purpose = ExecutionPurpose.GIT_MUTATION,
            label = "Initializing Git repository"
        )
    }

    fun gitCommitAll(message: String) {
        val safeMsg = message.trim().replace("\"", "\\\"")
        if (safeMsg.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Enter a commit message first") }
            return
        }
        executeTermuxScript(
            script = "git add -A && git commit -m \"$safeMsg\"",
            displayCommand = "git add -A && git commit -m \"$safeMsg\"",
            workdir = resolveExecutionWorkdir(_uiState.value),
            purpose = ExecutionPurpose.GIT_MUTATION,
            label = "Committing changes"
        )
    }

    fun gitPull() {
        executeTermuxScript(
            script = "git pull --rebase",
            displayCommand = "git pull --rebase",
            workdir = resolveExecutionWorkdir(_uiState.value),
            purpose = ExecutionPurpose.GIT_MUTATION,
            label = "Pulling from remote"
        )
    }

    fun gitPush() {
        executeTermuxScript(
            script = "git push -u origin HEAD",
            displayCommand = "git push -u origin HEAD",
            workdir = resolveExecutionWorkdir(_uiState.value),
            purpose = ExecutionPurpose.GIT_MUTATION,
            label = "Pushing to GitHub"
        )
    }

    fun gitCloneRepo(repoUrl: String, targetFolder: String) {
        val url = repoUrl.trim()
        val folder = targetFolder.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Repository URL is required") }
            return
        }
        val cmd = if (folder.isNotBlank()) "git clone \"$url\" \"$folder\"" else "git clone \"$url\""
        executeTermuxScript(
            script = cmd,
            displayCommand = cmd,
            workdir = resolveExecutionWorkdir(_uiState.value),
            purpose = ExecutionPurpose.GIT_MUTATION,
            label = "Cloning repository"
        )
    }

    fun configureGitHubCredentials(name: String, email: String, remoteUrl: String, patToken: String) {
        val parts = mutableListOf<String>()
        if (name.isNotBlank()) parts.add("git config user.name \"${name.trim().replace("\"", "\\\"")}\"")
        if (email.isNotBlank()) parts.add("git config user.email \"${email.trim().replace("\"", "\\\"")}\"")
        if (remoteUrl.isNotBlank()) {
            val cleanUrl = remoteUrl.trim()
            parts.add("(git remote set-url origin \"$cleanUrl\" 2>/dev/null || git remote add origin \"$cleanUrl\")")
        }
        if (patToken.isNotBlank()) {
            val tok = patToken.trim()
            parts.add("git config credential.helper store && printf 'https://x-access-token:%s@github.com\\n' '$tok' > ~/.git-credentials && chmod 600 ~/.git-credentials")
        }
        if (parts.isEmpty()) return
        executeTermuxScript(
            script = parts.joinToString(" && "),
            displayCommand = "Configuring Git identity & GitHub remote",
            workdir = resolveExecutionWorkdir(_uiState.value),
            purpose = ExecutionPurpose.GIT_MUTATION,
            label = "Updating GitHub configuration"
        )
    }

    private fun executeTermuxScript(
        script: String,
        displayCommand: String,
        workdir: String,
        purpose: ExecutionPurpose,
        label: String,
        switchToTerminalOnBuild: Boolean = true
    ) {
        val entryId = terminalEntryCounter++
        val timestamp = timeFormat.format(Date())
        val shouldShowTerminal = purpose == ExecutionPurpose.BUILD_RUN ||
            purpose == ExecutionPurpose.TERMINAL ||
            purpose == ExecutionPurpose.APP_CONFIG_BUILD ||
            purpose == ExecutionPurpose.PKG_INSTALL ||
            purpose == ExecutionPurpose.LIB_FETCH

        if (shouldShowTerminal) {
            _uiState.update { state ->
                val nextSection = if (purpose == ExecutionPurpose.BUILD_RUN && switchToTerminalOnBuild) {
                    IdeSection.TERMINAL
                } else {
                    state.activeSection
                }
                state.copy(
                    activeSection = nextSection,
                    jobStatus = JobStatus.RUNNING,
                    terminalEntries = state.terminalEntries + TerminalEntry(
                        id = entryId,
                        timestampLabel = timestamp,
                        command = displayCommand,
                        stdout = "",
                        stderr = "",
                        exitCode = null,
                        isRunning = true
                    )
                )
            }
        } else {
            _uiState.update { it.copy(jobStatus = JobStatus.RUNNING) }
        }

        val sharedDir = workspaceManager.getActiveLocalRoot().absolutePath
        val dispatchResult = TermuxBridge.dispatchBashCommand(
            context = context,
            script = script,
            workdir = workdir,
            sharedDir = sharedDir,
            label = label,
            keepAliveService = _uiState.value.keepBackgroundAlive,
            injectGpuDrivers = _uiState.value.autoGpuDrivers
        )

        dispatchResult.onSuccess { execId ->
            pendingPurposes[execId] = purpose to entryId
        }.onFailure { err ->
            _uiState.update { state ->
                val updatedEntries = if (shouldShowTerminal) {
                    state.terminalEntries.map {
                        if (it.id == entryId) {
                            it.copy(
                                stderr = err.message ?: "Failed to start Termux",
                                exitCode = -1,
                                isRunning = false
                            )
                        } else it
                    }
                } else state.terminalEntries
                state.copy(
                    jobStatus = JobStatus.FAILED,
                    terminalEntries = updatedEntries,
                    snackbarMessage = err.message
                )
            }
        }
    }

    private fun handleTermuxResult(result: TermuxCommandResult) {
        val meta = pendingPurposes.remove(result.executionId)
        val purpose = meta?.first ?: ExecutionPurpose.TERMINAL
        val entryId = meta?.second ?: -1L
        val succeeded = result.exitCode == 0 && result.err == 0

        if (purpose == ExecutionPurpose.COMPILER_PROBE) {
            parseCompilerProbeOutput(result.stdout)
            refreshIpcProbeFromDisk()
            _uiState.update {
                it.copy(
                    jobStatus = if (succeeded) JobStatus.SUCCEEDED else JobStatus.FAILED,
                    snackbarMessage = if (succeeded) "Probed Termux compilers & listener status" else (result.stderr.ifBlank { "Probe failed" })
                )
            }
            return
        }

        if (purpose == ExecutionPurpose.BUILD_RUN ||
            purpose == ExecutionPurpose.TERMINAL ||
            purpose == ExecutionPurpose.APP_CONFIG_BUILD ||
            purpose == ExecutionPurpose.PKG_INSTALL ||
            purpose == ExecutionPurpose.LIB_FETCH
        ) {
            val combinedDiagText = listOf(result.stderr, result.stdout).filter { it.isNotBlank() }.joinToString("\n")
            val parsedDiags = CppLanguageEngine.parseCompilerDiagnostics(combinedDiagText)
            if (purpose == ExecutionPurpose.PKG_INSTALL && result.stdout.contains("termux_studio=")) {
                parseCompilerProbeOutput(result.stdout)
            }
            _uiState.update { state ->
                val updated = state.terminalEntries.map {
                    if (it.id == entryId) {
                        it.copy(
                            stdout = result.stdout,
                            stderr = result.stderr,
                            exitCode = result.exitCode,
                            isRunning = false,
                            internalError = result.errmsg
                        )
                    } else it
                }
                val snack = when {
                    purpose == ExecutionPurpose.PKG_INSTALL && succeeded -> "Termux packages installed cleanly"
                    purpose == ExecutionPurpose.LIB_FETCH && succeeded -> "Downloaded library via Termux into libs/"
                    purpose == ExecutionPurpose.APP_CONFIG_BUILD && succeeded -> result.stdout.trim().lines().lastOrNull()
                    else -> state.snackbarMessage
                }
                state.copy(
                    jobStatus = if (succeeded) JobStatus.SUCCEEDED else JobStatus.FAILED,
                    terminalEntries = updated,
                    diagnostics = if (purpose == ExecutionPurpose.BUILD_RUN) parsedDiags else state.diagnostics,
                    snackbarMessage = snack
                )
            }
            viewModelScope.launch {
                val cmd = _uiState.value.terminalEntries.firstOrNull { it.id == entryId }?.command ?: return@launch
                dao.insertCommandHistory(
                    CommandHistoryEntity(
                        command = cmd,
                        workdir = resolveExecutionWorkdir(_uiState.value),
                        exitCode = result.exitCode
                    )
                )
            }
            if (succeeded && (purpose == ExecutionPurpose.BUILD_RUN || purpose == ExecutionPurpose.LIB_FETCH || purpose == ExecutionPurpose.PKG_INSTALL)) {
                refreshWorkspace()
                refreshIpcProbeFromDisk()
            }
        } else if (purpose == ExecutionPurpose.GIT_STATUS) {
            parseGitStatusResult(result)
        } else if (purpose == ExecutionPurpose.GIT_DIFF) {
            _uiState.update { state ->
                state.copy(
                    jobStatus = if (succeeded) JobStatus.SUCCEEDED else JobStatus.FAILED,
                    gitState = state.gitState.copy(
                        diffPreview = result.stdout.ifBlank { result.stderr.ifBlank { "Working tree clean — no unstaged diffs." } }
                    )
                )
            }
        } else if (purpose == ExecutionPurpose.GIT_MUTATION) {
            val msg = listOf(result.stdout.trim(), result.stderr.trim(), result.errmsg.orEmpty())
                .firstOrNull { it.isNotBlank() } ?: "Git operation finished (exit ${result.exitCode})"
            _uiState.update { state ->
                state.copy(
                    jobStatus = if (succeeded) JobStatus.SUCCEEDED else JobStatus.FAILED,
                    gitState = state.gitState.copy(statusMessage = msg),
                    snackbarMessage = msg.lines().firstOrNull()
                )
            }
            if (succeeded) {
                refreshGitStatus()
                refreshWorkspace()
            }
        }
    }

    private fun parseCompilerProbeOutput(stdout: String) {
        val parsed = mutableMapOf<String, CompilerProbeStatus>()
        stdout.lines().forEach { line ->
            val eq = line.indexOf('=')
            if (eq > 0) {
                val key = line.substring(0, eq).trim()
                val rest = line.substring(eq + 1)
                if (rest.startsWith("1|") || rest.startsWith("0|")) {
                    parsed[key] = CompilerProbeStatus(
                        isInstalled = rest.startsWith("1|"),
                        versionLine = rest.substringAfter('|', "").trim()
                    )
                }
            }
        }
        if (parsed.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(
                    compilerProbeMap = parsed,
                    termuxEnv = state.termuxEnv.copy(
                        isTermuxStudioPkgInstalled = parsed["termux_studio"]?.isInstalled == true ||
                            state.termuxEnv.isTermuxStudioPkgInstalled
                    )
                )
            }
        }
    }

    private fun parseGitStatusResult(result: TermuxCommandResult) {
        if (result.err != 0 || result.errmsg != null) {
            _uiState.update {
                it.copy(
                    jobStatus = JobStatus.FAILED,
                    gitState = it.gitState.copy(statusMessage = result.errmsg ?: "Termux git check failed")
                )
            }
            return
        }
        val lines = result.stdout.lines()
        val isRepo = lines.any { it.trim() == "__IS_REPO__=1" }
        val branch = lines.firstOrNull { it.startsWith("__BRANCH__=") }?.substringAfter("=")?.ifBlank { "main" } ?: "main"
        val remote = lines.firstOrNull { it.startsWith("__REMOTE__=") }?.substringAfter("=") ?: ""
        val name = lines.firstOrNull { it.startsWith("__NAME__=") }?.substringAfter("=") ?: ""
        val email = lines.firstOrNull { it.startsWith("__EMAIL__=") }?.substringAfter("=") ?: ""
        val statusIdx = lines.indexOfFirst { it.trim() == "__STATUS_START__" }
        val changes = if (statusIdx != -1) {
            lines.drop(statusIdx + 1).filter { it.length >= 4 }.map { line ->
                GitFileChange(
                    statusCode = line.substring(0, 2),
                    filePath = line.substring(3).trim()
                )
            }
        } else emptyList()

        _uiState.update {
            it.copy(
                jobStatus = JobStatus.SUCCEEDED,
                gitState = it.gitState.copy(
                    isGitRepo = isRepo,
                    branch = branch,
                    remoteUrl = remote,
                    authorName = name,
                    authorEmail = email,
                    changes = changes,
                    statusMessage = if (isRepo) {
                        if (changes.isEmpty()) "Clean working tree on $branch" else "${changes.size} modified/untracked file(s)"
                    } else {
                        "Current folder is not a Git repository yet."
                    }
                )
            )
        }
    }

    fun jumpToDiagnostic(diag: CompilerDiagnostic) {
        val targetNode = _uiState.value.workspaceNodes.firstOrNull {
            !it.isDirectory && (it.name == diag.fileName || it.relativePath.endsWith(diag.fileName))
        }
        val openTabIndex = _uiState.value.openTabs.indexOfFirst {
            it.name == diag.fileName || it.relativePath.endsWith(diag.fileName)
        }
        if (openTabIndex != -1) {
            val tab = _uiState.value.openTabs[openTabIndex]
            val offset = lineColumnToOffset(tab.content, diag.line, diag.column)
            _uiState.update { state ->
                val updated = state.openTabs.toMutableList()
                updated[openTabIndex] = updated[openTabIndex].copy(cursorOffset = offset)
                state.copy(
                    openTabs = updated,
                    activeTabIndex = openTabIndex,
                    activeSection = IdeSection.EDITOR,
                    snackbarMessage = "Line ${diag.line}: ${diag.message}"
                )
            }
        } else if (targetNode != null) {
            openNode(targetNode)
        }
    }

    private fun lineColumnToOffset(text: String, targetLine: Int, targetCol: Int): Int {
        var line = 1
        var idx = 0
        while (idx < text.length && line < targetLine) {
            if (text[idx] == '\n') line++
            idx++
        }
        return (idx + (targetCol - 1).coerceAtLeast(0)).coerceIn(0, text.length)
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        _uiState.update { it.copy(snackbarMessage = "$label copied to clipboard") }
    }
}
