package app.visto.ui.reader

import app.visto.core.book.Chapter
import app.visto.core.book.Page
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
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
        val loaded = ReaderSessionReducer.reduce(loadedState(), ReaderSessionAction.NextPage)
        val originalPageStart = loaded.pagesForCurrentChapter[loaded.currentPage].startChar

        val next = ReaderSessionReducer.reduce(loaded, ReaderSessionAction.SetFontSize(28))
        val newPage = next.pagesForCurrentChapter[next.currentPage]

        assertEquals(28, next.fontSizeSp)
        assertTrue(next.pagesForCurrentChapter.size >= loaded.pagesForCurrentChapter.size)
        assertEquals(loaded.currentChapterIndex, next.currentChapterIndex)
        assertTrue(originalPageStart >= newPage.startChar)
        assertTrue(originalPageStart <= newPage.endChar)
    }

    @Test
    fun setViewportRepaginatesWithDynamicDimensions() {
        val loaded = loadedState()
        val viewport = ReaderViewport(widthPx = 160, heightPx = 220, density = 1f)

        val next = ReaderSessionReducer.reduce(loaded, ReaderSessionAction.SetViewport(viewport))

        assertEquals(viewport, next.viewport)
        assertEquals(loaded.currentChapterIndex, next.currentChapterIndex)
        assertTrue(next.pagesForCurrentChapter.size > loaded.pagesForCurrentChapter.size)
    }

    @Test
    fun setFontChoicePersistsChoiceAndKeepsCurrentReadingPosition() {
        val loaded = ReaderSessionReducer.reduce(loadedState(), ReaderSessionAction.NextPage)
        val originalPageStart = loaded.pagesForCurrentChapter[loaded.currentPage].startChar

        val next = ReaderSessionReducer.reduce(loaded, ReaderSessionAction.SetFontChoice(ReaderFontChoice.Serif))

        assertEquals(ReaderFontChoice.Serif, next.fontChoice)
        assertEquals(loaded.currentChapterIndex, next.currentChapterIndex)
        assertTrue(originalPageStart >= next.pagesForCurrentChapter[next.currentPage].startChar)
        assertTrue(originalPageStart <= next.pagesForCurrentChapter[next.currentPage].endChar)
    }

    @Test
    fun readerFontChoiceStorageSanitizesCustomFileNames() {
        assertEquals(ReaderFontChoice.Sans, ReaderFontChoice.fromStorage("sans"))
        assertEquals(ReaderFontChoice.Custom("mine.ttf"), ReaderFontChoice.fromStorage("custom:mine.ttf"))
        assertEquals(ReaderFontChoice.SystemDefault, ReaderFontChoice.fromStorage("custom:../bad.ttf"))
        assertEquals(ReaderFontChoice.SystemDefault, ReaderFontChoice.fromStorage("custom:bad.txt"))
    }

    @Test
    fun viewportUpdateWhileLoadingKeepsSavedPage() {
        val loading = ReaderSessionReducer.initial(loading = true).copy(currentPage = 8)
        val viewport = ReaderViewport(widthPx = 240, heightPx = 320, density = 2f)

        val next = ReaderSessionReducer.reduce(loading, ReaderSessionAction.SetViewport(viewport))

        assertTrue(next.isLoading)
        assertEquals(8, next.currentPage)
        assertEquals(viewport, next.viewport)
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

    @Test
    fun setTextColorAndBackgroundUpdatePaletteWithoutRepaginating() {
        val loaded = loadedState()

        val colored = ReaderSessionReducer.reduce(loaded, ReaderSessionAction.SetTextColor(ReaderTextColor.INK))
        val background = ReaderSessionReducer.reduce(loaded, ReaderSessionAction.SetBackgroundStyle(ReaderBackgroundStyle.BLACK))

        assertEquals(ReaderTextColor.INK, colored.textColor)
        assertEquals(loaded.pagesForCurrentChapter, colored.pagesForCurrentChapter)
        assertEquals(ReaderBackgroundStyle.BLACK, background.backgroundStyle)
        assertEquals(loaded.pagesForCurrentChapter, background.pagesForCurrentChapter)
        assertTrue(background.readerPalette().isDark)
    }

    @Test
    fun pagePresenterStylesChapterTitleOnlyAtChapterStart() {
        val page = Page(
            startChar = 0,
            endChar = 15,
            text = "第一章 开始\n这是正文",
        )

        val styled = ReaderPagePresenter.present(page, chapters[0])
        val normal = ReaderPagePresenter.present(page.copy(startChar = 4), chapters[0])

        assertTrue(styled.hasStyledTitle)
        assertEquals("第一章 开始", styled.title)
        assertEquals("这是正文", styled.body)
        assertFalse(normal.hasStyledTitle)
        assertEquals(page.text, normal.body)
    }

    @Test
    fun pagePresenterFallsBackWhenFirstLineIsNotChapterTitle() {
        val page = Page(
            startChar = 0,
            endChar = 10,
            text = "序言\n第一章 开始",
        )

        val presentation = ReaderPagePresenter.present(page, chapters[0])

        assertFalse(presentation.hasStyledTitle)
        assertEquals(page.text, presentation.body)
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
