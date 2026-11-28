package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.GitRepoState
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostText
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.WarmAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GitSyncScreen(
    gitState: GitRepoState,
    workdirPath: String,
    onRefreshStatus: () -> Unit,
    onInitRepo: () -> Unit,
    onLoadDiff: () -> Unit,
    onCommitAll: (String) -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onCloneRepo: (String, String) -> Unit,
    onConfigureCredentials: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var commitMessage by remember { mutableStateOf("") }
    var showCloneDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianCanvas),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Git & GitHub CLI Sync",
                        style = MaterialTheme.typography.headlineSmall,
                        color = FrostText
                    )
                    Text(
                        text = "Executed via Termux git in $workdirPath",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoolSlate
                    )
                }
                OutlinedButton(
                    onClick = onRefreshStatus,
                    modifier = Modifier.testTag("git_refresh_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Git status", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Status")
                }
            }
        }

        // Repository Overview Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarbonSurface)
                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (gitState.isGitRepo) "Branch: ${gitState.branch}" else "No .git repository detected",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (gitState.isGitRepo) MintEmerald else WarmAmber
                    )
                    OutlinedButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier.testTag("git_config_btn")
                    ) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = "Configure GitHub remote", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("GitHub Setup")
                    }
                }

                if (gitState.remoteUrl.isNotBlank()) {
                    Text(
                        text = "Origin: ${gitState.remoteUrl}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                }
                Text(
                    text = gitState.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!gitState.isGitRepo) {
                        Button(
                            onClick = onInitRepo,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianCanvas),
                            modifier = Modifier.testTag("git_init_btn")
                        ) {
                            Text("git init")
                        }
                    }
                    OutlinedButton(
                        onClick = { showCloneDialog = true },
                        modifier = Modifier.testTag("git_clone_btn")
                    ) {
                        Text("Clone Repo")
                    }
                    if (gitState.isGitRepo) {
                        OutlinedButton(
                            onClick = onPull,
                            modifier = Modifier.testTag("git_pull_btn")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Pull", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Pull")
                        }
                        Button(
                            onClick = onPush,
                            colors = ButtonDefaults.buttonColors(containerColor = MintEmerald, contentColor = ObsidianCanvas),
                            modifier = Modifier.testTag("git_push_btn")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Push", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Push")
                        }
                        OutlinedButton(
                            onClick = onLoadDiff,
                            modifier = Modifier.testTag("git_diff_btn")
                        ) {
                            Icon(Icons.Default.Difference, contentDescription = "View diff", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Diff")
                        }
                    }
                }
            }
        }

        if (gitState.isGitRepo) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CarbonSurface)
                        .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Stage & Commit Changes", style = MaterialTheme.typography.titleSmall, color = FrostText)
                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        label = { Text("Commit message") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("git_commit_input")
                    )
                    Button(
                        onClick = {
                            onCommitAll(commitMessage)
                            commitMessage = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianCanvas),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("git_commit_btn")
                    ) {
                        Text("git add -A && git commit")
                    }
                }
            }
        }

        if (gitState.changes.isNotEmpty()) {
            item {
                Text(
                    text = "Changed Files (${gitState.changes.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = FrostText
                )
            }
            items(gitState.changes, key = { it.filePath }) { change ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CarbonSurface)
                        .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(WarmAmber.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = change.statusCode.trim().ifBlank { "?" },
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = change.filePath,
                        style = MaterialTheme.typography.labelMedium,
                        color = FrostText,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = change.statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = CoolSlate
                    )
                }
            }
        }

        if (gitState.diffPreview.isNotBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarbonSurface)
                        .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text("Working Tree Diff", style = MaterialTheme.typography.titleSmall, color = ElectricCyan)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = gitState.diffPreview,
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FrostText
                    )
                }
            }
        }
    }

    if (showCloneDialog) {
        var repoUrl by remember { mutableStateOf("") }
        var folder by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCloneDialog = false },
            containerColor = GraphiteElevated,
            titleContentColor = FrostText,
            title = { Text("Clone GitHub Repository") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = repoUrl,
                        onValueChange = { repoUrl = it },
                        label = { Text("HTTPS or SSH URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = folder,
                        onValueChange = { folder = it },
                        label = { Text("Target folder name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCloneRepo(repoUrl, folder)
                    showCloneDialog = false
                }) { Text("Clone") }
            },
            dismissButton = {
                TextButton(onClick = { showCloneDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showConfigDialog) {
        var name by remember { mutableStateOf(gitState.authorName) }
        var email by remember { mutableStateOf(gitState.authorEmail) }
        var remoteUrl by remember { mutableStateOf(gitState.remoteUrl) }
        var patToken by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            containerColor = GraphiteElevated,
            titleContentColor = FrostText,
            title = { Text("GitHub Identity & Remote") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("git user.name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("git user.email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        label = { Text("origin URL (https://github.com/user/repo.git)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = patToken,
                        onValueChange = { patToken = it },
                        label = { Text("GitHub PAT (saved in ~/.git-credentials)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onConfigureCredentials(name, email, remoteUrl, patToken)
                    showConfigDialog = false
                }) { Text("Save to Termux") }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) { Text("Cancel") }
            }
        )
    }
}
