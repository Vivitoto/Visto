package app.visto.ui.settings

import app.visto.data.account.AccountSummary
import app.visto.data.account.ThumbnailCacheLimit
import app.visto.data.update.AppUpdateInfo
import app.visto.data.update.DownloadedApk
import app.visto.ui.theme.ThemeMode

/** Update-section state. */
data class UpdateUiState(
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadedBytes: Long = 0L,
    val downloadTotalBytes: Long? = null,
    val info: AppUpdateInfo? = null,
    val downloaded: DownloadedApk? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

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
    val blurThumbnails: Boolean = false,
    val thumbnailCacheLimit: ThumbnailCacheLimit = ThumbnailCacheLimit.DEFAULT,
    val isClearingCache: Boolean = false,
    val message: String? = null,
    val update: UpdateUiState = UpdateUiState(),
)
