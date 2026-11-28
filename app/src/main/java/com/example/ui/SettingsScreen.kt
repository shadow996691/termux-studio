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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.BuildProfileEntity
import com.example.data.model.AccentPreset
import com.example.data.model.CompilerChoice
import com.example.data.model.StudioThemePreset
import com.example.ui.theme.StudioTheme
import com.example.viewmodel.IdeUiState
import kotlin.math.roundToInt

private val ALL_LANGUAGES = listOf("C", "C++", "Zig", "ASM", "Rust", "Go", "Java", "Kotlin")
private val OPT_LEVELS = listOf("-O0", "-O2", "-O3", "-Os")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: IdeUiState,
    onSelectTheme: (StudioThemePreset) -> Unit,
    onSelectAccent: (AccentPreset) -> Unit,
    onFontSizeChanged: (Int) -> Unit,
    onIndentWidthChanged: (Int) -> Unit,
    onSetUseTabs: (Boolean) -> Unit,
    onSetAutoIndent: (Boolean) -> Unit,
    onSetShowLineNumbers: (Boolean) -> Unit,
    onSetWordWrap: (Boolean) -> Unit,
    onSetCompactSuggestions: (Boolean) -> Unit,
    onUpdateCompilerOptLevel: (String) -> Unit,
    onUpdateBuildProfile: ((BuildProfileEntity) -> BuildProfileEntity) -> Unit,
    onSetKeepBackgroundAlive: (Boolean) -> Unit,
    onSetDefaultLanguage: (String) -> Unit,
    onToggleEnabledLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudioTheme.colors
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Themes", "Compiler", "Editor", "Languages")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Studio Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary
        )
        Text(
            text = "Customize themes, compiler flags, indentation, and active language toolchains.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMuted
        )
        Spacer(Modifier.height(10.dp))

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_category_tabs")
        ) {
            tabs.forEachIndexed { idx, label ->
                SegmentedButton(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.primaryAccent.copy(alpha = 0.2f),
                        activeContentColor = colors.primaryAccent,
                        inactiveContainerColor = colors.surface,
                        inactiveContentColor = colors.textMuted
                    ),
                    modifier = Modifier.testTag("settings_tab_$idx")
                ) {
                    Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        SettingsCard(
                            title = "Dark Theme Palette",
                            icon = Icons.Default.Palette
                        ) {
                            StudioThemePreset.entries.forEach { preset ->
                                val isSelected = uiState.themePreset == preset
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) colors.primaryAccent.copy(alpha = 0.14f) else colors.elevated)
                                        .border(
                                            1.dp,
                                            if (isSelected) colors.primaryAccent else colors.border,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSelectTheme(preset) }
                                        .padding(12.dp)
                                        .testTag("theme_preset_${preset.name}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(preset.backgroundHex))
                                                    .border(1.dp, Color(preset.borderHex), CircleShape)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(preset.surfaceHex))
                                                    .border(1.dp, Color(preset.borderHex), CircleShape)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(preset.textHex))
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isSelected) colors.primaryAccent else colors.textPrimary
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected theme",
                                            tint = colors.primaryAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SettingsCard(
                            title = "Accent Color",
                            icon = Icons.Default.Palette
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AccentPreset.entries.forEach { accent ->
                                    val isSelected = uiState.accentPreset == accent
                                    val swatch = Color(accent.primaryHex)
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) swatch.copy(alpha = 0.2f) else colors.elevated)
                                            .border(
                                                1.5.dp,
                                                if (isSelected) swatch else colors.border,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onSelectAccent(accent) }
                                            .padding(horizontal = 12.dp, vertical = 9.dp)
                                            .testTag("accent_preset_${accent.name}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(swatch)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = accent.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) swatch else colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        SettingsCard(
                            title = "Compiler & Optimization",
                            icon = Icons.Default.SettingsApplications
                        ) {
                            Text("Default Compiler", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CompilerChoice.entries.forEach { choice ->
                                    FilterChip(
                                        selected = uiState.buildProfile.compilerName == choice.name,
                                        onClick = { onUpdateBuildProfile { it.copy(compilerName = choice.name) } },
                                        label = { Text(choice.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.primaryAccent.copy(alpha = 0.2f),
                                            selectedLabelColor = colors.primaryAccent
                                        )
                                    )
                                }
                            }

                            HorizontalDivider(color = colors.border)
                            Text("Optimization Level", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OPT_LEVELS.forEach { opt ->
                                    FilterChip(
                                        selected = uiState.compilerOptLevel == opt,
                                        onClick = { onUpdateCompilerOptLevel(opt) },
                                        label = { Text(opt) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.secondaryAccent.copy(alpha = 0.2f),
                                            selectedLabelColor = colors.secondaryAccent
                                        ),
                                        modifier = Modifier.testTag("opt_level_$opt")
                                    )
                                }
                            }

                            HorizontalDivider(color = colors.border)
                            Text("C / C++ Standards", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("c11", "c17", "c23").forEach { std ->
                                    FilterChip(
                                        selected = uiState.buildProfile.cStandard == std,
                                        onClick = { onUpdateBuildProfile { it.copy(cStandard = std) } },
                                        label = { Text(std) }
                                    )
                                }
                                listOf("c++17", "c++20", "c++23").forEach { std ->
                                    FilterChip(
                                        selected = uiState.buildProfile.cppStandard == std,
                                        onClick = { onUpdateBuildProfile { it.copy(cppStandard = std) } },
                                        label = { Text(std) }
                                    )
                                }
                            }

                            SettingsToggleRow(
                                title = "Keep Termux Build Service Alive",
                                subtitle = "Maintain foreground notification while Termux compiles in background",
                                checked = uiState.keepBackgroundAlive,
                                onCheckedChange = onSetKeepBackgroundAlive
                            )
                        }
                    }
                }

                2 -> {
                    item {
                        SettingsCard(
                            title = "Indentation & Code Editor",
                            icon = Icons.Default.Code
                        ) {
                            Text(
                                text = "Font Size: ${uiState.editorFontSizeSp} sp",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary
                            )
                            Slider(
                                value = uiState.editorFontSizeSp.toFloat(),
                                onValueChange = { onFontSizeChanged(it.roundToInt()) },
                                valueRange = 10f..22f,
                                steps = 11
                            )

                            SettingsToggleRow(
                                title = "Auto-Indentation",
                                subtitle = "Preserve line indent on Enter and increase after '{' or ':'",
                                checked = uiState.autoIndent,
                                onCheckedChange = onSetAutoIndent,
                                testTag = "setting_auto_indent_switch"
                            )

                            SettingsToggleRow(
                                title = "Indent Using Hard Tabs (\\t)",
                                subtitle = "Disable to insert spaces instead of tab characters",
                                checked = uiState.useTabs,
                                onCheckedChange = onSetUseTabs,
                                testTag = "setting_use_tabs_switch"
                            )

                            Text("Indent Width (Spaces)", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(2, 4, 8).forEach { w ->
                                    FilterChip(
                                        selected = uiState.indentWidth == w,
                                        onClick = { onIndentWidthChanged(w) },
                                        label = { Text("$w spaces") },
                                        modifier = Modifier.testTag("indent_width_$w")
                                    )
                                }
                            }

                            HorizontalDivider(color = colors.border)

                            SettingsToggleRow(
                                title = "Show Line Numbers",
                                subtitle = "Display left gutter line numbers",
                                checked = uiState.showLineNumbers,
                                onCheckedChange = onSetShowLineNumbers
                            )

                            SettingsToggleRow(
                                title = "Word Wrap",
                                subtitle = "Wrap long source lines inside viewport instead of horizontal scrolling",
                                checked = uiState.wordWrap,
                                onCheckedChange = onSetWordWrap
                            )

                            SettingsToggleRow(
                                title = "Compact Suggestion Popup",
                                subtitle = "Keep autocomplete in a small floating corner window (~3 rows)",
                                checked = uiState.compactSuggestions,
                                onCheckedChange = onSetCompactSuggestions
                            )
                        }
                    }
                }

                else -> {
                    item {
                        SettingsCard(
                            title = "Languages & Active Toolchains",
                            icon = Icons.Default.Translate
                        ) {
                            Text("Default Project Language", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ALL_LANGUAGES.forEach { lang ->
                                    FilterChip(
                                        selected = uiState.defaultLanguage == lang,
                                        onClick = { onSetDefaultLanguage(lang) },
                                        label = { Text(lang) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.primaryAccent.copy(alpha = 0.2f),
                                            selectedLabelColor = colors.primaryAccent
                                        ),
                                        modifier = Modifier.testTag("default_lang_$lang")
                                    )
                                }
                            }

                            HorizontalDivider(color = colors.border)
                            Text("Enabled Language Toolchains", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                            val enabledSet = uiState.enabledLanguagesCsv
                                .split(',')
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .toSet()

                            ALL_LANGUAGES.forEach { lang ->
                                val isEnabled = lang in enabledSet
                                SettingsToggleRow(
                                    title = lang,
                                    subtitle = if (isEnabled) "Enabled in Editor, Compiler & App packager" else "Hidden from quick toolchain filters",
                                    checked = isEnabled,
                                    onCheckedChange = { onToggleEnabledLanguage(lang) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    val colors = StudioTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = colors.primaryAccent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.primaryAccent,
                fontWeight = FontWeight.SemiBold
            )
        }
        content()
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    val colors = StudioTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}
