package app.visto.ui.reader

import androidx.compose.ui.graphics.Color

/** Reader mode presets for plain-text books. */
enum class ReaderTheme(
    val storageKey: String,
    val backgroundColor: Color,
    val textColor: Color,
    val toolbarColor: Color,
    val isDark: Boolean,
) {
    LIGHT(
        storageKey = "light",
        backgroundColor = Color(0xFFFFFFFF),
        textColor = Color(0xFF1A1A1A),
        toolbarColor = Color(0xF2FFFFFF),
        isDark = false,
    ),
    DARK(
        storageKey = "dark",
        backgroundColor = Color(0xFF1A1A1A),
        textColor = Color(0xFFD0D0D0),
        toolbarColor = Color(0xF2242424),
        isDark = true,
    ),
    CREAM(
        storageKey = "cream",
        backgroundColor = Color(0xFFF5F0E8),
        textColor = Color(0xFF3D3222),
        toolbarColor = Color(0xF2EFE5D6),
        isDark = false,
    );

    companion object {
        val DEFAULT = LIGHT

        fun fromStorage(value: String?): ReaderTheme {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.storageKey == normalized } ?: DEFAULT
        }

        fun sanitizeStorageKey(value: String?): String = fromStorage(value).storageKey
    }
}

/** Readable background presets. DEFAULT keeps using the selected reader mode. */
enum class ReaderBackgroundStyle(
    val storageKey: String,
    val displayLabel: String,
) {
    DEFAULT("default", "默认"),
    WHITE("white", "白纸"),
    PAPER("paper", "纸张"),
    CREAM("cream", "米色"),
    SOFT_GRAY("soft_gray", "柔灰"),
    NIGHT("night", "夜色"),
    BLACK("black", "纯黑");

    companion object {
        val DEFAULT_STYLE = DEFAULT

        fun fromStorage(value: String?): ReaderBackgroundStyle {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.storageKey == normalized } ?: DEFAULT_STYLE
        }

        fun sanitizeStorageKey(value: String?): String = fromStorage(value).storageKey
    }
}

/** Safe reader text color presets. DEFAULT follows the active background. */
enum class ReaderTextColor(
    val storageKey: String,
    val displayLabel: String,
) {
    DEFAULT("default", "默认"),
    INK("ink", "墨色"),
    WARM_BROWN("warm_brown", "暖棕"),
    SOFT_GRAY("soft_gray", "柔灰"),
    PURE("pure", "纯黑/白");

    companion object {
        val DEFAULT_COLOR = DEFAULT

        fun fromStorage(value: String?): ReaderTextColor {
            val normalized = value?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.storageKey == normalized } ?: DEFAULT_COLOR
        }

        fun sanitizeStorageKey(value: String?): String = fromStorage(value).storageKey
    }
}

data class ReaderPalette(
    val backgroundColor: Color,
    val textColor: Color,
    val toolbarColor: Color,
    val isDark: Boolean,
)

fun readerPalette(
    theme: ReaderTheme,
    textColor: ReaderTextColor,
    backgroundStyle: ReaderBackgroundStyle,
): ReaderPalette {
    val background = backgroundStyle.backgroundFor(theme)
    return ReaderPalette(
        backgroundColor = background.backgroundColor,
        textColor = textColor.colorFor(background),
        toolbarColor = background.toolbarColor,
        isDark = background.isDark,
    )
}

fun ReaderSession.readerPalette(): ReaderPalette = readerPalette(
    theme = theme,
    textColor = textColor,
    backgroundStyle = backgroundStyle,
)

private data class ReaderBackgroundPalette(
    val backgroundColor: Color,
    val toolbarColor: Color,
    val defaultTextColor: Color,
    val isDark: Boolean,
)

private fun ReaderBackgroundStyle.backgroundFor(theme: ReaderTheme): ReaderBackgroundPalette = when (this) {
    ReaderBackgroundStyle.DEFAULT -> ReaderBackgroundPalette(
        backgroundColor = theme.backgroundColor,
        toolbarColor = theme.toolbarColor,
        defaultTextColor = theme.textColor,
        isDark = theme.isDark,
    )
    ReaderBackgroundStyle.WHITE -> ReaderBackgroundPalette(
        backgroundColor = Color(0xFFFFFFFF),
        toolbarColor = Color(0xF2FFFFFF),
        defaultTextColor = Color(0xFF1A1A1A),
        isDark = false,
    )
    ReaderBackgroundStyle.PAPER -> ReaderBackgroundPalette(
        backgroundColor = Color(0xFFFBF7EF),
        toolbarColor = Color(0xF2F4ECDE),
        defaultTextColor = Color(0xFF2B251D),
        isDark = false,
    )
    ReaderBackgroundStyle.CREAM -> ReaderBackgroundPalette(
        backgroundColor = Color(0xFFF5F0E8),
        toolbarColor = Color(0xF2EFE5D6),
        defaultTextColor = Color(0xFF3D3222),
        isDark = false,
    )
    ReaderBackgroundStyle.SOFT_GRAY -> ReaderBackgroundPalette(
        backgroundColor = Color(0xFFEFF1EE),
        toolbarColor = Color(0xF2E6E9E5),
        defaultTextColor = Color(0xFF252A27),
        isDark = false,
    )
    ReaderBackgroundStyle.NIGHT -> ReaderBackgroundPalette(
        backgroundColor = Color(0xFF191C1A),
        toolbarColor = Color(0xF2242725),
        defaultTextColor = Color(0xFFD7DCD5),
        isDark = true,
    )
    ReaderBackgroundStyle.BLACK -> ReaderBackgroundPalette(
        backgroundColor = Color(0xFF000000),
        toolbarColor = Color(0xF2101010),
        defaultTextColor = Color(0xFFFFFFFF),
        isDark = true,
    )
}

private fun ReaderTextColor.colorFor(background: ReaderBackgroundPalette): Color = when (this) {
    ReaderTextColor.DEFAULT -> background.defaultTextColor
    ReaderTextColor.INK -> if (background.isDark) Color(0xFFE8E2D8) else Color(0xFF202124)
    ReaderTextColor.WARM_BROWN -> if (background.isDark) Color(0xFFE8D1B3) else Color(0xFF4A3424)
    ReaderTextColor.SOFT_GRAY -> if (background.isDark) Color(0xFFC8CCC7) else Color(0xFF5D625D)
    ReaderTextColor.PURE -> if (background.isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
}
