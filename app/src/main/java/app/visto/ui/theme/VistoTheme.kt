package app.visto.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Visto theme selector.
 *
 * Persisted as the string value (see [ThemeMode.storageKey]) so storage
 * format is stable across builds even if the enum order changes.
 */
enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): ThemeMode = values()
            .firstOrNull { it.storageKey == value } ?: SYSTEM
    }
}

/**
 * Brand palette. Champagne is the throughline that ties light and dark
 * variants together — everything else flows from the active color scheme.
 */
object VistoBrand {
    val ChampagneDark = Color(0xFFD6B981)
    val ChampagneLight = Color(0xFFB88945)
}

private val DarkScheme = darkColorScheme(
    primary = VistoBrand.ChampagneDark,
    onPrimary = Color(0xFF0E0F12),
    primaryContainer = Color(0xFF2A2316),
    onPrimaryContainer = Color(0xFFF1E2C2),
    secondary = Color(0xFFD0CAB7),
    onSecondary = Color(0xFF1B1D22),
    background = Color(0xFF0E0F12),
    onBackground = Color(0xFFF5F1E8),
    surface = Color(0xFF0E0F12),
    onSurface = Color(0xFFF5F1E8),
    surfaceVariant = Color(0xFF1B1D22),
    onSurfaceVariant = Color(0xFFB4AC9A),
    outline = Color(0xFF2B2E36),
    outlineVariant = Color(0xFF22252B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF0E0F12),
)

private val LightScheme = lightColorScheme(
    primary = VistoBrand.ChampagneLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF4E6CB),
    onPrimaryContainer = Color(0xFF3B2A10),
    secondary = Color(0xFF6F685C),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F3EA),
    onBackground = Color(0xFF1C1A16),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1A16),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF6F685C),
    outline = Color(0xFFE3D9C8),
    outlineVariant = Color(0xFFF0E8D7),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

/**
 * Apply Visto's premium dark or warm light scheme.
 *
 * Dynamic color is intentionally **not** used — a photo viewer needs a
 * neutral, predictable canvas. The user can still pin light/dark/system in
 * Settings.
 */
@Composable
fun VistoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}
