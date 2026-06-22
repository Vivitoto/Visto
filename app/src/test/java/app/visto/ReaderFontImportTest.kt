package app.visto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderFontImportTest {

    @Test
    fun importedReaderFontFileNameKeepsOnlySupportedFontExtensions() {
        assertEquals("42_My_Font.ttf", importedReaderFontFileName("My Font.ttf", nowMillis = 42))
        assertEquals("42_song.otf", importedReaderFontFileName("song.otf", nowMillis = 42))
        assertNull(importedReaderFontFileName("cover.jpg", nowMillis = 42))
    }

    @Test
    fun importedReaderFontFileNameSanitizesUnsafeDisplayNames() {
        assertEquals("7_bad_name.ttf", importedReaderFontFileName("../bad name.ttf", nowMillis = 7))
        assertEquals("7_font.otf", importedReaderFontFileName("  .otf", nowMillis = 7))
    }
}
