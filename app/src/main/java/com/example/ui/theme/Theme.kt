package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AccentPreset
import com.example.data.model.StudioThemePreset

@Immutable
data class StudioThemeColors(
    val background: Color = ObsidianCanvas,
    val surface: Color = CarbonSurface,
    val elevated: Color = GraphiteElevated,
    val high: Color = GraphiteHigh,
    val border: Color = SlateBorder,
    val primaryAccent: Color = ElectricCyan,
    val secondaryAccent: Color = MintEmerald,
    val tertiaryAccent: Color = SoftViolet,
    val warning: Color = WarmAmber,
    val error: Color = CoralError,
    val textPrimary: Color = FrostText,
    val textMuted: Color = CoolSlate,
    val gutter: Color = GutterSlate
)

val LocalStudioColors = staticCompositionLocalOf { StudioThemeColors() }

object StudioTheme {
    val colors: StudioThemeColors
        @Composable get() = LocalStudioColors.current
}

private val StudioShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp)
)

@Composable
fun TermuxCStudioTheme(
    themePreset: StudioThemePreset = StudioThemePreset.DEEP_OBSIDIAN,
    accentPreset: AccentPreset = AccentPreset.ELECTRIC_CYAN,
    content: @Composable () -> Unit
) {
    val bg = Color(themePreset.backgroundHex)
    val surface = Color(themePreset.surfaceHex)
    val elevated = Color(themePreset.elevatedHex)
    val high = Color(themePreset.highHex)
    val border = Color(themePreset.borderHex)
    val text = Color(themePreset.textHex)
    val muted = Color(themePreset.mutedTextHex)
    val gutter = Color(themePreset.gutterHex)

    val primary = Color(accentPreset.primaryHex)
    val secondary = Color(accentPreset.secondaryHex)
    val tertiary = Color(accentPreset.tertiaryHex)
    val primaryContainer = Color(accentPreset.containerHex)

    val dynamicScheme = darkColorScheme(
        primary = primary,
        onPrimary = bg,
        primaryContainer = primaryContainer,
        onPrimaryContainer = text,
        secondary = secondary,
        onSecondary = bg,
        secondaryContainer = elevated,
        onSecondaryContainer = text,
        tertiary = tertiary,
        onTertiary = bg,
        background = bg,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = elevated,
        onSurfaceVariant = muted,
        surfaceContainerLow = surface,
        surfaceContainer = elevated,
        surfaceContainerHigh = high,
        outline = border,
        outlineVariant = gutter,
        error = CoralError,
        onError = bg
    )

    val studioColors = StudioThemeColors(
        background = bg,
        surface = surface,
        elevated = elevated,
        high = high,
        border = border,
        primaryAccent = primary,
        secondaryAccent = secondary,
        tertiaryAccent = tertiary,
        warning = WarmAmber,
        error = CoralError,
        textPrimary = text,
        textMuted = muted,
        gutter = gutter
    )

    CompositionLocalProvider(LocalStudioColors provides studioColors) {
        MaterialTheme(
            colorScheme = dynamicScheme,
            typography = Typography,
            shapes = StudioShapes,
            content = content
        )
    }
}
