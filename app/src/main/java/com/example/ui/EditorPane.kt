package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.editor.CppLanguageEngine
import com.example.data.model.AutocompleteSuggestion
import com.example.data.model.CompilerDiagnostic
import com.example.data.model.JobStatus
import com.example.data.model.OpenEditorTab
import com.example.data.model.SuggestionKind
import com.example.data.model.TerminalEntry
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StudioTheme

private class PolyglotSyntaxVisualTransformation(
    private val fileType: com.example.data.model.CodeFileType
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = CppLanguageEngine.highlightCode(text.text, fileType)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

private val QUICK_SYMBOLS = listOf(
    "#" to "#",
    "<" to "<",
    ">" to ">",
    "{" to "{",
    "}" to "}",
    "(" to "(",
    ")" to ")",
    ";" to ";",
    "*" to "*",
    "&" to "&",
    "->" to "->",
    "::" to "::",
    "\"" to "\""
)

@Composable
fun EditorPane(
    openTabs: List<OpenEditorTab>,
    activeTabIndex: Int,
    suggestions: List<AutocompleteSuggestion>,
    fontSizeSp: Int,
    indentWidth: Int,
    useTabs: Boolean,
    autoIndent: Boolean,
    showLineNumbers: Boolean,
    wordWrap: Boolean,
    compactSuggestions: Boolean,
    showBuildDrawer: Boolean,
    terminalEntries: List<TerminalEntry>,
    diagnostics: List<CompilerDiagnostic>,
    jobStatus: JobStatus,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onTextChanged: (String, Int) -> Unit,
    onApplySuggestion: (AutocompleteSuggestion) -> Pair<String, Int>?,
    onDismissSuggestions: () -> Unit,
    onDismissBuildDrawer: () -> Unit,
    onOpenTerminalTab: () -> Unit,
    onJumpToDiagnostic: (CompilerDiagnostic) -> Unit,
    onUndo: () -> Pair<String, Int>?,
    onSave: () -> Unit,
    onFontSizeDelta: (Int) -> Unit,
    onOpenNewFileDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudioTheme.colors
    val activeTab = openTabs.getOrNull(activeTabIndex)
    val indentUnit = remember(useTabs, indentWidth) {
        if (useTabs) "\t" else " ".repeat(indentWidth.coerceIn(2, 8))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Tab bar + quick editor controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(openTabs, key = { _, tab -> tab.uriString }) { idx, tab ->
                    val selected = idx == activeTabIndex
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) colors.elevated else colors.surface)
                            .border(
                                width = 1.dp,
                                color = if (selected) colors.primaryAccent.copy(alpha = 0.55f) else colors.border.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectTab(idx) }
                            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                            .testTag("editor_tab_${tab.name}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tab.fileType.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primaryAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = tab.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) colors.textPrimary else colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (tab.isDirty) {
                            Spacer(Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(colors.warning)
                            )
                        }
                        IconButton(
                            onClick = { onCloseTab(idx) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_close_tab),
                                tint = colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onOpenNewFileDialog,
                modifier = Modifier.testTag("editor_new_file_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_file),
                    tint = colors.textMuted
                )
            }
            IconButton(
                onClick = { onFontSizeDelta(-1) },
                modifier = Modifier.testTag("editor_font_decrease_btn")
            ) {
                Icon(Icons.Default.TextDecrease, contentDescription = "Decrease font size", tint = colors.textMuted)
            }
            IconButton(
                onClick = { onFontSizeDelta(1) },
                modifier = Modifier.testTag("editor_font_increase_btn")
            ) {
                Icon(Icons.Default.TextIncrease, contentDescription = "Increase font size", tint = colors.textMuted)
            }
        }

        HorizontalDivider(color = colors.border, thickness = 1.dp)

        if (activeTab == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = colors.primaryAccent,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No source file open",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Open a C, C++, Java, Rust, Go, Zig, or Assembly file from the Workspace drawer or tap + to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted
                    )
                }
            }
            return@Column
        }

        var fieldValue by remember(activeTab.uriString) {
            mutableStateOf(
                TextFieldValue(
                    text = activeTab.content,
                    selection = TextRange(activeTab.cursorOffset.coerceIn(0, activeTab.content.length))
                )
            )
        }

        LaunchedEffect(activeTab.content, activeTab.cursorOffset) {
            if (fieldValue.text != activeTab.content || fieldValue.selection.start != activeTab.cursorOffset) {
                fieldValue = TextFieldValue(
                    text = activeTab.content,
                    selection = TextRange(activeTab.cursorOffset.coerceIn(0, activeTab.content.length))
                )
            }
        }

        val visualTransform = remember(activeTab.fileType) { PolyglotSyntaxVisualTransformation(activeTab.fileType) }
        val codeVerticalScroll = rememberScrollState()
        val codeHorizontalScroll = rememberScrollState()
        val lines = remember(fieldValue.text) { fieldValue.text.split('\n') }
        val lineHeightSp = (fontSizeSp * 1.45f).sp
        val (curLine, curCol) = remember(fieldValue.text, fieldValue.selection.start) {
            computeLineCol(fieldValue.text, fieldValue.selection.start)
        }

        // Main Code Area with compact floating suggestion popup overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(codeVerticalScroll)
                    .padding(vertical = 10.dp)
            ) {
                if (showLineNumbers) {
                    Column(
                        modifier = Modifier
                            .background(colors.background)
                            .padding(start = 8.dp, end = 10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        lines.forEachIndexed { idx, _ ->
                            val isCurrentLine = idx + 1 == curLine
                            Text(
                                text = "${idx + 1}",
                                style = TextStyle(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = lineHeightSp,
                                    color = if (isCurrentLine) colors.primaryAccent else colors.gutter
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(colors.border.copy(alpha = 0.5f))
                    )
                }

                val textFieldModifier = if (wordWrap) {
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .testTag("cpp_code_editor_input")
                } else {
                    Modifier
                        .weight(1f)
                        .horizontalScroll(codeHorizontalScroll)
                        .padding(horizontal = 12.dp)
                        .testTag("cpp_code_editor_input")
                }

                BasicTextField(
                    value = fieldValue,
                    onValueChange = { rawNext ->
                        val processed = if (autoIndent) {
                            applyAutoIndentationIfNewline(
                                previous = fieldValue,
                                next = rawNext,
                                indentUnit = indentUnit
                            )
                        } else {
                            rawNext
                        }
                        fieldValue = processed
                        onTextChanged(processed.text, processed.selection.start)
                    },
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = fontSizeSp.sp,
                        lineHeight = lineHeightSp,
                        color = colors.textPrimary
                    ),
                    cursorBrush = SolidColor(colors.primaryAccent),
                    visualTransformation = visualTransform,
                    modifier = textFieldModifier
                )
            }

            // Small Floating Autocomplete Window (~3 rows, does not shrink editor height)
            androidx.compose.animation.AnimatedVisibility(
                visible = suggestions.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 8.dp)
            ) {
                Surface(
                    color = colors.elevated.copy(alpha = 0.96f),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .widthIn(min = 210.dp, max = if (compactSuggestions) 290.dp else 360.dp)
                        .border(1.dp, colors.primaryAccent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .testTag("compact_suggestion_popup")
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surface)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Suggestions (${suggestions.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primaryAccent
                            )
                            IconButton(
                                onClick = onDismissSuggestions,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Hide suggestions",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = colors.border)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = if (compactSuggestions) 118.dp else 184.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(suggestions, key = { "${it.kind}_${it.displayLabel}" }) { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onApplySuggestion(suggestion)?.let { (newText, nextCursor) ->
                                                fieldValue = TextFieldValue(newText, TextRange(nextCursor))
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("autocomplete_item_${suggestion.displayLabel}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val badgeColor = when (suggestion.kind) {
                                        SuggestionKind.STD_HEADER -> colors.primaryAccent
                                        SuggestionKind.LOCAL_HEADER -> colors.secondaryAccent
                                        SuggestionKind.KEYWORD -> colors.tertiaryAccent
                                        SuggestionKind.SNIPPET -> colors.warning
                                        SuggestionKind.SYMBOL, SuggestionKind.LIBRARY_API -> colors.primaryAccent
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeColor.copy(alpha = 0.16f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = suggestion.kind.tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        text = suggestion.displayLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Slide-Up Compact Build Output Drawer (inside Editor Tab, synced with Terminal tab)
        AnimatedVisibility(
            visible = showBuildDrawer,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val latestEntry = terminalEntries.lastOrNull()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 215.dp)
                    .background(colors.elevated)
                    .border(1.dp, colors.primaryAccent.copy(alpha = 0.4f))
                    .testTag("editor_build_drawer")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        if (jobStatus == JobStatus.RUNNING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = colors.warning
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Compiling & running in Termux...",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.warning
                            )
                        } else {
                            val ok = latestEntry?.exitCode == 0 && latestEntry.internalError == null
                            Text(
                                text = if (latestEntry != null) "Build Output (exit ${latestEntry.exitCode ?: "?"})" else "Build Output",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (ok) colors.secondaryAccent else colors.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onOpenTerminalTab,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("build_drawer_open_terminal_btn")
                        ) {
                            Icon(
                                Icons.Default.Terminal,
                                contentDescription = null,
                                tint = colors.primaryAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Terminal", color = colors.primaryAccent, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(
                            onClick = onDismissBuildDrawer,
                            modifier = Modifier.size(28.dp).testTag("build_drawer_close_btn")
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close build drawer",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (diagnostics.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.background.copy(alpha = 0.65f))
                            .padding(vertical = 4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(diagnostics) { diag ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.error.copy(alpha = 0.16f))
                                    .border(1.dp, colors.error.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .clickable { onJumpToDiagnostic(diag) }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = colors.error,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${diag.fileName}:${diag.line} ${diag.message}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                val outScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(outScroll)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    if (latestEntry == null) {
                        Text(
                            text = "Waiting for Termux execution result...",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    } else {
                        Text(
                            text = "$ ${latestEntry.command}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primaryAccent,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (latestEntry.stdout.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = latestEntry.stdout.trimEnd(),
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary
                            )
                        }
                        if (latestEntry.stderr.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = latestEntry.stderr.trimEnd(),
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error
                            )
                        }
                        if (!latestEntry.internalError.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = latestEntry.internalError,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.warning
                            )
                        }
                    }
                }
            }
        }

        // Status + Quick Symbol & Indentation Accessory Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(1.dp, colors.border.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${activeTab.relativePath}  •  Ln $curLine, Col $curCol  •  ${if (useTabs) "Tab" else "${indentWidth}sp"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            onUndo()?.let { (txt, cur) ->
                                fieldValue = TextFieldValue(txt, TextRange(cur))
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("editor_undo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo edit",
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("editor_save_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.cd_save_file),
                            tint = if (activeTab.isDirty) colors.warning else colors.secondaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.high.copy(alpha = 0.5f))
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    QuickKeyPill(
                        label = "Tab",
                        onClick = {
                            val next = insertAtCursor(fieldValue, indentUnit)
                            fieldValue = next
                            onTextChanged(next.text, next.selection.start)
                        }
                    )
                }
                item {
                    QuickKeyPill(
                        label = "Unindent",
                        onClick = {
                            val next = unindentCurrentLine(fieldValue, indentWidth)
                            fieldValue = next
                            onTextChanged(next.text, next.selection.start)
                        }
                    )
                }
                items(QUICK_SYMBOLS) { (label, insert) ->
                    QuickKeyPill(
                        label = label,
                        onClick = {
                            val next = insertAtCursor(fieldValue, insert)
                            fieldValue = next
                            onTextChanged(next.text, next.selection.start)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickKeyPill(
    label: String,
    onClick: () -> Unit
) {
    val colors = StudioTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.elevated)
            .border(1.dp, colors.border, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary
        )
    }
}

private fun insertAtCursor(value: TextFieldValue, insert: String): TextFieldValue {
    val start = value.selection.start.coerceIn(0, value.text.length)
    val end = value.selection.end.coerceIn(start, value.text.length)
    val updated = buildString {
        append(value.text.substring(0, start))
        append(insert)
        append(value.text.substring(end))
    }
    return TextFieldValue(updated, TextRange(start + insert.length))
}

private fun unindentCurrentLine(value: TextFieldValue, indentWidth: Int): TextFieldValue {
    val cursor = value.selection.start.coerceIn(0, value.text.length)
    val lineStart = value.text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
        .let { if (it == -1 || cursor == 0) 0 else it + 1 }
    if (lineStart >= value.text.length) return value

    val removedCount = when {
        value.text[lineStart] == '\t' -> 1
        else -> {
            var spaces = 0
            while (lineStart + spaces < value.text.length &&
                spaces < indentWidth &&
                value.text[lineStart + spaces] == ' '
            ) {
                spaces++
            }
            spaces
        }
    }
    if (removedCount == 0) return value
    val nextText = value.text.removeRange(lineStart, lineStart + removedCount)
    val nextCursor = (cursor - removedCount).coerceAtLeast(lineStart)
    return TextFieldValue(nextText, TextRange(nextCursor))
}

/**
 * Automatically carries leading whitespace onto a new line when Enter (`\n`) is typed,
 * adding an extra [indentUnit] if the previous trimmed line ends with `{` or `:`.
 */
internal fun applyAutoIndentationIfNewline(
    previous: TextFieldValue,
    next: TextFieldValue,
    indentUnit: String
): TextFieldValue {
    if (next.text.length != previous.text.length + 1) return next
    val cursor = next.selection.start
    if (cursor <= 0 || cursor > next.text.length) return next
    if (next.text[cursor - 1] != '\n') return next

    val beforeNewline = next.text.substring(0, cursor - 1)
    val prevLineStart = beforeNewline.lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
    val prevLine = beforeNewline.substring(prevLineStart)
    val baseIndent = prevLine.takeWhile { it == ' ' || it == '\t' }
    val trimmedPrev = prevLine.trimEnd()
    val extraIndent = if (trimmedPrev.endsWith("{") || trimmedPrev.endsWith(":")) indentUnit else ""
    val fullIndent = baseIndent + extraIndent
    if (fullIndent.isEmpty()) return next

    val updatedText = buildString {
        append(next.text.substring(0, cursor))
        append(fullIndent)
        append(next.text.substring(cursor))
    }
    return next.copy(
        text = updatedText,
        selection = TextRange(cursor + fullIndent.length)
    )
}

private fun computeLineCol(text: String, offset: Int): Pair<Int, Int> {
    val safe = offset.coerceIn(0, text.length)
    var line = 1
    var col = 1
    for (i in 0 until safe) {
        if (text[i] == '\n') {
            line++
            col = 1
        } else {
            col++
        }
    }
    return line to col
}
