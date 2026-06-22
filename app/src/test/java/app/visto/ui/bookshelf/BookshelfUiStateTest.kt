package app.visto.ui.bookshelf

import app.visto.data.db.BookProgressEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookshelfUiStateTest {

    private fun book(
        name: String = "小说.txt",
        chapterIndex: Int = 2,
        chapterTitle: String? = "第三章 风起",
        totalChapters: Int = 10,
        lastReadAt: Long = 1_700_000_000_000L,
    ) = BookProgressEntity(
        accountId = 1L,
        path = "/Books/$name",
        name = name,
        sizeBytes = 1024,
        etag = "etag",
        encoding = "UTF-8",
        chapterIndex = chapterIndex,
        chapterTitle = chapterTitle,
        pageOffset = 0,
        totalChapters = totalChapters,
        lastReadAt = lastReadAt,
        addedAt = lastReadAt,
    )

    @Test
    fun fromBooksMarksLoadedAndKeepsBooks() {
        val books = listOf(book(name = "a.txt"), book(name = "b.txt"))

        val state = BookshelfStateBuilder.fromBooks(books)

        assertEquals(books, state.books)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun fromFlowEmitsLoadingThenLoadedState() = runBlocking {
        val emissions = BookshelfStateBuilder.fromFlow(flowOf(listOf(book(name = "a.txt")))).toList()

        assertTrue(emissions.first().isLoading)
        assertEquals(listOf("a.txt"), emissions.last().books.map { it.name })
        assertFalse(emissions.last().isLoading)
    }

    @Test
    fun progressSummaryUsesChapterTitleAndPercent() {
        val summary = BookshelfStateBuilder.progressSummary(book(chapterIndex = 2, totalChapters = 10))

        assertEquals("第三章 风起 · 已读30%", summary)
    }

    @Test
    fun progressSummaryFallsBackToChapterNumber() {
        val summary = BookshelfStateBuilder.progressSummary(book(chapterIndex = 2, chapterTitle = null, totalChapters = 0))

        assertEquals("第3章", summary)
    }

    @Test
    fun relativeTimeFormatsCommonRanges() {
        val now = 1_700_000_000_000L

        assertEquals("刚刚", BookshelfStateBuilder.relativeLastReadTime(now - 30_000L, now))
        assertEquals("5分钟前", BookshelfStateBuilder.relativeLastReadTime(now - 5 * 60_000L, now))
        assertEquals("3小时前", BookshelfStateBuilder.relativeLastReadTime(now - 3 * 60 * 60_000L, now))
        assertEquals("2天前", BookshelfStateBuilder.relativeLastReadTime(now - 2 * 24 * 60 * 60_000L, now))
    }
}
