package app.visto.ui.reader

import app.visto.core.book.Chapter
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterListScreenTest {

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

    @Test
    fun initialChapterListScrollIndexUsesCurrentChapter() {
        assertEquals(2, initialChapterListScrollIndex(chapterCount = 5, currentIndex = 2))
    }

    @Test
    fun initialChapterListScrollIndexClampsInvalidCurrentChapter() {
        assertEquals(0, initialChapterListScrollIndex(chapterCount = 5, currentIndex = -1))
        assertEquals(4, initialChapterListScrollIndex(chapterCount = 5, currentIndex = 99))
        assertEquals(0, initialChapterListScrollIndex(chapterCount = 0, currentIndex = 3))
    }

    @Test
    fun selectedChapterRowColorsUsePrimaryContainerColors() {
        val colorScheme = testColorScheme()

        val colors = chapterListRowColors(selected = true, colorScheme)

        assertEquals(colorScheme.primaryContainer, colors.containerColor)
        assertEquals(colorScheme.onPrimaryContainer, colors.headlineColor)
    }

    @Test
    fun unselectedChapterRowColorsUseSurfaceColors() {
        val colorScheme = testColorScheme()

        val colors = chapterListRowColors(selected = false, colorScheme)

        assertEquals(colorScheme.surface, colors.containerColor)
        assertEquals(colorScheme.onSurface, colors.headlineColor)
    }

    private fun chapters(): List<Chapter> = listOf(
        Chapter(index = 0, title = "第一章 开始", startOffset = 0, endOffset = 10),
        Chapter(index = 1, title = "Chapter 2 Second", startOffset = 10, endOffset = 20),
        Chapter(index = 2, title = "第三章 结束", startOffset = 20, endOffset = 30),
    )

    private fun testColorScheme() = lightColorScheme(
        primaryContainer = Color(0xFF112233),
        onPrimaryContainer = Color(0xFF445566),
        surface = Color(0xFF778899),
        onSurface = Color(0xFFAABBCC),
    )
}
