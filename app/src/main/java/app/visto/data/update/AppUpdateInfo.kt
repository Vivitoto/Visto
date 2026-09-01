package app.visto.data.update

/**
 * Result of a "check latest release" call against GitHub.
 *
 * Mirrors the app update schema used by the downloader so update flows stay
 * consistent. Visto does not consult an R2 mirror; GitHub Release is the
 * single source of truth.
 */
data class AppUpdateInfo(
    val currentVersion: String,
    val currentVersionCode: Int,
    val latestVersion: String,
    val hasUpdate: Boolean,
    val apkName: String,
    val apkUrl: String,
    val releaseUrl: String,
    val releaseNotes: String,
    val apkSize: Long?,
)

data class DownloadedApk(
    val name: String,
    val uri: String,
    val path: String?,
)
