package app.visto.ui.reader

import androidx.compose.ui.graphics.Color

/** Reader color presets for plain-text books. */
enum class ReaderTheme(
    val backgroundColor: Color,
    val textColor: Color,
    val toolbarColor: Color,
) {
    LIGHT(
        backgroundColor = Color(0xFFFFFFFF),
        textColor = Color(0xFF1A1A1A),
        toolbarColor = Color(0xF2FFFFFF),
    ),
    DARK(
        backgroundColor = Color(0xFF1A1A1A),
        textColor = Color(0xFFD0D0D0),
        toolbarColor = Color(0xF2242424),
    ),
    CREAM(
        backgroundColor = Color(0xFFF5F0E8),
        textColor = Color(0xFF3D3222),
        toolbarColor = Color(0xF2EFE5D6),
    );
}
