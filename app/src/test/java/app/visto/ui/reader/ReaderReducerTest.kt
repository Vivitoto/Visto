package app.visto.ui.reader

import app.visto.core.book.Chapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ReaderReducerTest {

    @Test
    fun loadedBuildsPagesForCurrentChapter() {
        val session = emptySession()
        val next = ReaderReducer.reduce(
            session,
            ReaderAction.Loaded(
                encoding = "UTF-8",
                fullText = TEXT,
                chapters = chapters(),
                currentChapterIndex = 1,
                currentPage = 2,
            ),
        )

        assertFalse(next.isLoading)
        assertEquals("UTF-8", next.encoding)
        assertEquals(1, next.currentChapterIndex)
        assertTrue(next.pagesForCurrentChapter.isNotEmpty())
        assertTrue(next.currentPage in next.pagesForCurrentChapter.indices)
    }

    @Test
    fun goToChapterRecalculatesPagesAndResetsPage() {
        val session = loadedSession().copy(currentPage = 3)

        val next = ReaderReducer.reduce(session, ReaderAction.GoToChapter(1))

        assertEquals(1, next.currentChapterIndex)
        assertEquals(0, next.currentPage)
        assertEquals(TEXT.substring(chapters()[1].startOffset, chapters()[1].endOffset), next.pagesForCurrentChapter.joinToString("") { it.text })
    }

    @Test
    fun fontSizeAndLineSpacingRecalculateCurrentChapterPages() {
        val session = loadedSession()

        val largerFont = ReaderReducer.reduce(session, ReaderAction.SetFontSize(28))
        val spacious = ReaderReducer.reduce(session, ReaderAction.SetLineSpacing(2.0f))

        assertEquals(28, largerFont.fontSizeSp)
        assertEquals(2.0f, spacious.lineSpacing, 0.0f)
        assertTrue(largerFont.pagesForCurrentChapter.size >= session.pagesForCurrentChapter.size)
        assertTrue(spacious.pagesForCurrentChapter.size >= session.pagesForCurrentChapter.size)
    }

    @Test
    fun fontChoiceUpdatesSessionWithoutChangingChapter() {
        val session = loadedSession()

        val next = ReaderReducer.reduce(session, ReaderAction.SetFontChoice(ReaderFontChoice.Sans))

        assertEquals(ReaderFontChoice.Sans, next.fontChoice)
        assertEquals(session.currentChapterIndex, next.currentChapterIndex)
        assertTrue(next.pagesForCurrentChapter.isNotEmpty())
    }

    @Test
    fun pageMarginActionsUpdateClampedMargins() {
        val session = loadedSession()

        val top = ReaderReducer.reduce(session, ReaderAction.SetPageMarginTop(40))
        val bottom = ReaderReducer.reduce(top, ReaderAction.SetPageMarginBottom(500))
        val start = ReaderReducer.reduce(bottom, ReaderAction.SetPageMarginStart(36))
        val end = ReaderReducer.reduce(start, ReaderAction.SetPageMarginEnd(0))

        assertEquals(40, end.pageMargins.topDp)
        assertEquals(ReaderPageMargins.MAX_DP, end.pageMargins.bottomDp)
        assertEquals(36, end.pageMargins.startDp)
        assertEquals(ReaderPageMargins.MIN_DP, end.pageMargins.endDp)
        assertEquals(session.currentChapterIndex, end.currentChapterIndex)
        assertTrue(end.pagesForCurrentChapter.isNotEmpty())
    }

    @Test
    fun viewportChangeRecalculatesPagesWithoutChangingChapter() {
        val session = loadedSession()
        val viewport = ReaderViewport(widthPx = 160, heightPx = 200, density = 1f)

        val next = ReaderReducer.reduce(session, ReaderAction.SetViewport(viewport))

        assertEquals(viewport, next.viewport)
        assertEquals(session.currentChapterIndex, next.currentChapterIndex)
        assertTrue(next.pagesForCurrentChapter.isNotEmpty())
        assertEquals(
            TEXT.substring(chapters()[0].startOffset, chapters()[0].endOffset),
            next.pagesForCurrentChapter.joinToString("") { it.text },
        )
    }

    @Test
    fun pageActionsClampToAvailablePages() {
        val session = loadedSession().copy(currentPage = 0)

        val previous = ReaderReducer.reduce(session, ReaderAction.PrevPage)
        val next = ReaderReducer.reduce(session, ReaderAction.NextPage)
        val beyond = ReaderReducer.reduce(session, ReaderAction.GoToPage(999))

        assertEquals(0, previous.currentPage)
        assertEquals(1.coerceAtMost(session.pagesForCurrentChapter.lastIndex), next.currentPage)
        assertEquals(session.pagesForCurrentChapter.lastIndex, beyond.currentPage)
    }

    @Test
    fun themeColorBackgroundAndToolbarActionsUpdateState() {
        val session = loadedSession()

        val themed = ReaderReducer.reduce(session, ReaderAction.SetTheme(ReaderTheme.CREAM))
        val colored = ReaderReducer.reduce(session, ReaderAction.SetTextColor(ReaderTextColor.WARM_BROWN))
        val background = ReaderReducer.reduce(session, ReaderAction.SetBackgroundStyle(ReaderBackgroundStyle.NIGHT))
        val toggled = ReaderReducer.reduce(session, ReaderAction.ToggleToolbar)

        assertEquals(ReaderTheme.CREAM, themed.theme)
        assertEquals(ReaderTextColor.WARM_BROWN, colored.textColor)
        assertEquals(ReaderBackgroundStyle.NIGHT, background.backgroundStyle)
        assertEquals(!session.showToolbar, toggled.showToolbar)
    }

    private fun emptySession() = ReaderSession(
        filePath = "/books/a.txt",
        fileName = "a.txt",
        encoding = "",
        fullText = "",
        chapters = emptyList(),
    )

    private fun loadedSession(): ReaderSession = ReaderReducer.reduce(
        emptySession(),
        ReaderAction.Loaded(
            encoding = "UTF-8",
            fullText = TEXT,
            chapters = chapters(),
        ),
    )

    private fun chapters() = listOf(
        Chapter(index = 0, title = "第一章 开始", startOffset = 0, endOffset = TEXT.indexOf("第二章")),
        Chapter(index = 1, title = "第二章 继续", startOffset = TEXT.indexOf("第二章"), endOffset = TEXT.length),
    )

    private companion object {
        private val TEXT = "第一章 开始\n" + "这是第一章的正文。".repeat(80) + "\n第二章 继续\n" + "这是第二章的正文。".repeat(60)
    }
}
