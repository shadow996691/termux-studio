package com.example.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.CompilerChoice
import com.example.data.model.JobStatus
import com.example.data.model.PermissionItemState
import com.example.data.termux.TermuxBridge
import com.example.ui.theme.StudioTheme
import com.example.ui.theme.TermuxCStudioTheme
import com.example.viewmodel.IdeSection
import com.example.viewmodel.IdeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeApp(
    viewModel: IdeViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val commandHistory by viewModel.commandHistory.collectAsStateWithLifecycle()
    val cdnLibraries by viewModel.cdnLibraries.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showNewFileDialog by remember { mutableStateOf(false) }
    var pendingImportLibName by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshEnvironment()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.activeSection != IdeSection.EDITOR) {
        BackHandler {
            viewModel.selectSection(IdeSection.EDITOR)
        }
    }

    val openTermuxTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.onTermuxFolderConnected(uri)
        }
    }

    val importLibFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importOfflineLibraryFiles(pendingImportLibName, uris)
        }
    }

    val importLibFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.importOfflineLibraryFolder(pendingImportLibName, uri)
        }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissionsOnly()
    }

    val allFilesSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshEnvironment()
    }

    val launchTermuxFolderPicker: () -> Unit = {
        val initialUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DocumentsContract.buildTreeDocumentUri(
                "com.termux.documents",
                TermuxBridge.TERMUX_HOME_PATH
            )
        } else null
        openTermuxTreeLauncher.launch(initialUri)
    }

    val requestSinglePermission: (PermissionItemState) -> Unit = { item ->
        when (item.permissionKey) {
            "SAF_TERMUX_TREE" -> launchTermuxFolderPicker()
            "MANAGE_EXTERNAL_STORAGE" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    allFilesSettingsLauncher.launch(intent)
                } else {
                    permissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            }
            Manifest.permission.BLUETOOTH_CONNECT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    permissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        )
                    )
                }
            }
            else -> permissionsLauncher.launch(arrayOf(item.permissionKey))
        }
    }

    val requestAllHardwarePermissions: () -> Unit = {
        val perms = buildList {
            add(TermuxBridge.PERMISSION_RUN_COMMAND)
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        permissionsLauncher.launch(perms.toTypedArray())
    }

    TermuxCStudioTheme(
        themePreset = uiState.themePreset,
        accentPreset = uiState.accentPreset
    ) {
        val colors = StudioTheme.colors

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedLayout = maxWidth >= 720.dp

            val mainScaffold: @Composable () -> Unit = {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background),
                    containerColor = colors.background,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colors.surface,
                                titleContentColor = colors.textPrimary
                            ),
                            navigationIcon = {
                                if (!isExpandedLayout) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("open_workspace_drawer_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = stringResource(R.string.cd_workspace_drawer),
                                            tint = colors.textPrimary
                                        )
                                    }
                                }
                            },
                            title = {
                                Column {
                                    Text(
                                        text = "Termux studio",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary,
                                        maxLines = 1
                                    )
                                    val compilerLabel = runCatching {
                                        CompilerChoice.valueOf(uiState.buildProfile.compilerName).label
                                    }.getOrDefault("Auto")
                                    val langLabel = uiState.activeTab?.fileType?.languageName ?: uiState.defaultLanguage
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { viewModel.selectSection(IdeSection.COMPILER) }
                                    ) {
                                        Text(
                                            text = "$langLabel • $compilerLabel",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.primaryAccent,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            },
                            actions = {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (uiState.termuxEnv.isTermuxInstalled) {
                                                colors.secondaryAccent.copy(alpha = 0.14f)
                                            } else {
                                                colors.warning.copy(alpha = 0.14f)
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (uiState.termuxEnv.isTermuxInstalled) colors.secondaryAccent.copy(alpha = 0.45f)
                                            else colors.warning.copy(alpha = 0.45f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (uiState.termuxEnv.isTermuxInstalled) {
                                                viewModel.selectSection(IdeSection.PERMISSIONS)
                                            } else {
                                                viewModel.showTermuxSetupDialog()
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .testTag("termux_status_pill")
                                ) {
                                    Text(
                                        text = if (uiState.termuxEnv.isTermuxInstalled) {
                                            if (uiState.termuxEnv.isTermuxStudioPkgInstalled) "Termux ✓" else "Perms"
                                        } else {
                                            "Get Termux"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (uiState.termuxEnv.isTermuxInstalled) colors.secondaryAccent else colors.warning,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.selectSection(IdeSection.SETTINGS) },
                                    modifier = Modifier.testTag("toolbar_settings_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = stringResource(R.string.cd_settings),
                                        tint = if (uiState.activeSection == IdeSection.SETTINGS) colors.primaryAccent else colors.textMuted
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.selectSection(IdeSection.PERMISSIONS) },
                                    modifier = Modifier.testTag("toolbar_permissions_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Security,
                                        contentDescription = "Permissions & Setup",
                                        tint = if (uiState.activeSection == IdeSection.PERMISSIONS) colors.primaryAccent else colors.textMuted
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.compileAndRunActive(runBinaryAfterCompile = false) },
                                    modifier = Modifier.testTag("toolbar_build_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = stringResource(R.string.action_build),
                                        tint = colors.textMuted
                                    )
                                }

                                Button(
                                    onClick = { viewModel.compileAndRunActive(runBinaryAfterCompile = true) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primaryAccent,
                                        contentColor = colors.background
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .testTag("toolbar_run_btn")
                                ) {
                                    if (uiState.jobStatus == JobStatus.RUNNING) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = colors.background
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = stringResource(R.string.cd_run_code),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.action_run),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (!isExpandedLayout) {
                            NavigationBar(
                                containerColor = colors.surface,
                                modifier = Modifier.navigationBarsPadding()
                            ) {
                                CompactIdeNavItems(
                                    active = uiState.activeSection,
                                    onSelect = { section ->
                                        viewModel.selectSection(section)
                                        if (section == IdeSection.GIT && uiState.termuxEnv.isTermuxInstalled) {
                                            viewModel.refreshGitStatus()
                                        }
                                    }
                                ).forEach { nav ->
                                    NavigationBarItem(
                                        selected = uiState.activeSection == nav.section,
                                        onClick = nav.onClick,
                                        icon = { Icon(nav.icon, contentDescription = nav.label) },
                                        label = { Text(nav.label, maxLines = 1) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = colors.primaryAccent,
                                            selectedTextColor = colors.primaryAccent,
                                            indicatorColor = colors.primaryAccent.copy(alpha = 0.16f),
                                            unselectedIconColor = colors.textMuted,
                                            unselectedTextColor = colors.textMuted
                                        ),
                                        modifier = Modifier.testTag("nav_${nav.section.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .imePadding()
                    ) {
                        if (isExpandedLayout) {
                            NavigationRail(containerColor = colors.surface) {
                                ExpandedIdeNavItems(
                                    active = uiState.activeSection,
                                    onSelect = { section ->
                                        viewModel.selectSection(section)
                                        if (section == IdeSection.GIT && uiState.termuxEnv.isTermuxInstalled) {
                                            viewModel.refreshGitStatus()
                                        }
                                    }
                                ).forEach { nav ->
                                    NavigationRailItem(
                                        selected = uiState.activeSection == nav.section,
                                        onClick = nav.onClick,
                                        icon = { Icon(nav.icon, contentDescription = nav.label) },
                                        label = { Text(nav.label) },
                                        modifier = Modifier.testTag("rail_${nav.section.name.lowercase()}")
                                    )
                                }
                            }
                            VerticalDivider(color = colors.border)
                            WorkspaceDrawerContent(
                                termuxEnv = uiState.termuxEnv,
                                nodes = uiState.workspaceNodes,
                                activeFileUri = uiState.activeTab?.uriString,
                                onConnectTermuxFolder = launchTermuxFolderPicker,
                                onDisconnectFolder = viewModel::disconnectTermuxFolder,
                                onRefreshWorkspace = viewModel::refreshWorkspace,
                                onOpenNode = viewModel::openNode,
                                onDeleteNode = viewModel::deleteWorkspaceNode,
                                onRequestNewFile = { showNewFileDialog = true },
                                modifier = Modifier.width(260.dp)
                            )
                            VerticalDivider(color = colors.border)
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            when (uiState.activeSection) {
                                IdeSection.EDITOR -> {
                                    if (isExpandedLayout) {
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            EditorPane(
                                                openTabs = uiState.openTabs,
                                                activeTabIndex = uiState.activeTabIndex,
                                                suggestions = uiState.autocompleteSuggestions,
                                                fontSizeSp = uiState.editorFontSizeSp,
                                                indentWidth = uiState.indentWidth,
                                                useTabs = uiState.useTabs,
                                                autoIndent = uiState.autoIndent,
                                                showLineNumbers = uiState.showLineNumbers,
                                                wordWrap = uiState.wordWrap,
                                                compactSuggestions = uiState.compactSuggestions,
                                                showBuildDrawer = uiState.showEditorBuildDrawer,
                                                terminalEntries = uiState.terminalEntries,
                                                diagnostics = uiState.diagnostics,
                                                jobStatus = uiState.jobStatus,
                                                onSelectTab = viewModel::selectTab,
                                                onCloseTab = viewModel::closeTab,
                                                onTextChanged = viewModel::onEditorTextChanged,
                                                onApplySuggestion = viewModel::applyAutocompleteSuggestion,
                                                onDismissSuggestions = viewModel::dismissAutocompleteSuggestions,
                                                onDismissBuildDrawer = viewModel::dismissEditorBuildDrawer,
                                                onOpenTerminalTab = viewModel::openTerminalFromBuildDrawer,
                                                onJumpToDiagnostic = viewModel::jumpToDiagnostic,
                                                onUndo = viewModel::undoActiveEditor,
                                                onSave = viewModel::saveActiveTab,
                                                onFontSizeDelta = viewModel::adjustFontSize,
                                                onOpenNewFileDialog = { showNewFileDialog = true },
                                                modifier = Modifier.weight(1.25f)
                                            )
                                            VerticalDivider(color = colors.border)
                                            TerminalPanel(
                                                termuxEnv = uiState.termuxEnv,
                                                entries = uiState.terminalEntries,
                                                diagnostics = uiState.diagnostics,
                                                history = commandHistory,
                                                jobStatus = uiState.jobStatus,
                                                keepBackgroundAlive = uiState.keepBackgroundAlive,
                                                onToggleBackgroundAlive = viewModel::setKeepBackgroundAlive,
                                                onRunCommand = viewModel::runTerminalCommand,
                                                onClearTerminal = viewModel::clearTerminal,
                                                onJumpToDiagnostic = viewModel::jumpToDiagnostic,
                                                onOpenTermuxApp = {
                                                    if (!TermuxBridge.launchTermuxApp(context)) {
                                                        viewModel.showTermuxSetupDialog()
                                                    }
                                                },
                                                modifier = Modifier.weight(0.95f)
                                            )
                                        }
                                    } else {
                                        EditorPane(
                                            openTabs = uiState.openTabs,
                                            activeTabIndex = uiState.activeTabIndex,
                                            suggestions = uiState.autocompleteSuggestions,
                                            fontSizeSp = uiState.editorFontSizeSp,
                                            indentWidth = uiState.indentWidth,
                                            useTabs = uiState.useTabs,
                                            autoIndent = uiState.autoIndent,
                                            showLineNumbers = uiState.showLineNumbers,
                                            wordWrap = uiState.wordWrap,
                                            compactSuggestions = uiState.compactSuggestions,
                                            showBuildDrawer = uiState.showEditorBuildDrawer,
                                            terminalEntries = uiState.terminalEntries,
                                            diagnostics = uiState.diagnostics,
                                            jobStatus = uiState.jobStatus,
                                            onSelectTab = viewModel::selectTab,
                                            onCloseTab = viewModel::closeTab,
                                            onTextChanged = viewModel::onEditorTextChanged,
                                            onApplySuggestion = viewModel::applyAutocompleteSuggestion,
                                            onDismissSuggestions = viewModel::dismissAutocompleteSuggestions,
                                            onDismissBuildDrawer = viewModel::dismissEditorBuildDrawer,
                                            onOpenTerminalTab = viewModel::openTerminalFromBuildDrawer,
                                            onJumpToDiagnostic = viewModel::jumpToDiagnostic,
                                            onUndo = viewModel::undoActiveEditor,
                                            onSave = viewModel::saveActiveTab,
                                            onFontSizeDelta = viewModel::adjustFontSize,
                                            onOpenNewFileDialog = { showNewFileDialog = true }
                                        )
                                    }
                                }
                                IdeSection.COMPILER -> CompilerTabScreen(
                                    activePanel = uiState.activeCompilerPanel,
                                    onSelectPanel = viewModel::selectCompilerPanel,
                                    termuxEnv = uiState.termuxEnv,
                                    compilerProbeMap = uiState.compilerProbeMap,
                                    buildProfile = uiState.buildProfile,
                                    activeTab = uiState.activeTab,
                                    compileCommandPreview = viewModel.previewActiveCompileCommand(true),
                                    githubAptRepoUrl = uiState.githubAptRepoUrl,
                                    cdnLibraries = cdnLibraries,
                                    onProbeCompilers = viewModel::probeTermuxCompilers,
                                    onStartListener = viewModel::startTermuxListenerDaemon,
                                    onInstallCompilers = viewModel::installTermuxCompilers,
                                    onInstallLocalDeb = viewModel::installTermuxStudioLocalDeb,
                                    onInstallFromGitHubApt = viewModel::installTermuxStudioFromGitHubApt,
                                    onRegenerateAptRepo = viewModel::regenerateAptRepoBundle,
                                    onCopyText = viewModel::copyToClipboard,
                                    onUpdateBuildProfile = viewModel::updateBuildProfile,
                                    onCompileAndRun = { runAfter ->
                                        viewModel.compileAndRunActive(
                                            runBinaryAfterCompile = runAfter,
                                            stayInEditorDrawer = false
                                        )
                                    },
                                    onRegisterOnlineCdnLib = viewModel::registerOnlineCdnLibrary,
                                    onToggleCdnLibEnabled = viewModel::toggleCdnLibraryEnabled,
                                    onAttachCdnFlags = { flags ->
                                        viewModel.toggleLibraryFlag(flags)
                                        viewModel.selectSection(IdeSection.LIBRARIES)
                                    },
                                    onDeleteCdnLib = viewModel::deleteCdnLibraryRecord
                                )
                                IdeSection.TERMINAL -> TerminalPanel(
                                    termuxEnv = uiState.termuxEnv,
                                    entries = uiState.terminalEntries,
                                    diagnostics = uiState.diagnostics,
                                    history = commandHistory,
                                    jobStatus = uiState.jobStatus,
                                    keepBackgroundAlive = uiState.keepBackgroundAlive,
                                    onToggleBackgroundAlive = viewModel::setKeepBackgroundAlive,
                                    onRunCommand = viewModel::runTerminalCommand,
                                    onClearTerminal = viewModel::clearTerminal,
                                    onJumpToDiagnostic = viewModel::jumpToDiagnostic,
                                    onOpenTermuxApp = {
                                        if (!TermuxBridge.launchTermuxApp(context)) {
                                            viewModel.showTermuxSetupDialog()
                                        }
                                    }
                                )
                                IdeSection.GIT -> GitSyncScreen(
                                    gitState = uiState.gitState,
                                    workdirPath = uiState.termuxEnv.resolvedTermuxWorkdir,
                                    onRefreshStatus = viewModel::refreshGitStatus,
                                    onInitRepo = viewModel::gitInitRepo,
                                    onLoadDiff = viewModel::loadGitDiff,
                                    onCommitAll = viewModel::gitCommitAll,
                                    onPull = viewModel::gitPull,
                                    onPush = viewModel::gitPush,
                                    onCloneRepo = viewModel::gitCloneRepo,
                                    onConfigureCredentials = viewModel::configureGitHubCredentials
                                )
                                IdeSection.LIBRARIES -> LibrariesAndBuildScreen(
                                    buildProfile = uiState.buildProfile,
                                    workspaceNodes = uiState.workspaceNodes,
                                    folderLibraries = uiState.folderLibraries,
                                    sharedWorkdir = uiState.termuxEnv.resolvedTermuxWorkdir,
                                    gpuDriverInfo = uiState.gpuDriverInfo,
                                    autoGpuDrivers = uiState.autoGpuDrivers,
                                    selectedAppPlatform = uiState.selectedAppPlatform,
                                    selectedLinuxDistro = uiState.selectedLinuxDistro,
                                    onToggleAutoGpuDrivers = viewModel::setAutoGpuDrivers,
                                    onSelectAppPlatform = viewModel::selectAppPlatform,
                                    onSelectLinuxDistro = viewModel::selectLinuxDistro,
                                    onConfigureAppTarget = viewModel::configureAppTarget,
                                    onBuildAppPackage = viewModel::buildAppPackage,
                                    onUpdateProfile = viewModel::updateBuildProfile,
                                    onToggleLibraryFlag = viewModel::toggleLibraryFlag,
                                    onAttachFolderLibrary = viewModel::attachLibraryWithCustomFlags,
                                    onImportFilesFromStorage = { libName ->
                                        pendingImportLibName = libName
                                        importLibFilesLauncher.launch(arrayOf("*/*"))
                                    },
                                    onImportFolderFromStorage = { libName ->
                                        pendingImportLibName = libName
                                        importLibFolderLauncher.launch(null)
                                    },
                                    onDownloadWithTermux = viewModel::downloadLibraryWithTermux,
                                    onOpenCdnPanel = {
                                        viewModel.selectSection(IdeSection.COMPILER)
                                        viewModel.selectCompilerPanel(2)
                                    },
                                    onInsertHeaderDirective = { directive ->
                                        val tab = uiState.activeTab
                                        if (tab != null && !tab.content.contains(directive)) {
                                            val updated = "$directive\n${tab.content}"
                                            viewModel.onEditorTextChanged(updated, directive.length + 1)
                                            viewModel.selectSection(IdeSection.EDITOR)
                                        }
                                    }
                                )
                                IdeSection.PERMISSIONS -> PermissionsScreen(
                                    termuxEnv = uiState.termuxEnv,
                                    gpuDriverInfo = uiState.gpuDriverInfo,
                                    permissions = uiState.permissions,
                                    onRequestPermission = requestSinglePermission,
                                    onRequestAllHardwarePermissions = requestAllHardwarePermissions,
                                    onDownloadTermuxFDroid = { TermuxBridge.openTermuxFDroidPage(context) },
                                    onDownloadTermuxApiFDroid = {
                                        TermuxBridge.openTermuxFDroidPage(context, TermuxBridge.FDROID_TERMUX_API_URL)
                                    },
                                    onDownloadTermuxX11 = {
                                        TermuxBridge.openTermuxFDroidPage(context, TermuxBridge.TERMUX_X11_URL)
                                    },
                                    onCopyBootstrap = {
                                        viewModel.copyToClipboard("Termux setup", TermuxBridge.BOOTSTRAP_SETUP_COMMAND)
                                    }
                                )
                                IdeSection.SETTINGS -> SettingsScreen(
                                    uiState = uiState,
                                    onSelectTheme = viewModel::updateThemePreset,
                                    onSelectAccent = viewModel::updateAccentPreset,
                                    onFontSizeChanged = { nextSize ->
                                        viewModel.adjustFontSize(nextSize - uiState.editorFontSizeSp)
                                    },
                                    onIndentWidthChanged = viewModel::updateIndentWidth,
                                    onSetUseTabs = viewModel::setUseTabs,
                                    onSetAutoIndent = viewModel::setAutoIndent,
                                    onSetShowLineNumbers = viewModel::setShowLineNumbers,
                                    onSetWordWrap = viewModel::setWordWrap,
                                    onSetCompactSuggestions = viewModel::setCompactSuggestions,
                                    onUpdateCompilerOptLevel = viewModel::updateCompilerOptLevel,
                                    onUpdateBuildProfile = viewModel::updateBuildProfile,
                                    onSetKeepBackgroundAlive = viewModel::setKeepBackgroundAlive,
                                    onSetDefaultLanguage = viewModel::updateDefaultLanguage,
                                    onToggleEnabledLanguage = viewModel::toggleEnabledLanguage
                                )
                            }
                        }
                    }
                }
            }

            if (isExpandedLayout) {
                mainScaffold()
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = colors.surface,
                            modifier = Modifier.width(300.dp)
                        ) {
                            WorkspaceDrawerContent(
                                termuxEnv = uiState.termuxEnv,
                                nodes = uiState.workspaceNodes,
                                activeFileUri = uiState.activeTab?.uriString,
                                onConnectTermuxFolder = {
                                    scope.launch { drawerState.close() }
                                    launchTermuxFolderPicker()
                                },
                                onDisconnectFolder = viewModel::disconnectTermuxFolder,
                                onRefreshWorkspace = viewModel::refreshWorkspace,
                                onOpenNode = { node ->
                                    viewModel.openNode(node)
                                    if (node.fileType.isEditableText) {
                                        scope.launch { drawerState.close() }
                                    }
                                },
                                onDeleteNode = viewModel::deleteWorkspaceNode,
                                onRequestNewFile = {
                                    scope.launch { drawerState.close() }
                                    showNewFileDialog = true
                                }
                            )
                        }
                    }
                ) {
                    mainScaffold()
                }
            }
        }

        if (showNewFileDialog) {
            NewCppFileDialog(
                onDismiss = { showNewFileDialog = false },
                onCreate = { relName, template -> viewModel.createNewFile(relName, template) }
            )
        }

        if (uiState.showTermuxMissingDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissTermuxMissingDialog,
                containerColor = colors.elevated,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textMuted,
                title = { Text(stringResource(R.string.termux_missing_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.termux_missing_body))
                        HorizontalDivider(color = colors.border)
                        Text(
                            text = "After installing Termux from F-Droid, run this once in Termux:",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primaryAccent
                        )
                        Text(
                            text = TermuxBridge.BOOTSTRAP_SETUP_COMMAND,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.background)
                                .padding(8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            TermuxBridge.openTermuxFDroidPage(context)
                            viewModel.dismissTermuxMissingDialog()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.background
                        ),
                        modifier = Modifier.testTag("dialog_download_fdroid_btn")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.termux_download_fdroid))
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.copyToClipboard("Termux setup", TermuxBridge.BOOTSTRAP_SETUP_COMMAND)
                            }
                        ) {
                            Text("Copy Setup")
                        }
                        TextButton(onClick = viewModel::dismissTermuxMissingDialog) {
                            Text("Later")
                        }
                    }
                }
            )
        }
    }
}

