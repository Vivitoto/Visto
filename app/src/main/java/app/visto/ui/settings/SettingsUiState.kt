package app.visto.ui.settings

/**
 * Visible state for the settings sheet.
 */
data class SettingsUiState(
    val accountDisplayName: String,
    val accountBaseUrl: String,
    val accountRoot: String,
    val thumbnailCacheBytes: Long,
    val isClearingCache: Boolean = false,
    val message: String? = null,
)
