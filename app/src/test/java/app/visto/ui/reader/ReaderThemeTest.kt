package app.visto.ui.reader

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderThemeTest {

    @Test
    fun lightThemeUsesWhiteBackgroundAndDarkText() {
        assertEquals(Color(0xFFFFFFFF), ReaderTheme.LIGHT.backgroundColor)
        assertEquals(Color(0xFF1A1A1A), ReaderTheme.LIGHT.textColor)
    }

    @Test
    fun darkThemeUsesDarkBackgroundAndLightText() {
        assertEquals(Color(0xFF1A1A1A), ReaderTheme.DARK.backgroundColor)
        assertEquals(Color(0xFFD0D0D0), ReaderTheme.DARK.textColor)
    }

    @Test
    fun creamThemeUsesWarmBackgroundAndBrownText() {
        assertEquals(Color(0xFFF5F0E8), ReaderTheme.CREAM.backgroundColor)
        assertEquals(Color(0xFF3D3222), ReaderTheme.CREAM.textColor)
    }
}
