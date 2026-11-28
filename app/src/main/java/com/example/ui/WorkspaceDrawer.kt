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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CodeFileType
import com.example.data.model.TermuxEnvironmentState
import com.example.data.model.WorkspaceNode
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostText
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SoftViolet
import com.example.ui.theme.WarmAmber

@Composable
fun WorkspaceDrawerContent(
    termuxEnv: TermuxEnvironmentState,
    nodes: List<WorkspaceNode>,
    activeFileUri: String?,
    onConnectTermuxFolder: () -> Unit,
    onDisconnectFolder: () -> Unit,
    onRefreshWorkspace: () -> Unit,
    onOpenNode: (WorkspaceNode) -> Unit,
    onDeleteNode: (WorkspaceNode) -> Unit,
    onRequestNewFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(CarbonSurface)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "WORKSPACE",
                style = MaterialTheme.typography.titleSmall,
                color = FrostText,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(
                    onClick = onRefreshWorkspace,
                    modifier = Modifier.testTag("workspace_refresh_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh workspace", tint = CoolSlate)
                }
                IconButton(
                    onClick = onRequestNewFile,
                    modifier = Modifier.testTag("workspace_new_file_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_file), tint = ElectricCyan)
                }
            }
        }

        // Termux folder connection card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(GraphiteElevated)
                .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            val isSaf = termuxEnv.connectedSafTreeUri != null
            val isShared = termuxEnv.isSharedStorageGranted
            Text(
                text = when {
                    isSaf -> "Termux SAF Tree Connected"
                    isShared -> "Shared Termux Folder Active"
                    else -> "Shared Storage Needed"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (isSaf || isShared) MintEmerald else WarmAmber
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    isSaf -> termuxEnv.resolvedTermuxWorkdir
                    isShared -> "${termuxEnv.resolvedTermuxWorkdir} ↔ ~/TermuxStudio (Raylib 6.2 & App inside libs/)"
                    else -> "Grant All Files Access so Termux studio & Termux share /sdcard/TermuxStudio directly, or open via SAF picker."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = CoolSlate,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onConnectTermuxFolder,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = ObsidianCanvas
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("connect_termux_folder_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = stringResource(R.string.cd_open_termux_folder),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSaf) "Switch SAF" else "Browse Termux SAF")
                }
                if (isSaf) {
                    IconButton(
                        onClick = onDisconnectFolder,
                        modifier = Modifier.testTag("disconnect_termux_folder_btn")
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = "Use /sdcard/TermuxStudio shared folder", tint = CoolSlate)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = SlateBorder)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(nodes, key = { it.uriString }) { node ->
                val isSelected = node.uriString == activeFileUri
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) ElectricCyan.copy(alpha = 0.14f) else CarbonSurface)
                        .clickable(enabled = !node.isDirectory) { onOpenNode(node) }
                        .padding(
                            start = (8 + node.depth * 14).dp,
                            end = 4.dp,
                            top = 7.dp,
                            bottom = 7.dp
                        )
                        .testTag("workspace_node_${node.name}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (node.isDirectory) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = WarmAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        val badgeColor = when (node.fileType) {
                            CodeFileType.C_SOURCE, CodeFileType.GO, CodeFileType.ZIG -> ElectricCyan
                            CodeFileType.CPP_SOURCE, CodeFileType.RUST, CodeFileType.KOTLIN -> SoftViolet
                            CodeFileType.HEADER, CodeFileType.BUILD_CONFIG -> MintEmerald
                            CodeFileType.JAVA, CodeFileType.ASSEMBLY, CodeFileType.SHARED_OBJECT,
                            CodeFileType.STATIC_ARCHIVE, CodeFileType.OBJECT_FILE, CodeFileType.PACKAGE_ARTIFACT -> WarmAmber
                            CodeFileType.MAKEFILE -> CoralError
                            else -> CoolSlate
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.16f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = node.fileType.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = node.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) FrostText else FrostText.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!node.isDirectory) {
                        IconButton(
                            onClick = { onDeleteNode(node) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete ${node.name}",
                                tint = CoolSlate,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewCppFileDialog(
    onDismiss: () -> Unit,
    onCreate: (relativeName: String, content: String) -> Unit
) {
    var selectedKind by remember { mutableStateOf("cpp") }
    var fileName by remember { mutableStateOf("module.cpp") }

    val templateCode = remember(selectedKind, fileName) {
        when (selectedKind) {
            "c" -> """
                #include <stdio.h>
                #include "libs/app/app.h"

                int main(void) {
                    AppConfig cfg = app_default_config("CProgram");
                    app_init(&cfg);
                    printf("Hello from C with GPU: %s\n", app_get_gpu_backend());
                    app_shutdown();
                    return 0;
                }
            """.trimIndent() + "\n"
            "h" -> """
                #pragma once

                #ifdef __cplusplus
                extern "C" {
                #endif

                int compute_value(int x);

                #ifdef __cplusplus
                }
                #endif
            """.trimIndent() + "\n"
            "java" -> """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello from Java in Termux studio!");
                    }
                }
            """.trimIndent() + "\n"
            "rs" -> """
                fn main() {
                    println!("Hello from Rust in Termux studio!");
                }
            """.trimIndent() + "\n"
            "go" -> """
                package main

                import "fmt"

                func main() {
                    fmt.Println("Hello from Go in Termux studio!")
                }
            """.trimIndent() + "\n"
            "zig" -> """
                const std = @import("std");

                pub fn main() !void {
                    std.debug.print("Hello from Zig in Termux studio!\n", .{});
                }
            """.trimIndent() + "\n"
            "asm" -> """
                .global main
                .text
                main:
                    mov w0, #0
                    ret
            """.trimIndent() + "\n"
            "f90" -> """
                program hello
                    print *, "Hello from Fortran in Termux studio!"
                end program hello
            """.trimIndent() + "\n"
            else -> """
                #include <iostream>

                int main() {
                    std::cout << "Hello from C++ in Termux studio!" << std::endl;
                    return 0;
                }
            """.trimIndent() + "\n"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphiteElevated,
        titleContentColor = FrostText,
        textContentColor = CoolSlate,
        title = { Text("New Source / Header File") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "cpp" to "C++ (.cpp)" to "module.cpp",
                        "c" to "C (.c)" to "module.c",
                        "h" to "Header (.h)" to "include/utils.h",
                        "java" to "Java (.java)" to "Main.java",
                        "rs" to "Rust (.rs)" to "main.rs",
                        "go" to "Go (.go)" to "main.go",
                        "zig" to "Zig (.zig)" to "main.zig",
                        "asm" to "ASM (.s)" to "boot.s",
                        "f90" to "Fortran" to "main.f90"
                    ).forEach { (kindLabel, defaultName) ->
                        val (kind, label) = kindLabel
                        FilterChip(
                            selected = selectedKind == kind,
                            onClick = {
                                selectedKind = kind
                                fileName = defaultName
                            },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Relative path (e.g. Main.java or src/app.rs)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_file_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileName.isNotBlank()) {
                        onCreate(fileName.trim(), templateCode)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_create_file_btn")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
