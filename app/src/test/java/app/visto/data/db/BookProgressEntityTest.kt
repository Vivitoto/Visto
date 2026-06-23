package app.visto.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

class BookProgressEntityTest {

    @Test
    fun defaultsMatchReaderDesign() {
        val entity = BookProgressEntity(
            accountId = 7,
            path = "/Books/demo.txt",
            name = "demo.txt",
            sizeBytes = 1024,
            etag = "etag-1",
            encoding = "UTF-8",
            chapterTitle = null,
            lastReadAt = 100,
            addedAt = 50,
        )

        assertEquals(0, entity.id)
        assertEquals(7, entity.accountId)
        assertEquals("/Books/demo.txt", entity.path)
        assertEquals("demo.txt", entity.name)
        assertEquals(1024L, entity.sizeBytes)
        assertEquals("etag-1", entity.etag)
        assertEquals("UTF-8", entity.encoding)
        assertEquals(0, entity.chapterIndex)
        assertEquals(null, entity.chapterTitle)
        assertEquals(0, entity.pageOffset)
        assertEquals(0, entity.totalChapters)
        assertEquals(18, entity.fontSizeSp)
        assertEquals(1.5f, entity.lineSpacing, 0.0001f)
        assertEquals("light", entity.theme)
        assertEquals("system", entity.fontChoice)
        assertEquals("default", entity.textColor)
        assertEquals("default", entity.backgroundStyle)
        assertEquals(28, entity.pageMarginTopDp)
        assertEquals(54, entity.pageMarginBottomDp)
        assertEquals(22, entity.pageMarginStartDp)
        assertEquals(22, entity.pageMarginEndDp)
        assertEquals(100L, entity.lastReadAt)
        assertEquals(50L, entity.addedAt)
    }

    @Test
    fun customReaderPreferencesCanBeStoredOnEntity() {
        val entity = BookProgressEntity(
            accountId = 7,
            path = "/Books/demo.txt",
            name = "demo.txt",
            sizeBytes = 1024,
            etag = "etag-1",
            encoding = "UTF-8",
            chapterTitle = "第一章",
            chapterIndex = 3,
            pageOffset = 8,
            totalChapters = 20,
            fontSizeSp = 22,
            lineSpacing = 2.0f,
            theme = "cream",
            fontChoice = "serif",
            textColor = "ink",
            backgroundStyle = "paper",
            pageMarginTopDp = 34,
            pageMarginBottomDp = 72,
            pageMarginStartDp = 30,
            pageMarginEndDp = 38,
            lastReadAt = 100,
            addedAt = 50,
        )

        assertEquals(3, entity.chapterIndex)
        assertEquals("第一章", entity.chapterTitle)
        assertEquals(8, entity.pageOffset)
        assertEquals(20, entity.totalChapters)
        assertEquals(22, entity.fontSizeSp)
        assertEquals(2.0f, entity.lineSpacing, 0.0001f)
        assertEquals("cream", entity.theme)
        assertEquals("serif", entity.fontChoice)
        assertEquals("ink", entity.textColor)
        assertEquals("paper", entity.backgroundStyle)
        assertEquals(34, entity.pageMarginTopDp)
        assertEquals(72, entity.pageMarginBottomDp)
        assertEquals(30, entity.pageMarginStartDp)
        assertEquals(38, entity.pageMarginEndDp)
    }
}
