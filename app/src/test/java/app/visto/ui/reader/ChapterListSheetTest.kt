package app.visto.ui.reader

import app.visto.core.book.Chapter
import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterListSheetTest {

    @Test
    fun matchingChapterIndicesFiltersByTitle() {
        val chapters = chapters()

        val matches = matchingChapterIndices(chapters, "second")

        assertEquals(listOf(1), matches)
    }

    @Test
    fun matchingChapterIndicesFiltersByOneBasedChapterNumber() {
        val chapters = chapters()

        val matches = matchingChapterIndices(chapters, "3")

        assertEquals(listOf(2), matches)
    }

    @Test
    fun matchingChapterIndicesReturnsAllChaptersForBlankQuery() {
        val chapters = chapters()

        val matches = matchingChapterIndices(chapters, "  ")

        assertEquals(listOf(0, 1, 2), matches)
    }

    private fun chapters(): List<Chapter> = listOf(
        Chapter(index = 0, title = "第一章 开始", startOffset = 0, endOffset = 10),
        Chapter(index = 1, title = "Chapter 2 Second", startOffset = 10, endOffset = 20),
        Chapter(index = 2, title = "第三章 结束", startOffset = 20, endOffset = 30),
    )
}
