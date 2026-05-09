package com.znam.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Light Colors ────────────────────────────────────────────────────────
// Matches colors.xml md_theme_light_* values

private val LightPrimary = Color(0xFF006874)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFF97F0FF)
private val LightOnPrimaryContainer = Color(0xFF001F24)
private val LightSecondary = Color(0xFF4A6267)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFCDE7EC)
private val LightOnSecondaryContainer = Color(0xFF051F23)
private val LightTertiary = Color(0xFF525E7D)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFDAE2FF)
private val LightOnTertiaryContainer = Color(0xFF0E1B37)
private val LightError = Color(0xFFBA1A1A)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFFFDAD6)
private val LightOnErrorContainer = Color(0xFF410002)
private val LightBackground = Color(0xFFFAFDFD)
private val LightOnBackground = Color(0xFF191C1D)
private val LightSurface = Color(0xFFFAFDFD)
private val LightOnSurface = Color(0xFF191C1D)
private val LightSurfaceVariant = Color(0xFFDBE4E6)
private val LightOnSurfaceVariant = Color(0xFF3F484A)
private val LightOutline = Color(0xFF6F797A)
private val LightOutlineVariant = Color(0xFFBFC8CA)
private val LightInverseSurface = Color(0xFF2E3132)
private val LightInverseOnSurface = Color(0xFFEFF1F1)
private val LightInversePrimary = Color(0xFF4FD8EB)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary,
)

// ── Dark Colors ─────────────────────────────────────────────────────────
// Matches colors.xml md_theme_dark_* values

private val DarkPrimary = Color(0xFF4FD8EB)
private val DarkOnPrimary = Color(0xFF00363D)
private val DarkPrimaryContainer = Color(0xFF004F58)
private val DarkOnPrimaryContainer = Color(0xFF97F0FF)
private val DarkSecondary = Color(0xFFB1CBD0)
private val DarkOnSecondary = Color(0xFF1C3438)
private val DarkSecondaryContainer = Color(0xFF334B4F)
private val DarkOnSecondaryContainer = Color(0xFFCDE7EC)
private val DarkTertiary = Color(0xFFBAC6EA)
private val DarkOnTertiary = Color(0xFF24304D)
private val DarkTertiaryContainer = Color(0xFF3B4664)
private val DarkOnTertiaryContainer = Color(0xFFDAE2FF)
private val DarkError = Color(0xFFFFB4AB)
private val DarkOnError = Color(0xFF690005)
private val DarkErrorContainer = Color(0xFF93000A)
private val DarkOnErrorContainer = Color(0xFFFFDAD6)
private val DarkBackground = Color(0xFF191C1D)
private val DarkOnBackground = Color(0xFFE1E3E3)
private val DarkSurface = Color(0xFF191C1D)
private val DarkOnSurface = Color(0xFFE1E3E3)
private val DarkSurfaceVariant = Color(0xFF3F484A)
private val DarkOnSurfaceVariant = Color(0xFFBFC8CA)
private val DarkOutline = Color(0xFF899294)
private val DarkOutlineVariant = Color(0xFF3F484A)
private val DarkInverseSurface = Color(0xFFE1E3E3)
private val DarkInverseOnSurface = Color(0xFF191C1D)
private val DarkInversePrimary = Color(0xFF006874)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
)

// ── Theme Composable ────────────────────────────────────────────────────

@Composable
fun ZnamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Update status bar color to match the theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