private data class NavItemSpec(
    val section: IdeSection,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun CompactIdeNavItems(
    active: IdeSection,
    onSelect: (IdeSection) -> Unit
): List<NavItemSpec> = listOf(
    NavItemSpec(
        section = IdeSection.EDITOR,
        label = stringResource(R.string.nav_editor),
        icon = if (active == IdeSection.EDITOR) Icons.Default.Code else Icons.Outlined.Code,
        onClick = { onSelect(IdeSection.EDITOR) }
    ),
    NavItemSpec(
        section = IdeSection.COMPILER,
        label = stringResource(R.string.nav_compiler),
        icon = if (active == IdeSection.COMPILER) Icons.Default.SettingsApplications else Icons.Outlined.SettingsApplications,
        onClick = { onSelect(IdeSection.COMPILER) }
    ),
    NavItemSpec(
        section = IdeSection.TERMINAL,
        label = stringResource(R.string.nav_terminal),
        icon = if (active == IdeSection.TERMINAL) Icons.Default.Terminal else Icons.Outlined.Terminal,
        onClick = { onSelect(IdeSection.TERMINAL) }
    ),
    NavItemSpec(
        section = IdeSection.LIBRARIES,
        label = stringResource(R.string.nav_libs),
        icon = if (active == IdeSection.LIBRARIES) Icons.Default.Extension else Icons.Outlined.Extension,
        onClick = { onSelect(IdeSection.LIBRARIES) }
    ),
    NavItemSpec(
        section = IdeSection.GIT,
        label = stringResource(R.string.nav_git),
        icon = Icons.Outlined.AccountTree,
        onClick = { onSelect(IdeSection.GIT) }
    )
)

@Composable
private fun ExpandedIdeNavItems(
    active: IdeSection,
    onSelect: (IdeSection) -> Unit
): List<NavItemSpec> = CompactIdeNavItems(active, onSelect) + listOf(
    NavItemSpec(
        section = IdeSection.PERMISSIONS,
        label = stringResource(R.string.nav_permissions),
        icon = if (active == IdeSection.PERMISSIONS) Icons.Default.Security else Icons.Outlined.Security,
        onClick = { onSelect(IdeSection.PERMISSIONS) }
    ),
    NavItemSpec(
        section = IdeSection.SETTINGS,
        label = stringResource(R.string.nav_settings),
        icon = if (active == IdeSection.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings,
        onClick = { onSelect(IdeSection.SETTINGS) }
    )
)
