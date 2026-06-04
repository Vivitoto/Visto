package app.visto.ui.viewer

import org.junit.Assert.assertEquals
import org.junit.Test

class OriginalImageUiTextTest {

    @Test
    fun loadButtonOmitsSizeWhenUnknown() {
        assertEquals("加载原图", OriginalImageUiText.loadButtonLabel(null))
        assertEquals("加载原图", OriginalImageUiText.loadButtonLabel(0))
    }

    @Test
    fun loadButtonIncludesHumanReadableSize() {
        assertEquals("加载原图 · 8.4 MB", OriginalImageUiText.loadButtonLabel(8_808_038))
    }

    @Test
    fun fileDetailsIncludesNameAndSizeWhenKnown() {
        assertEquals("猫 图#a+b%.jpg · 512 B", OriginalImageUiText.fileDetails("猫 图#a+b%.jpg", 512))
    }

    @Test
    fun byteFormatterUsesExpectedUnits() {
        assertEquals("512 B", OriginalImageUiText.formatBytes(512))
        assertEquals("1.0 KB", OriginalImageUiText.formatBytes(1024))
        assertEquals("1.5 MB", OriginalImageUiText.formatBytes(1_572_864))
        assertEquals("2.0 GB", OriginalImageUiText.formatBytes(2_147_483_648))
    }
}
