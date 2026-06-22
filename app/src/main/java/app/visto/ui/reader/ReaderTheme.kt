package app.visto.ui.reader

import androidx.compose.ui.graphics.Color

enum class ReaderTheme {
    LIGHT,
    DARK,
    CREAM,
}

data class ReaderColors(
    val background: Color,
    val text: Color,
    val toolbarBackground: Color,
    val toolbarText: Color,
    val divider: Color,
)

fun ReaderTheme.colors(): ReaderColors = when (this) {
    ReaderTheme.LIGHT -> ReaderColors(
        background = Color(0xFFFFFFFF),
        text = Color(0xFF1A1A1A),
        toolbarBackground = Color(0xEEFFFFFF),
        toolbarText = Color(0xFF1A1A1A),
        divider = Color(0xFFE0E0E0),
    )
    ReaderTheme.DARK -> ReaderColors(
        background = Color(0xFF1A1A1A),
        text = Color(0xFFE0E0E0),
        toolbarBackground = Color(0xEE202020),
        toolbarText = Color(0xFFE0E0E0),
        divider = Color(0xFF424242),
    )
    ReaderTheme.CREAM -> ReaderColors(
        background = Color(0xFFF5E6D3),
        text = Color(0xFF3E2723),
        toolbarBackground = Color(0xEEF5E6D3),
        toolbarText = Color(0xFF3E2723),
        divider = Color(0xFFD7C3A7),
    )
}
