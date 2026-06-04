package app.visto.ui.settings

import app.visto.data.account.AccountSummary
import app.visto.ui.theme.ThemeMode

/**
 * Visible state for the settings sheet.
 */
data class SettingsUiState(
    val activeAccountId: Long? = null,
    val accounts: List<AccountSummary> = emptyList(),
    val accountDisplayName: String = "",
    val accountBaseUrl: String = "",
    val accountRoot: String = "",
    val thumbnailCacheBytes: Long,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoLoadOriginalImages: Boolean = false,
    val isClearingCache: Boolean = false,
    val message: String? = null,
)
