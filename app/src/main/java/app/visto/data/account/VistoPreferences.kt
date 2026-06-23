package app.visto.data.account

import android.content.Context
import android.content.SharedPreferences
import app.visto.core.sort.SortMode
import app.visto.ui.reader.ReaderBackgroundStyle
import app.visto.ui.reader.ReaderFontChoice
import app.visto.ui.reader.ReaderTextColor
import app.visto.ui.reader.ReaderTheme
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

    /** Blur thumbnails in lists/grids for shoulder-surfing privacy. */
    var blurThumbnails: Boolean
        get() = prefs.getBoolean(KEY_BLUR_THUMBNAILS, false)
        set(enabled) = prefs.edit { putBoolean(KEY_BLUR_THUMBNAILS, enabled) }

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

    /** Last user-selected sort mode for album folders/grids. */
    var albumSortMode: SortMode
        get() = SortMode.fromStorage(prefs.getString(KEY_ALBUM_SORT_MODE, SortMode.DEFAULT.storageKey))
        set(mode) = prefs.edit { putString(KEY_ALBUM_SORT_MODE, mode.storageKey) }

    /** Default grid density for album detail and WebDAV browser thumbnails. */
    var gridDensity: GridDensity
        get() = GridDensity.fromStorage(prefs.getString(KEY_GRID_DENSITY, GridDensity.STANDARD.storageKey))
        set(value) = prefs.edit { putString(KEY_GRID_DENSITY, value.storageKey) }

    /** Reader settings used for books that do not have saved per-book progress yet. */
    var defaultReaderSettings: ReaderDefaultSettings
        get() = ReaderDefaultSettings(
            fontSizeSp = prefs
                .getInt(KEY_READER_DEFAULT_FONT_SIZE_SP, DEFAULT_READER_FONT_SIZE_SP)
                .coerceIn(MIN_READER_FONT_SIZE_SP, MAX_READER_FONT_SIZE_SP),
            lineSpacing = prefs
                .getFloat(KEY_READER_DEFAULT_LINE_SPACING, DEFAULT_READER_LINE_SPACING)
                .coerceIn(MIN_READER_LINE_SPACING, MAX_READER_LINE_SPACING),
            theme = prefs
                .getString(KEY_READER_DEFAULT_THEME, DEFAULT_READER_THEME)
                .let(ReaderTheme::sanitizeStorageKey),
            fontChoice = ReaderFontChoice.sanitizeStorageKey(
                prefs.getString(KEY_READER_DEFAULT_FONT_CHOICE, ReaderFontChoice.DEFAULT.storageKey),
            ),
            textColor = ReaderTextColor.sanitizeStorageKey(
                prefs.getString(KEY_READER_DEFAULT_TEXT_COLOR, ReaderTextColor.DEFAULT_COLOR.storageKey),
            ),
            backgroundStyle = ReaderBackgroundStyle.sanitizeStorageKey(
                prefs.getString(KEY_READER_DEFAULT_BACKGROUND_STYLE, ReaderBackgroundStyle.DEFAULT_STYLE.storageKey),
            ),
        )
        set(value) = prefs.edit {
            putInt(
                KEY_READER_DEFAULT_FONT_SIZE_SP,
                value.fontSizeSp.coerceIn(MIN_READER_FONT_SIZE_SP, MAX_READER_FONT_SIZE_SP),
            )
            putFloat(
                KEY_READER_DEFAULT_LINE_SPACING,
                value.lineSpacing.coerceIn(MIN_READER_LINE_SPACING, MAX_READER_LINE_SPACING),
            )
            putString(KEY_READER_DEFAULT_THEME, ReaderTheme.sanitizeStorageKey(value.theme))
            putString(KEY_READER_DEFAULT_FONT_CHOICE, ReaderFontChoice.sanitizeStorageKey(value.fontChoice))
            putString(KEY_READER_DEFAULT_TEXT_COLOR, ReaderTextColor.sanitizeStorageKey(value.textColor))
            putString(KEY_READER_DEFAULT_BACKGROUND_STYLE, ReaderBackgroundStyle.sanitizeStorageKey(value.backgroundStyle))
        }

    /**
     * Maximum disk space Visto's thumbnail cache may occupy. The cache is
     * still a Coil [coil.disk.DiskCache] LRU under the hood; this bound
     * caps how much it grows before old entries are evicted.
     */
    var thumbnailCacheLimit: ThumbnailCacheLimit
        get() = ThumbnailCacheLimit.fromStorage(prefs.getString(KEY_THUMBNAIL_CACHE_LIMIT, ThumbnailCacheLimit.DEFAULT.storageKey))
        set(value) = prefs.edit { putString(KEY_THUMBNAIL_CACHE_LIMIT, value.storageKey) }

    companion object {
        private const val PREF_NAME = "visto_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_AUTO_LOAD_ORIGINAL_IMAGES = "auto_load_original_images"
        private const val KEY_BLUR_THUMBNAILS = "blur_thumbnails"
        private const val KEY_MAX_GRID_THUMBNAIL_BYTES = "max_grid_thumbnail_bytes"
        private const val KEY_ALBUM_VIEW_MODE = "album_view_mode"
        private const val KEY_ALBUM_SORT_MODE = "album_sort_mode"
        private const val KEY_GRID_DENSITY = "grid_density"
        private const val KEY_READER_DEFAULT_FONT_SIZE_SP = "reader_default_font_size_sp"
        private const val KEY_READER_DEFAULT_LINE_SPACING = "reader_default_line_spacing"
        private const val KEY_READER_DEFAULT_THEME = "reader_default_theme"
        private const val KEY_READER_DEFAULT_FONT_CHOICE = "reader_default_font_choice"
        private const val KEY_READER_DEFAULT_TEXT_COLOR = "reader_default_text_color"
        private const val KEY_READER_DEFAULT_BACKGROUND_STYLE = "reader_default_background_style"
        private const val KEY_THUMBNAIL_CACHE_LIMIT = "thumbnail_cache_limit"
        const val DEFAULT_MAX_GRID_THUMBNAIL_BYTES: Long = 8L * 1024 * 1024
        const val DEFAULT_READER_FONT_SIZE_SP = 18
        const val DEFAULT_READER_LINE_SPACING = 1.5f
        const val DEFAULT_READER_THEME = "light"
        private const val MIN_READER_FONT_SIZE_SP = 14
        private const val MAX_READER_FONT_SIZE_SP = 28
        private const val MIN_READER_LINE_SPACING = 1.0f
        private const val MAX_READER_LINE_SPACING = 2.4f
    }
}

