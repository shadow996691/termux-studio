package com.example.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.example.data.local.BuildProfileEntity
import com.example.data.local.CdnLibraryEntity
import com.example.data.model.CompilerChoice
import com.example.data.model.CompilerProbeStatus
import com.example.data.model.OpenEditorTab
import com.example.data.model.OutputTargetType
import com.example.data.model.TermuxEnvironmentState
import com.example.data.termux.TermuxBridge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StudioTheme

@Composable
fun CompilerTabScreen(
    activePanel: Int,
    onSelectPanel: (Int) -> Unit,
    termuxEnv: TermuxEnvironmentState,
    compilerProbeMap: Map<String, CompilerProbeStatus>,
    buildProfile: BuildProfileEntity,
    activeTab: OpenEditorTab?,
    compileCommandPreview: String,
    githubAptRepoUrl: String,
    cdnLibraries: List<CdnLibraryEntity>,
    onProbeCompilers: () -> Unit,
    onStartListener: () -> Unit,
    onInstallCompilers: (List<String>) -> Unit,
    onInstallLocalDeb: () -> Unit,
    onInstallFromGitHubApt: (String) -> Unit,
    onRegenerateAptRepo: () -> Unit,
    onCopyText: (label: String, text: String) -> Unit,
    onUpdateBuildProfile: ((BuildProfileEntity) -> BuildProfileEntity) -> Unit,
    onCompileAndRun: (runAfter: Boolean) -> Unit,
    onRegisterOnlineCdnLib: (name: String, url: String, flags: String) -> Unit,
    onToggleCdnLibEnabled: (CdnLibraryEntity) -> Unit,
    onAttachCdnFlags: (String) -> Unit,
    onDeleteCdnLib: (CdnLibraryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudioTheme.colors
    val panelTitles = listOf("Install", "Choose", "CDN Libs")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Termux Compiler Hub",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary
        )
        Text(
            text = "Shared Folder: ${termuxEnv.resolvedTermuxWorkdir}",
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.labelSmall,
            color = colors.secondaryAccent
        )
        Spacer(Modifier.height(10.dp))

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("compiler_panel_switcher")
        ) {
            panelTitles.forEachIndexed { idx, label ->
                SegmentedButton(
                    selected = activePanel == idx,
                    onClick = { onSelectPanel(idx) },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = panelTitles.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.primaryAccent.copy(alpha = 0.2f),
                        activeContentColor = colors.primaryAccent,
                        inactiveContainerColor = colors.surface,
                        inactiveContentColor = colors.textMuted
                    ),
                    modifier = Modifier.testTag("compiler_panel_btn_$idx")
                ) {
                    Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when (activePanel) {
            0 -> CompilerInstallPanel(
                termuxEnv = termuxEnv,
                compilerProbeMap = compilerProbeMap,
                githubAptRepoUrl = githubAptRepoUrl,
                onProbeCompilers = onProbeCompilers,
                onStartListener = onStartListener,
                onInstallCompilers = onInstallCompilers,
                onInstallLocalDeb = onInstallLocalDeb,
                onInstallFromGitHubApt = onInstallFromGitHubApt,
                onRegenerateAptRepo = onRegenerateAptRepo,
                onCopyText = onCopyText,
                modifier = Modifier.weight(1f)
            )
            1 -> ChooseCompilerPanel(
                buildProfile = buildProfile,
                activeTab = activeTab,
                compileCommandPreview = compileCommandPreview,
                compilerProbeMap = compilerProbeMap,
                onUpdateBuildProfile = onUpdateBuildProfile,
                onCompileAndRun = onCompileAndRun,
                modifier = Modifier.weight(1f)
            )
            else -> CdnLibrariesPanel(
                cdnLibraries = cdnLibraries,
                onRegisterOnlineCdnLib = onRegisterOnlineCdnLib,
                onToggleCdnLibEnabled = onToggleCdnLibEnabled,
                onAttachCdnFlags = onAttachCdnFlags,
                onDeleteCdnLib = onDeleteCdnLib,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompilerInstallPanel(
    termuxEnv: TermuxEnvironmentState,
    compilerProbeMap: Map<String, CompilerProbeStatus>,
    githubAptRepoUrl: String,
    onProbeCompilers: () -> Unit,
    onStartListener: () -> Unit,
    onInstallCompilers: (List<String>) -> Unit,
    onInstallLocalDeb: () -> Unit,
    onInstallFromGitHubApt: (String) -> Unit,
    onRegenerateAptRepo: () -> Unit,
    onCopyText: (label: String, text: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudioTheme.colors
    var selectedPkgIds by remember { mutableStateOf(setOf("clang", "gcc", "javac")) }
    var repoUrlDraft by remember(githubAptRepoUrl) { mutableStateOf(githubAptRepoUrl) }

    val selectedPackages = TermuxBridge.compilerCatalog
        .filter { it.id in selectedPkgIds }
        .map { it.termuxPkg }
        .distinct()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Termux Package: termux-studio v1.0.0",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryAccent
                        )
                        Text(
                            text = if (termuxEnv.isTermuxStudioPkgInstalled) {
                                "Installed in Termux • Listener synced"
                            } else {
                                "Ready in termux-repo/ (.deb + GitHub APT repo)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (termuxEnv.isTermuxStudioPkgInstalled) colors.secondaryAccent else colors.warning
                        )
                    }
                    IconButton(
                        onClick = onProbeCompilers,
                        modifier = Modifier.testTag("probe_compilers_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Probe Termux compilers", tint = colors.primaryAccent)
                    }
                }

                Text(
                    text = "Includes live CDN resolve, pkg/curl/wget/git library fetchers, and multi-language App object linking. Install local `.deb` or push `termux-repo/` to GitHub Pages.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted
                )

                OutlinedTextField(
                    value = repoUrlDraft,
                    onValueChange = { repoUrlDraft = it },
                    label = { Text("GitHub Pages APT Repo URL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("github_apt_repo_input")
                )

                val aptCmd = "echo \"deb [trusted=yes] ${repoUrlDraft.trim().removeSuffix("/")} stable main\" > \$PREFIX/etc/apt/sources.list.d/termux-studio.list && pkg update -y && pkg install -y termux-studio"
                Text(
                    text = aptCmd,
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryAccent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.elevated)
                        .padding(8.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onInstallLocalDeb,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.secondaryAccent,
                            contentColor = colors.background
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("install_local_deb_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Install .deb Now", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onInstallFromGitHubApt(repoUrlDraft) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("install_github_apt_btn")
                    ) {
                        Text("pkg install APT", maxLines = 1)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onStartListener,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_listener_btn")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sync Listener", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onCopyText("APT install", aptCmd) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy APT", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onRegenerateAptRepo,
                        modifier = Modifier.testTag("regen_apt_repo_btn")
                    ) {
                        Text("Rebuild Repo", maxLines = 1)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Install Compilers in Termux",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Button(
                    onClick = { onInstallCompilers(selectedPackages) },
                    enabled = selectedPackages.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryAccent,
                        contentColor = colors.background
                    ),
                    modifier = Modifier.testTag("install_selected_compilers_btn")
                ) {
                    Text("Install Selected (${selectedPackages.size})", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(TermuxBridge.compilerCatalog, key = { it.id }) { spec ->
            val probe = compilerProbeMap[spec.id]
            val isInstalled = probe?.isInstalled == true
            val isChecked = spec.id in selectedPkgIds

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .border(
                        width = 1.dp,
                        color = if (isInstalled) colors.secondaryAccent.copy(alpha = 0.5f) else colors.border,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        selectedPkgIds = if (isChecked) selectedPkgIds - spec.id else selectedPkgIds + spec.id
                    }
                    .padding(10.dp)
                    .testTag("compiler_install_row_${spec.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { checked ->
                        selectedPkgIds = if (checked) selectedPkgIds + spec.id else selectedPkgIds - spec.id
                    },
                    colors = CheckboxDefaults.colors(checkedColor = colors.primaryAccent)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(spec.displayName, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isInstalled) colors.secondaryAccent.copy(alpha = 0.16f)
                                    else colors.elevated
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isInstalled) "Installed" else "pkg: ${spec.termuxPkg}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isInstalled) colors.secondaryAccent else colors.warning
                            )
                        }
                    }
                    Text(
                        text = if (isInstalled && probe?.versionLine?.isNotBlank() == true) {
                            probe.versionLine
                        } else {
                            spec.description
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                        maxLines = 2
                    )
                }
                Spacer(Modifier.width(6.dp))
                OutlinedButton(
                    onClick = { onInstallCompilers(listOf(spec.termuxPkg)) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("install_single_${spec.id}")
                ) {
                    Text(if (isInstalled) "Update" else "Install")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChooseCompilerPanel(
    buildProfile: BuildProfileEntity,
    activeTab: OpenEditorTab?,
    compileCommandPreview: String,
    compilerProbeMap: Map<String, CompilerProbeStatus>,
    onUpdateBuildProfile: ((BuildProfileEntity) -> BuildProfileEntity) -> Unit,
    onCompileAndRun: (runAfter: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudioTheme.colors
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    text = "Choose Active Compiler",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryAccent
                )
                Text(
                    text = "Active File: ${activeTab?.relativePath ?: "None opened"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompilerChoice.entries.forEach { choice ->
                        val isSelected = buildProfile.compilerName == choice.name
                        val probeKey = when (choice) {
                            CompilerChoice.CLANG -> "clang"
                            CompilerChoice.CLANG_CPP -> "clangpp"
                            CompilerChoice.GCC -> "gcc"
                            CompilerChoice.GPP -> "gpp"
                            CompilerChoice.OPENJDK -> "javac"
                            CompilerChoice.RUST -> "rustc"
                            CompilerChoice.GO -> "go"
                            CompilerChoice.ZIG -> "zig"
                            CompilerChoice.NASM -> "nasm"
                            CompilerChoice.GFORTRAN -> "gfortran"
                            CompilerChoice.KOTLIN -> "kotlinc"
                            CompilerChoice.AUTO -> "clang"
                        }
                        val ready = compilerProbeMap[probeKey]?.isInstalled == true
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdateBuildProfile { it.copy(compilerName = choice.name) } },
                            leadingIcon = if (ready) {
                                {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Detected in Termux",
                                        tint = colors.secondaryAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            label = { Text(choice.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryAccent.copy(alpha = 0.2f),
                                selectedLabelColor = colors.primaryAccent
                            ),
                            modifier = Modifier.testTag("choose_compiler_${choice.name}")
                        )
                    }
                }

                HorizontalDivider(color = colors.border)
                Text("Output Format", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutputTargetType.entries.forEach { target ->
                        FilterChip(
                            selected = buildProfile.outputType == target.name,
                            onClick = { onUpdateBuildProfile { it.copy(outputType = target.name) } },
                            label = { Text(target.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.secondaryAccent.copy(alpha = 0.2f),
                                selectedLabelColor = colors.secondaryAccent
                            )
                        )
                    }
                }

                Text("C / C++ Standard", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("c11", "c17", "c23").forEach { std ->
                        FilterChip(
                            selected = buildProfile.cStandard == std,
                            onClick = { onUpdateBuildProfile { it.copy(cStandard = std) } },
                            label = { Text(std) }
                        )
                    }
                    listOf("c++17", "c++20", "c++23").forEach { std ->
                        FilterChip(
                            selected = buildProfile.cppStandard == std,
                            onClick = { onUpdateBuildProfile { it.copy(cppStandard = std) } },
                            label = { Text(std) }
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.secondaryAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = colors.secondaryAccent)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Termux Execution Command",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.secondaryAccent
                    )
                }
                Text(
                    text = "$ $compileCommandPreview",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.elevated)
                        .padding(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onCompileAndRun(false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("compiler_tab_build_only_btn")
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Compile Only")
                    }
                    Button(
                        onClick = { onCompileAndRun(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.background
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("compiler_tab_compile_run_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Compile & Run", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CdnLibrariesPanel(
    cdnLibraries: List<CdnLibraryEntity>,
    onRegisterOnlineCdnLib: (name: String, url: String, flags: String) -> Unit,
    onToggleCdnLibEnabled: (CdnLibraryEntity) -> Unit,
    onAttachCdnFlags: (String) -> Unit,
    onDeleteCdnLib: (CdnLibraryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudioTheme.colors
    var libName by remember { mutableStateOf("") }
    var libUrl by remember { mutableStateOf("") }
    var libFlags by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = colors.primaryAccent)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "CDN Online Library Providers",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryAccent
                    )
                }
                Text(
                    text = "Online-only URL/API links — never saved permanently offline. When toggled active below, Wi-Fi / Internet is required at compile time so Termux fetches headers & binaries live.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted
                )

                OutlinedTextField(
                    value = libName,
                    onValueChange = { libName = it },
                    label = { Text("CDN Provider Name (e.g. nlohmann_json)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cdn_lib_name_input")
                )

                OutlinedTextField(
                    value = libUrl,
                    onValueChange = { libUrl = it },
                    label = { Text("Online Library API or URL (https://...)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cdn_lib_url_input")
                )

                OutlinedTextField(
                    value = libFlags,
                    onValueChange = { libFlags = it },
                    label = { Text("Optional Compiler / Linker Flags (e.g. -lfoo)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cdn_lib_flags_input")
                )

                Button(
                    onClick = {
                        onRegisterOnlineCdnLib(libName, libUrl, libFlags)
                        libName = ""
                        libUrl = ""
                        libFlags = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryAccent,
                        contentColor = colors.background
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cdn_download_btn")
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Online CDN Provider", fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Tip: For offline libraries without Internet, use Libs -> Import from Storage or Download via Termux (pkg/curl/wget).",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryAccent
                )
            }
        }

        item {
            Text(
                text = "Registered Online CDN Libraries (${cdnLibraries.size})",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        }

        if (cdnLibraries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "No online CDN URLs configured yet. Add an API or library header/archive URL above to resolve live during compilation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted
                    )
                }
            }
        } else {
            items(cdnLibraries, key = { it.name }) { cdn ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .border(
                            1.dp,
                            if (cdn.isEnabled) colors.primaryAccent.copy(alpha = 0.55f) else colors.border,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cdn.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text(
                            text = if (cdn.isEnabled) "Live at compile time (Wi-Fi/Internet required)" else "Paused for builds",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cdn.isEnabled) colors.warning else colors.textMuted
                        )
                        if (cdn.compilerFlags.isNotBlank()) {
                            Text(cdn.compilerFlags, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, color = colors.primaryAccent)
                        }
                        Text(cdn.url, style = MaterialTheme.typography.bodySmall, color = colors.textMuted, maxLines = 1)
                    }
                    Switch(
                        checked = cdn.isEnabled,
                        onCheckedChange = { onToggleCdnLibEnabled(cdn) },
                        modifier = Modifier.testTag("cdn_toggle_${cdn.name}")
                    )
                    if (cdn.compilerFlags.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        OutlinedButton(
                            onClick = { onAttachCdnFlags(cdn.compilerFlags) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Flags")
                        }
                    }
                    IconButton(onClick = { onDeleteCdnLib(cdn) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete CDN entry", tint = colors.error)
                    }
                }
            }
        }
    }
}
