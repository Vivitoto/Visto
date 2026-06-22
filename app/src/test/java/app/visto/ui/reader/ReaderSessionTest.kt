package app.visto.ui.reader

import app.visto.core.book.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderSessionTest {

    private val text = "第一章 开始\n" + "这是第一章的内容。".repeat(160) + "\n第二章 继续\n" + "这是第二章的内容。".repeat(80)
    private val chapters = listOf(
        Chapter(index = 0, title = "第一章 开始", startOffset = 0, endOffset = text.indexOf("第二章")),
        Chapter(index = 1, title = "第二章 继续", startOffset = text.indexOf("第二章"), endOffset = text.length),
    )

    @Test
    fun initialCanRepresentLoadingState() {
        val state = ReaderSessionReducer.initial(loading = true)

        assertTrue(state.isLoading)
        assertEquals("", state.filePath)
        assertEquals(ReaderTheme.LIGHT, state.theme)
    }

    @Test
    fun loadResultBuildsReadableSessionPages() {
        val state = ReaderSessionReducer.reduce(
            ReaderSessionReducer.initial(loading = true),
            ReaderSessionAction.LoadResult(
                filePath = "/books/a.txt",
                fileName = "a.txt",
                encoding = "UTF-8",
                fullText = text,
                chapters = chapters,
            ),
        )

        assertFalse(state.isLoading)
        assertEquals("/books/a.txt", state.filePath)
        assertEquals(2, state.chapters.size)
        assertEquals(0, state.currentChapterIndex)
        assertEquals(0, state.currentPage)
        assertTrue(state.pagesForCurrentChapter.isNotEmpty())
    }

    @Test
    fun setFontSizeRepaginatesCurrentChapter() {
        val loaded = loadedState()

        val next = ReaderSessionReducer.reduce(loaded, ReaderSessionAction.SetFontSize(28))

        assertEquals(28, next.fontSizeSp)
        assertTrue(next.pagesForCurrentChapter.size >= loaded.pagesForCurrentChapter.size)
        assertEquals(0, next.currentPage)
    }

    @Test
    fun selectChapterResetsCurrentPageAndRepaginates() {
        val loaded = ReaderSessionReducer.reduce(loadedState(), ReaderSessionAction.NextPage)

        val next = ReaderSessionReducer.reduce(loaded, ReaderSessionAction.SelectChapter(1))

        assertEquals(1, next.currentChapterIndex)
        assertEquals(0, next.currentPage)
        assertEquals(chapters[1].title, next.chapters[next.currentChapterIndex].title)
    }

    @Test
    fun prevAndNextPageStayWithinBounds() {
        val loaded = loadedState()

        val firstPrev = ReaderSessionReducer.reduce(loaded, ReaderSessionAction.PrevPage)
        assertEquals(0, firstPrev.currentPage)

        val next = ReaderSessionReducer.reduce(loaded, ReaderSessionAction.NextPage)
        assertEquals(1.coerceAtMost(loaded.pagesForCurrentChapter.lastIndex), next.currentPage)
    }

    @Test
    fun setThemeUpdatesThemeWithoutRepaginating() {
        val loaded = loadedState()

        val next = ReaderSessionReducer.reduce(loaded, ReaderSessionAction.SetTheme(ReaderTheme.CREAM))

        assertEquals(ReaderTheme.CREAM, next.theme)
        assertEquals(loaded.pagesForCurrentChapter, next.pagesForCurrentChapter)
    }

    private fun loadedState(): ReaderSession = ReaderSessionReducer.reduce(
        ReaderSessionReducer.initial(loading = true),
        ReaderSessionAction.LoadResult(
            filePath = "/books/a.txt",
            fileName = "a.txt",
            encoding = "UTF-8",
            fullText = text,
            chapters = chapters,
        ),
    )
}
