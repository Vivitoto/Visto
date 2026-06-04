package app.visto.data.account

import android.content.Context
import android.content.SharedPreferences
import app.visto.ui.theme.ThemeMode
import androidx.core.content.edit

/**
 * Thin preference wrapper for user-facing settings that survive restarts.
 *
 * Co-located with AccountService so preferences are scoped to the app's
 * private space, not leaked to any other process.
 */
class VistoPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.fromStorage(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.storageKey))
        set(mode) = prefs.edit { putString(KEY_THEME, mode.storageKey) }

    var autoLoadOriginalImages: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOAD_ORIGINAL_IMAGES, false)
        set(enabled) = prefs.edit { putBoolean(KEY_AUTO_LOAD_ORIGINAL_IMAGES, enabled) }

    /**
     * Maximum byte size for which Visto will fetch an image for grid
     * thumbnails. Files larger than this show a metadata placeholder so the
     * browser does not silently download dozens of MB-sized originals while
     * the user is just scanning a folder.
     */
    var maxGridThumbnailBytes: Long
        get() = prefs.getLong(KEY_MAX_GRID_THUMBNAIL_BYTES, DEFAULT_MAX_GRID_THUMBNAIL_BYTES)
        set(value) = prefs.edit { putLong(KEY_MAX_GRID_THUMBNAIL_BYTES, value) }

    /**
     * How an album is browsed when opened from the home screen.
     *
     * - FOLDERS (default): show the album's subdirectories and direct media
     *   files at the current level, like a file browser rooted at the
     *   album path. Lets users keep the structure they organized on disk.
     * - FLAT: recursively walk the album and show every image grouped by
     *   subfolder. Useful when the album is one logical collection.
     */
    var albumViewMode: AlbumViewMode
        get() = AlbumViewMode.fromStorage(prefs.getString(KEY_ALBUM_VIEW_MODE, AlbumViewMode.FOLDERS.storageKey))
        set(mode) = prefs.edit { putString(KEY_ALBUM_VIEW_MODE, mode.storageKey) }

    companion object {
        private const val PREF_NAME = "visto_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_AUTO_LOAD_ORIGINAL_IMAGES = "auto_load_original_images"
        private const val KEY_MAX_GRID_THUMBNAIL_BYTES = "max_grid_thumbnail_bytes"
        private const val KEY_ALBUM_VIEW_MODE = "album_view_mode"
        const val DEFAULT_MAX_GRID_THUMBNAIL_BYTES: Long = 8L * 1024 * 1024
    }
}

enum class AlbumViewMode(val storageKey: String) {
    FOLDERS("folders"),
    FLAT("flat");

    companion object {
        fun fromStorage(value: String?): AlbumViewMode {
            if (value == null) return FOLDERS
            return entries.firstOrNull { it.storageKey == value } ?: FOLDERS
        }
    }
}