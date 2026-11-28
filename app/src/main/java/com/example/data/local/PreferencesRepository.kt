package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AccentPreset
import com.example.data.model.StudioThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ideDataStore: DataStore<Preferences> by preferencesDataStore(name = "termuxc_prefs")

data class EditorPreferences(
    val termuxTreeUri: String? = null,
    val termuxWorkdirPath: String = "/storage/emulated/0/TermuxStudio",
    val editorFontSizeSp: Int = 13,
    val autoSaveBeforeBuild: Boolean = true,
    val keepBackgroundServiceAlive: Boolean = true,
    val lastOpenedFileUri: String? = null,
    val githubAptRepoUrl: String = "https://YOUR_GITHUB_USER.github.io/termux-studio-repo",
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
    val enabledLanguagesCsv: String = "C,C++,Zig,ASM,Rust,Go,Java,Kotlin"
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val TERMUX_TREE_URI = stringPreferencesKey("termux_tree_uri")
        val TERMUX_WORKDIR = stringPreferencesKey("termux_workdir")
        val EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val AUTO_SAVE = booleanPreferencesKey("auto_save_before_build")
        val KEEP_BACKGROUND_ALIVE = booleanPreferencesKey("keep_background_alive")
        val LAST_FILE_URI = stringPreferencesKey("last_opened_file_uri")
        val GITHUB_APT_REPO_URL = stringPreferencesKey("github_apt_repo_url")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val INDENT_WIDTH = intPreferencesKey("indent_width")
        val USE_TABS = booleanPreferencesKey("use_tabs")
        val AUTO_INDENT = booleanPreferencesKey("auto_indent")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val COMPACT_SUGGESTIONS = booleanPreferencesKey("compact_suggestions")
        val COMPILER_OPT_LEVEL = stringPreferencesKey("compiler_opt_level")
        val DEFAULT_LANGUAGE = stringPreferencesKey("default_language")
        val ENABLED_LANGUAGES_CSV = stringPreferencesKey("enabled_languages_csv")
    }

    val preferences: Flow<EditorPreferences> = context.ideDataStore.data.map { prefs ->
        val theme = runCatching {
            StudioThemePreset.valueOf(prefs[Keys.THEME_PRESET] ?: StudioThemePreset.DEEP_OBSIDIAN.name)
        }.getOrDefault(StudioThemePreset.DEEP_OBSIDIAN)
        val accent = runCatching {
            AccentPreset.valueOf(prefs[Keys.ACCENT_PRESET] ?: AccentPreset.ELECTRIC_CYAN.name)
        }.getOrDefault(AccentPreset.ELECTRIC_CYAN)

        EditorPreferences(
            termuxTreeUri = prefs[Keys.TERMUX_TREE_URI],
            termuxWorkdirPath = prefs[Keys.TERMUX_WORKDIR] ?: "/storage/emulated/0/TermuxStudio",
            editorFontSizeSp = prefs[Keys.EDITOR_FONT_SIZE] ?: 13,
            autoSaveBeforeBuild = prefs[Keys.AUTO_SAVE] ?: true,
            keepBackgroundServiceAlive = prefs[Keys.KEEP_BACKGROUND_ALIVE] ?: true,
            lastOpenedFileUri = prefs[Keys.LAST_FILE_URI],
            githubAptRepoUrl = prefs[Keys.GITHUB_APT_REPO_URL]
                ?: "https://YOUR_GITHUB_USER.github.io/termux-studio-repo",
            themePreset = theme,
            accentPreset = accent,
            indentWidth = prefs[Keys.INDENT_WIDTH] ?: 4,
            useTabs = prefs[Keys.USE_TABS] ?: false,
            autoIndent = prefs[Keys.AUTO_INDENT] ?: true,
            showLineNumbers = prefs[Keys.SHOW_LINE_NUMBERS] ?: true,
            wordWrap = prefs[Keys.WORD_WRAP] ?: false,
            compactSuggestions = prefs[Keys.COMPACT_SUGGESTIONS] ?: true,
            compilerOptLevel = prefs[Keys.COMPILER_OPT_LEVEL] ?: "-O2",
            defaultLanguage = prefs[Keys.DEFAULT_LANGUAGE] ?: "C++",
            enabledLanguagesCsv = prefs[Keys.ENABLED_LANGUAGES_CSV] ?: "C,C++,Zig,ASM,Rust,Go,Java,Kotlin"
        )
    }

    suspend fun setTermuxWorkspace(treeUri: String?, workdirPath: String) {
        context.ideDataStore.edit { prefs ->
            if (treeUri != null) {
                prefs[Keys.TERMUX_TREE_URI] = treeUri
            } else {
                prefs.remove(Keys.TERMUX_TREE_URI)
            }
            prefs[Keys.TERMUX_WORKDIR] = workdirPath
        }
    }

    suspend fun setEditorFontSize(sizeSp: Int) {
        context.ideDataStore.edit { prefs ->
            prefs[Keys.EDITOR_FONT_SIZE] = sizeSp.coerceIn(10, 22)
        }
    }

    suspend fun setKeepBackgroundServiceAlive(enabled: Boolean) {
        context.ideDataStore.edit { prefs ->
            prefs[Keys.KEEP_BACKGROUND_ALIVE] = enabled
        }
    }

    suspend fun setAutoSaveBeforeBuild(enabled: Boolean) {
        context.ideDataStore.edit { prefs ->
            prefs[Keys.AUTO_SAVE] = enabled
        }
    }

    suspend fun setLastOpenedFileUri(uriString: String?) {
        context.ideDataStore.edit { prefs ->
            if (uriString != null) {
                prefs[Keys.LAST_FILE_URI] = uriString
            } else {
                prefs.remove(Keys.LAST_FILE_URI)
            }
        }
    }

    suspend fun setGithubAptRepoUrl(url: String) {
        context.ideDataStore.edit { prefs ->
            prefs[Keys.GITHUB_APT_REPO_URL] = url.trim().removeSuffix("/")
        }
    }

    suspend fun setThemePreset(preset: StudioThemePreset) {
        context.ideDataStore.edit { prefs -> prefs[Keys.THEME_PRESET] = preset.name }
    }

    suspend fun setAccentPreset(preset: AccentPreset) {
        context.ideDataStore.edit { prefs -> prefs[Keys.ACCENT_PRESET] = preset.name }
    }

    suspend fun setIndentWidth(width: Int) {
        context.ideDataStore.edit { prefs -> prefs[Keys.INDENT_WIDTH] = width.coerceIn(2, 8) }
    }

    suspend fun setUseTabs(useTabs: Boolean) {
        context.ideDataStore.edit { prefs -> prefs[Keys.USE_TABS] = useTabs }
    }

    suspend fun setAutoIndent(enabled: Boolean) {
        context.ideDataStore.edit { prefs -> prefs[Keys.AUTO_INDENT] = enabled }
    }

    suspend fun setShowLineNumbers(show: Boolean) {
        context.ideDataStore.edit { prefs -> prefs[Keys.SHOW_LINE_NUMBERS] = show }
    }

    suspend fun setWordWrap(wrap: Boolean) {
        context.ideDataStore.edit { prefs -> prefs[Keys.WORD_WRAP] = wrap }
    }

    suspend fun setCompactSuggestions(compact: Boolean) {
        context.ideDataStore.edit { prefs -> prefs[Keys.COMPACT_SUGGESTIONS] = compact }
    }

    suspend fun setCompilerOptLevel(optLevel: String) {
        context.ideDataStore.edit { prefs -> prefs[Keys.COMPILER_OPT_LEVEL] = optLevel }
    }

    suspend fun setDefaultLanguage(language: String) {
        context.ideDataStore.edit { prefs -> prefs[Keys.DEFAULT_LANGUAGE] = language }
    }

    suspend fun setEnabledLanguagesCsv(csv: String) {
        context.ideDataStore.edit { prefs -> prefs[Keys.ENABLED_LANGUAGES_CSV] = csv }
    }
}