data class ReaderDefaultSettings(
    val fontSizeSp: Int = VistoPreferences.DEFAULT_READER_FONT_SIZE_SP,
    val lineSpacing: Float = VistoPreferences.DEFAULT_READER_LINE_SPACING,
    val theme: String = VistoPreferences.DEFAULT_READER_THEME,
    val fontChoice: String = ReaderFontChoice.DEFAULT.storageKey,
    val textColor: String = ReaderTextColor.DEFAULT_COLOR.storageKey,
    val backgroundStyle: String = ReaderBackgroundStyle.DEFAULT_STYLE.storageKey,
)

/**
 * User-selectable cap for the on-disk thumbnail cache.
 *
 * UNLIMITED is implemented as a very large ceiling (16 GB) so Coil's own
 * DiskCache, which requires a positive byte bound, still has a backstop.
 */
enum class ThumbnailCacheLimit(
    val storageKey: String,
    val bytes: Long,
    val displayLabel: String,
) {
    MB_100("100mb", 100L * 1024 * 1024, "100 MB"),
    MB_200("200mb", 200L * 1024 * 1024, "200 MB"),
    MB_500("500mb", 500L * 1024 * 1024, "500 MB"),
    MB_1000("1000mb", 1000L * 1024 * 1024, "1 GB"),
    UNLIMITED("unlimited", 16L * 1024 * 1024 * 1024, "不限制");

    companion object {
        val DEFAULT = MB_500

        fun fromStorage(value: String?): ThumbnailCacheLimit {
            if (value == null) return DEFAULT
            return entries.firstOrNull { it.storageKey == value } ?: DEFAULT
        }
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

enum class GridDensity(
    val storageKey: String,
    val displayLabel: String,
) {
    COMFORTABLE("comfortable", "舒适"),
    STANDARD("standard", "标准"),
    COMPACT("compact", "紧凑");

    fun next(): GridDensity = when (this) {
        COMFORTABLE -> STANDARD
        STANDARD -> COMPACT
        COMPACT -> COMFORTABLE
    }

    companion object {
        fun fromStorage(value: String?): GridDensity {
            if (value == null) return STANDARD
            return entries.firstOrNull { it.storageKey == value } ?: STANDARD
        }
    }
}

/**
 * Album icon-grid cycles through density modes, then returns to the list view.
 * Returning null means the next visible state is the list view.
 */
fun GridDensity.nextAlbumFolderGridDensityOrNull(): GridDensity? = when (this) {
    GridDensity.COMFORTABLE -> GridDensity.STANDARD
    GridDensity.STANDARD -> GridDensity.COMPACT
    GridDensity.COMPACT -> null
}
