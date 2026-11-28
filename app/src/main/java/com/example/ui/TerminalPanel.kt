package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CommandHistoryEntity
import com.example.data.model.CompilerDiagnostic
import com.example.data.model.JobStatus
import com.example.data.model.TerminalEntry
import com.example.data.model.TermuxEnvironmentState
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostText
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.WarmAmber

private val QUICK_TERMUX_COMMANDS = listOf(
    "clang++ --version",
    "gcc --version",
    "ls -la",
    "make",
    "nm -D *.so 2>/dev/null || true",
    "termux-battery-status"
)

@Composable
fun TerminalPanel(
    termuxEnv: TermuxEnvironmentState,
    entries: List<TerminalEntry>,
    diagnostics: List<CompilerDiagnostic>,
    history: List<CommandHistoryEntity>,
    jobStatus: JobStatus,
    keepBackgroundAlive: Boolean,
    onToggleBackgroundAlive: (Boolean) -> Unit,
    onRunCommand: (String) -> Unit,
    onClearTerminal: () -> Unit,
    onJumpToDiagnostic: (CompilerDiagnostic) -> Unit,
    onOpenTermuxApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commandInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
    ) {
        // Header & Background Listener Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CarbonSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(
                            when (jobStatus) {
                                JobStatus.RUNNING -> WarmAmber
                                JobStatus.SUCCEEDED -> MintEmerald
                                JobStatus.FAILED -> CoralError
                                JobStatus.IDLE -> ElectricCyan
                            }
                        )
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Termux Terminal",
                        style = MaterialTheme.typography.titleSmall,
                        color = FrostText
                    )
                    Text(
                        text = termuxEnv.resolvedTermuxWorkdir,
                        style = MaterialTheme.typography.labelSmall,
                        color = CoolSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Persist",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoolSlate
                )
                Spacer(Modifier.width(6.dp))
                Switch(
                    checked = keepBackgroundAlive,
                    onCheckedChange = onToggleBackgroundAlive,
                    modifier = Modifier.testTag("terminal_persist_switch")
                )
                IconButton(onClick = onOpenTermuxApp) {
                    Icon(Icons.Default.Terminal, contentDescription = "Launch Termux app", tint = ElectricCyan)
                }
                IconButton(
                    onClick = onClearTerminal,
                    modifier = Modifier.testTag("terminal_clear_btn")
                ) {
                    Icon(Icons.Default.ClearAll, contentDescription = stringResource(R.string.cd_clear_terminal), tint = CoolSlate)
                }
            }
        }

        HorizontalDivider(color = SlateBorder)

        // Clickable Compiler Diagnostics Banner
        if (diagnostics.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GraphiteElevated)
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(diagnostics) { diag ->
                    val isErr = diag.severity.contains("error", ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background((if (isErr) CoralError else WarmAmber).copy(alpha = 0.14f))
                            .border(
                                1.dp,
                                (if (isErr) CoralError else WarmAmber).copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onJumpToDiagnostic(diag) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("diagnostic_item_${diag.line}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isErr) Icons.Default.ErrorOutline else Icons.Default.WarningAmber,
                            contentDescription = diag.severity,
                            tint = if (isErr) CoralError else WarmAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${diag.fileName}:${diag.line}:${diag.column} ${diag.message}",
                            style = MaterialTheme.typography.labelSmall,
                            color = FrostText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            HorizontalDivider(color = SlateBorder)
        }

        // Terminal Output Transcript
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (entries.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CarbonSurface)
                            .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Integrated Termux Execution Console",
                            style = MaterialTheme.typography.titleSmall,
                            color = ElectricCyan
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Tap Run in the top bar to compile with GCC/Clang and stream stdout/stderr here, or enter any Termux shell command below. Background listening keeps running when the app is minimized.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolSlate
                        )
                    }
                }
            }

            items(entries, key = { it.id }) { entry ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarbonSurface)
                        .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "[${entry.timestampLabel}] $ ${entry.command}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        if (entry.isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = WarmAmber
                            )
                        } else if (entry.exitCode != null) {
                            val ok = entry.exitCode == 0 && entry.internalError == null
                            Text(
                                text = "exit ${entry.exitCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ok) MintEmerald else CoralError,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (entry.stdout.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = entry.stdout.trimEnd(),
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FrostText
                        )
                    }

                    if (entry.stderr.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = entry.stderr.trimEnd(),
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoralError
                        )
                    }

                    if (!entry.internalError.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = entry.internalError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmAmber
                        )
                    }
                }
            }
        }

        // Quick commands + history
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(CarbonSurface)
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(QUICK_TERMUX_COMMANDS) { cmd ->
                AssistChip(
                    onClick = { onRunCommand(cmd) },
                    label = {
                        Text(
                            text = cmd.substringBefore(" 2>"),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
            items(history.take(5), key = { it.id }) { past ->
                AssistChip(
                    onClick = { commandInput = past.command },
                    label = {
                        Text(
                            text = past.command,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        // Shell Command Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CarbonSurface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                placeholder = { Text("Run Termux command (e.g. g++ main.cpp -o app && ./app)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (commandInput.isNotBlank()) {
                            onRunCommand(commandInput)
                            commandInput = ""
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_command_input")
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        onRunCommand(commandInput)
                        commandInput = ""
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElectricCyan)
                    .testTag("terminal_send_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.cd_send_command),
                    tint = ObsidianCanvas
                )
            }
        }
    }
}
