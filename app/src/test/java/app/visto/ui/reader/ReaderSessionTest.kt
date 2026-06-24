package app.visto.ui.reader

import android.graphics.Typeface
import app.visto.core.book.Chapter
import app.visto.ui.book.bookDisplayTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
        assertEquals(ReaderPageMargins.DEFAULT, state.pageMargins)
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
        assertTrue(next.pagesForCurrentChapter.isNotEmpty())
        assertEquals(
            text.substring(chapters[0].startOffset, chapters[0].endOffset),
            next.pagesForCurrentChapter.joinToString("") { it.text },
        )
    }

    @Test
    fun loadResultRestoresByAbsolutePageStartCharThroughViewportChange() {
        val savedViewport = ReaderViewport(widthPx = 180, heightPx = 180, density = 1f)
        val saved = ReaderSessionReducer.reduce(
            ReaderSessionReducer.initial(loading = true).copy(viewport = savedViewport),
            ReaderSessionAction.LoadResult(
                filePath = "/books/a.txt",
                fileName = "a.txt",
                encoding = "UTF-8",
                fullText = text,
                chapters = chapters,
                initialChapterIndex = 1,
            ),
        )
        assertTrue(saved.pagesForCurrentChapter.size > 2)
        val savedPageIndex = (saved.pagesForCurrentChapter.lastIndex / 2).coerceAtLeast(1)
        val savedLocalStart = saved.pagesForCurrentChapter[savedPageIndex].startChar
        val savedAbsoluteStart = chapters[1].startOffset + savedLocalStart

        val restored = ReaderSessionReducer.reduce(
            ReaderSessionReducer.initial(loading = true),
            ReaderSessionAction.LoadResult(
                filePath = "/books/a.txt",
                fileName = "a.txt",
                encoding = "UTF-8",
                fullText = text,
                chapters = chapters,
                initialChapterIndex = 1,
                initialPage = 0,
                initialPageStartChar = savedAbsoluteStart,
            ),
        )
        val resized = ReaderSessionReducer.reduce(
            restored,
            ReaderSessionAction.SetViewport(ReaderViewport(widthPx = 260, heightPx = 240, density = 1f)),
        )
        val restoredPage = resized.pagesForCurrentChapter[resized.currentPage]

        assertEquals(1, resized.currentChapterIndex)
        assertTrue(savedLocalStart >= restoredPage.startChar)
        assertTrue(savedLocalStart <= restoredPage.endChar)
        assertEquals(savedAbsoluteStart, resized.currentAbsolutePageStartChar())
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
    fun setPageMarginsClampAndKeepCurrentReadingPosition() {
        val loaded = ReaderSessionReducer.reduce(loadedState(), ReaderSessionAction.NextPage)
        val originalPageStart = loaded.pagesForCurrentChapter[loaded.currentPage].startChar

        val widerStart = ReaderSessionReducer.reduce(loaded, ReaderSessionAction.SetPageMarginStart(500))
        val tighterEnd = ReaderSessionReducer.reduce(widerStart, ReaderSessionAction.SetPageMarginEnd(-2))

        assertEquals(ReaderPageMargins.HORIZONTAL_BASELINE_DP + ReaderPageMargins.EXTRA_MAX_DP, widerStart.pageMargins.startDp)
        assertEquals(ReaderPageMargins.HORIZONTAL_BASELINE_DP, tighterEnd.pageMargins.endDp)
        assertEquals(loaded.currentChapterIndex, tighterEnd.currentChapterIndex)
        assertTrue(originalPageStart >= tighterEnd.pagesForCurrentChapter[tighterEnd.currentPage].startChar)
        assertTrue(originalPageStart <= tighterEnd.pagesForCurrentChapter[tighterEnd.currentPage].endChar)
    }

    @Test
    fun readerFontChoiceStorageSanitizesCustomFileNames() {
        assertEquals(ReaderFontChoice.Sans, ReaderFontChoice.fromStorage("sans"))
        assertEquals(ReaderFontChoice.Custom("mine.ttf"), ReaderFontChoice.fromStorage("custom:mine.ttf"))
        assertEquals(ReaderFontChoice.SystemDefault, ReaderFontChoice.fromStorage("custom:../bad.ttf"))
        assertEquals(ReaderFontChoice.SystemDefault, ReaderFontChoice.fromStorage("custom:bad.txt"))
    }

    @Test
    fun paginationTypefaceMatchesBuiltInFontChoice() {
        assertNull(ReaderFontChoice.SystemDefault.paginationTypeface())
        assertSame(Typeface.SANS_SERIF, ReaderFontChoice.Sans.paginationTypeface())
        assertSame(Typeface.SERIF, ReaderFontChoice.Serif.paginationTypeface())
        assertNull(ReaderFontChoice.Custom("mine.ttf").paginationTypeface())
    }

    @Test
    fun readerChromeDisplayTitleMatchesBookshelfBookNameLogic() {
        assertEquals("demo", bookDisplayTitle("demo.txt"))
        assertEquals("README", bookDisplayTitle("README.MD"))
        assertEquals("三国演义", bookDisplayTitle("《三国演义》 罗贯中.txt"))
        assertEquals("archive.pdf", bookDisplayTitle("archive.pdf"))
        assertEquals("", bookDisplayTitle(""))
        assertEquals(".hidden", bookDisplayTitle(".hidden"))
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
    fun progressEstimatorCombinesChapterAndPagePosition() {
        assertEquals(
            25,
            ReaderProgressEstimator.percent(
                currentChapterIndex = 0,
                currentPage = 1,
                currentChapterPageCount = 4,
                totalChapters = 2,
            ),
        )
        assertEquals(
            75,
            ReaderProgressEstimator.percent(
                currentChapterIndex = 1,
                currentPage = 1,
                currentChapterPageCount = 4,
                totalChapters = 2,
            ),
        )
        assertEquals(
            100,
            ReaderProgressEstimator.percent(
                currentChapterIndex = 99,
                currentPage = 99,
                currentChapterPageCount = 4,
                totalChapters = 2,
            ),
        )
    }

    @Test
    fun progressEstimatorClampsEmptyInputs() {
        assertEquals(
            0,
            ReaderProgressEstimator.percent(
                currentChapterIndex = 0,
                currentPage = 0,
                currentChapterPageCount = 0,
                totalChapters = 0,
            ),
        )
        assertEquals(
            50,
            ReaderProgressEstimator.percent(
                currentChapterIndex = -1,
                currentPage = -1,
                currentChapterPageCount = 0,
                totalChapters = 2,
            ),
        )
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
