package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.editor.CppLanguageEngine
import com.example.data.local.BuildProfileEntity
import com.example.data.model.AppPlatformTarget
import com.example.data.model.CodeFileType
import com.example.data.model.CompilerChoice
import com.example.data.model.FolderLibraryItem
import com.example.data.model.GpuDriverInfo
import com.example.data.model.LinuxDistroOption
import com.example.data.model.OutputTargetType
import com.example.data.model.WorkspaceNode
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StudioTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibrariesAndBuildScreen(
    buildProfile: BuildProfileEntity,
    workspaceNodes: List<WorkspaceNode>,
    folderLibraries: List<FolderLibraryItem>,
    sharedWorkdir: String,
    gpuDriverInfo: GpuDriverInfo,
    autoGpuDrivers: Boolean,
    selectedAppPlatform: AppPlatformTarget,
    selectedLinuxDistro: LinuxDistroOption,
    onToggleAutoGpuDrivers: (Boolean) -> Unit,
    onSelectAppPlatform: (AppPlatformTarget) -> Unit,
    onSelectLinuxDistro: (LinuxDistroOption) -> Unit,
    onConfigureAppTarget: (AppPlatformTarget, LinuxDistroOption) -> Unit,
    onBuildAppPackage: () -> Unit,
    onUpdateProfile: ((BuildProfileEntity) -> BuildProfileEntity) -> Unit,
    onToggleLibraryFlag: (String) -> Unit,
    onAttachFolderLibrary: (FolderLibraryItem, String) -> Unit,
    onImportFilesFromStorage: (libName: String) -> Unit,
    onImportFolderFromStorage: (libName: String) -> Unit,
    onDownloadWithTermux: (tool: String, libName: String, target: String) -> Unit,
    onOpenCdnPanel: () -> Unit,
    onInsertHeaderDirective: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudioTheme.colors
    var showAddLibraryPanel by remember { mutableStateOf(false) }
    var storageLibName by remember { mutableStateOf("") }
    var termuxFetchTool by remember { mutableStateOf("pkg") }
    var termuxFetchName by remember { mutableStateOf("") }
    var termuxFetchTarget by remember { mutableStateOf("") }
    var showAppPackagerDetails by remember { mutableStateOf(false) }

    val binaryArtifacts = workspaceNodes.filter {
        it.fileType == CodeFileType.SHARED_OBJECT ||
            it.fileType == CodeFileType.STATIC_ARCHIVE ||
            it.fileType == CodeFileType.OBJECT_FILE ||
            it.fileType == CodeFileType.PACKAGE_ARTIFACT
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Libraries & Offline Import",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Folder: $sharedWorkdir",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryAccent
                    )
                }
                Button(
                    onClick = { showAddLibraryPanel = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryAccent,
                        contentColor = colors.background
                    ),
                    modifier = Modifier.testTag("open_add_library_panel_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Library", fontWeight = FontWeight.Bold)
                }
            }
        }

        // User Folder Libraries Summary Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.secondaryAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = colors.secondaryAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "User Folder Libraries (${folderLibraries.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.secondaryAccent
                        )
                    }
                    OutlinedButton(onClick = onOpenCdnPanel) {
                        Text("Online CDN")
                    }
                }
                Text(
                    text = "Raylib 6.2, app, and custom `.so`/`.a` or header libraries live inside `libs/`. Tap Add Library to inspect and attach flags.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted
                )
            }
        }

        // Import from Internal Storage (Offline)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.primaryAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Import from Internal Storage (Offline)",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryAccent
                )
                Text(
                    text = "No Internet needed. Import `.h`/`.hpp`, `.so`/`.a`, `.zip`, `.tar.gz`, or a whole library directory directly into `$sharedWorkdir/libs/<name>/`.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted
                )
                OutlinedTextField(
                    value = storageLibName,
                    onValueChange = { storageLibName = it },
                    label = { Text("Target Library Folder Name (optional)") },
                    placeholder = { Text("e.g. sqlite_local") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_storage_lib_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onImportFilesFromStorage(storageLibName) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.background
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_from_internal_files_btn")
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import Files", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onImportFolderFromStorage(storageLibName) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_from_internal_folder_btn")
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import Folder", maxLines = 1)
                    }
                }
            }
        }

        // Download Offline Libraries Inside Termux (pkg / curl / wget / git)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Download Library via Termux (pkg / curl / wget / git)",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = "Downloads run inside Termux with real commands (`pkg install`, `curl`, `wget`, `git clone`) and unpack into your shared `libs/` folder.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("pkg", "curl", "wget", "git").forEach { tool ->
                        FilterChip(
                            selected = termuxFetchTool == tool,
                            onClick = { termuxFetchTool = tool },
                            label = { Text(tool) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryAccent.copy(alpha = 0.2f),
                                selectedLabelColor = colors.primaryAccent
                            ),
                            modifier = Modifier.testTag("termux_fetch_tool_$tool")
                        )
                    }
                }

                if (termuxFetchTool != "pkg") {
                    OutlinedTextField(
                        value = termuxFetchName,
                        onValueChange = { termuxFetchName = it },
                        label = { Text("Folder Name inside libs/") },
                        placeholder = { Text("e.g. stb_image") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = termuxFetchTarget,
                    onValueChange = { termuxFetchTarget = it },
                    label = {
                        Text(
                            if (termuxFetchTool == "pkg") "Termux Package(s) (e.g. sdl2 libcurl sqlite)"
                            else "Archive / Header / Git URL (https://...)"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("termux_fetch_target_input")
                )

                Button(
                    onClick = {
                        onDownloadWithTermux(termuxFetchTool, termuxFetchName, termuxFetchTarget)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.secondaryAccent,
                        contentColor = colors.background
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("termux_fetch_execute_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Run `$termuxFetchTool` in Termux", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Standard User Library Row for `libs/app/` (Collapsible, multi-language Zig/ASM/Rust/C/C++)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAppPackagerDetails = !showAppPackagerDetails }
                        .testTag("app_lib_expander"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "app (libs/app/ • Package Builder)",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Links Zig, ASM, Rust, C & C++ via .o object files into .apk / .deb / .exe / .app",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    Icon(
                        imageVector = if (showAppPackagerDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle app library packaging options",
                        tint = colors.primaryAccent
                    )
                }

                AnimatedVisibility(visible = showAppPackagerDetails) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider(color = colors.border)
                        Text("Target Platform", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppPlatformTarget.entries.forEach { platform ->
                                FilterChip(
                                    selected = selectedAppPlatform == platform,
                                    onClick = { onSelectAppPlatform(platform) },
                                    label = { Text("${platform.label} (${platform.packageExt})") },
                                    modifier = Modifier.testTag("app_platform_${platform.cliArg}")
                                )
                            }
                        }

                        if (selectedAppPlatform == AppPlatformTarget.LINUX) {
                            Text("Linux Distribution", style = MaterialTheme.typography.labelLarge, color = colors.secondaryAccent)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LinuxDistroOption.entries.forEach { distro ->
                                    FilterChip(
                                        selected = selectedLinuxDistro == distro,
                                        onClick = { onSelectLinuxDistro(distro) },
                                        label = { Text(distro.label) },
                                        modifier = Modifier.testTag("linux_distro_${distro.id}")
                                    )
                                }
                            }
                        }

                        val cliPreview = if (selectedAppPlatform == AppPlatformTarget.LINUX) {
                            "app ${selectedAppPlatform.cliArg} ${selectedLinuxDistro.id} && app build"
                        } else {
                            "app ${selectedAppPlatform.cliArg} && app build"
                        }
                        Text(
                            text = "$ $cliPreview",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryAccent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.elevated)
                                .padding(10.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onConfigureAppTarget(selectedAppPlatform, selectedLinuxDistro) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_build_config_btn")
                            ) {
                                Text("app ${selectedAppPlatform.cliArg}")
                            }
                            Button(
                                onClick = onBuildAppPackage,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.secondaryAccent,
                                    contentColor = colors.background
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("app_build_package_btn")
                            ) {
                                Text("app build", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Automatic GPU API Drivers Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GPU API Drivers (Auto-Attached)",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.warning
                    )
                    Switch(
                        checked = autoGpuDrivers,
                        onCheckedChange = onToggleAutoGpuDrivers,
                        modifier = Modifier.testTag("auto_gpu_switch")
                    )
                }
                Text(
                    text = "Vendor: ${gpuDriverInfo.vendor} • ${gpuDriverInfo.renderer}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = "GLES: ${gpuDriverInfo.glesVersion} | Vulkan: ${gpuDriverInfo.vulkanVersion} (${gpuDriverInfo.recommendedTermuxDriver})",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }

        // Linker & Flags Profile Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Active Compiler & Build Flags", style = MaterialTheme.typography.titleSmall, color = colors.primaryAccent)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        CompilerChoice.AUTO,
                        CompilerChoice.CLANG,
                        CompilerChoice.CLANG_CPP,
                        CompilerChoice.GCC,
                        CompilerChoice.GPP,
                        CompilerChoice.OPENJDK
                    ).forEach { choice ->
                        FilterChip(
                            selected = buildProfile.compilerName == choice.name,
                            onClick = { onUpdateProfile { it.copy(compilerName = choice.name) } },
                            label = { Text(choice.label) }
                        )
                    }
                }

                Text("Build Output Target", style = MaterialTheme.typography.titleSmall, color = colors.primaryAccent)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutputTargetType.entries.forEach { target ->
                        FilterChip(
                            selected = buildProfile.outputType == target.name,
                            onClick = { onUpdateProfile { it.copy(outputType = target.name) } },
                            label = { Text(target.label) },
                            modifier = Modifier.testTag("target_chip_${target.name}")
                        )
                    }
                }

                OutlinedTextField(
                    value = buildProfile.outputName,
                    onValueChange = { v -> onUpdateProfile { it.copy(outputName = v) } },
                    label = { Text("Output Binary / Library Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = buildProfile.includeDirs,
                    onValueChange = { v -> onUpdateProfile { it.copy(includeDirs = v) } },
                    label = { Text("Include Directories (-I, comma-separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = buildProfile.libraryDirs,
                    onValueChange = { v -> onUpdateProfile { it.copy(libraryDirs = v) } },
                    label = { Text("Library Search Paths (-L, comma-separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = buildProfile.linkedLibs,
                    onValueChange = { v -> onUpdateProfile { it.copy(linkedLibs = v) } },
                    label = { Text("Linked Libraries (-l flags)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = buildProfile.extraFlags,
                    onValueChange = { v -> onUpdateProfile { it.copy(extraFlags = v) } },
                    label = { Text("Extra Compiler Flags") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (binaryArtifacts.isNotEmpty()) {
            item {
                Text(
                    text = "Workspace Packages, Objects & Libraries (.apk / .deb / .exe / .so / .a)",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.warning
                )
            }
            items(binaryArtifacts, key = { it.uriString }) { art ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = art.fileType.badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.warning,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(art.relativePath, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                        Text("${art.sizeBytes} bytes", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                    }
                    if (art.fileType == CodeFileType.SHARED_OBJECT || art.fileType == CodeFileType.STATIC_ARCHIVE) {
                        val libFlag = "-l:" + art.name
                        Text(
                            text = "Link $libFlag",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primaryAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleLibraryFlag(libFlag) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            HorizontalDivider(color = colors.border)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Built-in & Termux Libraries",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        }

        items(CppLanguageEngine.systemLibraries, key = { it.flag }) { lib ->
            val activeTokens = buildProfile.linkedLibs.split(Regex("\\s+")).toSet()
            val flagTokens = lib.flag.split(Regex("\\s+"))
            val isLinked = flagTokens.all { it in activeTokens }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isLinked) colors.primaryAccent.copy(alpha = 0.12f) else colors.surface)
                    .border(
                        width = 1.dp,
                        color = if (isLinked) colors.primaryAccent else colors.border,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onToggleLibraryFlag(lib.flag) }
                    .padding(12.dp)
                    .testTag("lib_toggle_${lib.flag}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(lib.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.elevated)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(lib.flag, style = MaterialTheme.typography.labelSmall, color = colors.primaryAccent)
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(lib.description, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                    Text(
                        text = "${lib.headerHint}  •  ${lib.termuxPkg}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryAccent
                    )
                }
                Icon(
                    imageVector = if (isLinked) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                    contentDescription = if (isLinked) "Linked" else "Add library flag",
                    tint = if (isLinked) colors.primaryAccent else colors.textMuted
                )
            }
        }

        item {
            HorizontalDivider(color = colors.border)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Standard & Built-in Headers (Tap to #include)",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        }

        items(CppLanguageEngine.standardHeaders.take(22), key = { it.header }) { hdr ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .clickable {
                        val inc = when (hdr.header) {
                            "raylib.h" -> "#include \"libs/raylib/include/raylib.h\""
                            "app.h" -> "#include \"libs/app/app.h\""
                            else -> "#include <${hdr.header}>"
                        }
                        onInsertHeaderDirective(inc)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "<${hdr.header}>",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(hdr.summary, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                }
                Text(
                    text = hdr.language,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryAccent
                )
            }
        }
    }

    if (showAddLibraryPanel) {
        AddLibraryPanelDialog(
            folderLibraries = folderLibraries,
            sharedWorkdir = sharedWorkdir,
            onDismiss = { showAddLibraryPanel = false },
            onAddLibrary = { selectedLib, flags ->
                onAttachFolderLibrary(selectedLib, flags)
                showAddLibraryPanel = false
            }
        )
    }
}

@Composable
fun AddLibraryPanelDialog(
    folderLibraries: List<FolderLibraryItem>,
    sharedWorkdir: String,
    onDismiss: () -> Unit,
    onAddLibrary: (FolderLibraryItem, String) -> Unit
) {
    val colors = StudioTheme.colors
    var selectedLib by remember(folderLibraries) {
        mutableStateOf(folderLibraries.firstOrNull())
    }
    var customFlags by remember(selectedLib) {
        mutableStateOf(selectedLib?.suggestedFlags ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.elevated,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textMuted,
        title = {
            Text("Add Library from User Folder")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Scanning: $sharedWorkdir/libs/",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryAccent
                )

                if (folderLibraries.isEmpty()) {
                    Text(
                        text = "No libraries found in the current user folder yet. Import from internal storage or download via Termux (pkg/curl/wget).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.warning
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 210.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(folderLibraries, key = { it.id }) { lib ->
                            val isSelected = selectedLib?.id == lib.id
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.primaryAccent.copy(alpha = 0.16f) else colors.surface)
                                    .border(
                                        1.dp,
                                        if (isSelected) colors.primaryAccent else colors.border,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedLib = lib
                                        customFlags = lib.suggestedFlags
                                    }
                                    .padding(10.dp)
                                    .testTag("add_lib_item_${lib.name}")
                            ) {
                                Text(
                                    text = lib.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) colors.primaryAccent else colors.textPrimary
                                )
                                Text(
                                    text = "${lib.relativePath} • ${lib.headerFiles.size} header(s), ${lib.binaryFiles.size} binary(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customFlags,
                        onValueChange = { customFlags = it },
                        label = { Text("Required Arguments / Compiler & Linker Flags") },
                        placeholder = { Text("-Ilibs/foo/include -Llibs/foo/lib -lfoo") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_lib_flags_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = selectedLib ?: return@Button
                    onAddLibrary(target, customFlags)
                },
                enabled = selectedLib != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryAccent,
                    contentColor = colors.background
                ),
                modifier = Modifier.testTag("confirm_add_lib_btn")
            ) {
                Text("Add to Build", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
