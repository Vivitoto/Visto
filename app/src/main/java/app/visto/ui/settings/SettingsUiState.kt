package app.visto.ui.settings

import app.visto.ui.theme.ThemeMode

/**
 * Visible state for the settings sheet.
 */
data class SettingsUiState(
    val accountDisplayName: String,
    val accountBaseUrl: String,
    val accountRoot: String,
    val thumbnailCacheBytes: Long,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoLoadOriginalImages: Boolean = false,
    val isClearingCache: Boolean = false,
    val message: String? = null,
)
