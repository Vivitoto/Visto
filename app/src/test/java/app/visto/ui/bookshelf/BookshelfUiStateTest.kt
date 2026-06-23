package app.visto.ui.bookshelf

import app.visto.data.db.BookProgressEntity
import androidx.compose.ui.unit.dp
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
        id: Long = 0L,
        accountId: Long = 1L,
        name: String = "小说.txt",
        chapterIndex: Int = 2,
        chapterTitle: String? = "第三章 风起",
        totalChapters: Int = 10,
        lastReadAt: Long = 1_700_000_000_000L,
        path: String = "/Books/$name",
    ) = BookProgressEntity(
        id = id,
        accountId = accountId,
        path = path,
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
    fun layoutModeCyclesListStandardGridCompactGridThenList() {
        assertEquals(BookshelfLayoutMode.GRID_STANDARD, BookshelfLayoutMode.LIST.next())
        assertEquals(BookshelfLayoutMode.GRID_COMPACT, BookshelfLayoutMode.GRID_STANDARD.next())
        assertEquals(BookshelfLayoutMode.LIST, BookshelfLayoutMode.GRID_COMPACT.next())
    }

    @Test
    fun layoutModeUsesAdaptiveBookshelfGridCellWidths() {
        assertNull(BookshelfLayoutMode.LIST.gridMinCellWidth)
        assertEquals(112.dp, BookshelfLayoutMode.GRID_STANDARD.gridMinCellWidth)
        assertEquals(72.dp, BookshelfLayoutMode.GRID_COMPACT.gridMinCellWidth)
    }

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
    fun displayTitleStripsSupportedExtensionsCaseInsensitively() {
        assertEquals("小说", BookshelfStateBuilder.displayTitle(book(name = "小说.txt")))
        assertEquals("README", BookshelfStateBuilder.displayTitle(book(name = "README.MD")))
        assertEquals("长篇中文标题", BookshelfStateBuilder.displayTitle(book(name = "长篇中文标题.EpUb")))
        assertEquals("archive.pdf", BookshelfStateBuilder.displayTitle(book(name = "archive.pdf")))
    }

    @Test
    fun displayTitleFallsBackToPathWhenNameIsBlank() {
        val displayTitle = BookshelfStateBuilder.displayTitle(
            book(
                name = "",
                path = "/Books/很长的中文书名.epub",
            ),
        )

        assertEquals("很长的中文书名", displayTitle)
    }

    @Test
    fun coverTitlePresentationSplitsChineseBookBrackets() {
        val coverTitle = BookshelfStateBuilder.coverTitlePresentation(
            book(name = "《三国演义》 罗贯中.txt"),
        )

        assertEquals("三国演义", coverTitle.title)
        assertEquals("罗贯中", coverTitle.subtitle)
        assertEquals("《三国演义》 罗贯中", BookshelfStateBuilder.displayTitle(book(name = "《三国演义》 罗贯中.txt")))
    }

    @Test
    fun coverTitlePresentationUsesWholeTitleWithoutChineseBookBrackets() {
        val coverTitle = BookshelfStateBuilder.coverTitlePresentation(book(name = "普通标题.txt"))

        assertEquals("普通标题", coverTitle.title)
        assertNull(coverTitle.subtitle)
    }

    @Test
    fun coverTitlePresentationKeepsRemainingTextAsSubtitle() {
        val coverTitle = BookshelfStateBuilder.coverTitlePresentation("前缀 《书名》 后缀")

        assertEquals("书名", coverTitle.title)
        assertEquals("前缀 后缀", coverTitle.subtitle)
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
    fun readingProgressFractionClampsToBounds() {
        assertEquals(0.3f, BookshelfStateBuilder.readingProgressFraction(book(chapterIndex = 2, totalChapters = 10)), 0.0001f)
        assertEquals(0f, BookshelfStateBuilder.readingProgressFraction(book(totalChapters = 0)), 0.0001f)
        assertEquals(1f, BookshelfStateBuilder.readingProgressFraction(book(chapterIndex = 999, totalChapters = 10)), 0.0001f)
    }

    @Test
    fun readingProgressPercentLabelUsesChapterCountSemantics() {
        assertEquals("已读30%", BookshelfStateBuilder.readingProgressPercentLabel(book(chapterIndex = 2, totalChapters = 10)))
        assertEquals("已读100%", BookshelfStateBuilder.readingProgressPercentLabel(book(chapterIndex = 999, totalChapters = 10)))
        assertNull(BookshelfStateBuilder.readingProgressPercentLabel(book(totalChapters = 0)))
    }

    @Test
    fun stableBookKeyUsesIdOrAccountPathFallback() {
        assertEquals("book:42", BookshelfStateBuilder.stableBookKey(book(id = 42L)))
        assertEquals(
            "book:7:/Books/a.txt",
            BookshelfStateBuilder.stableBookKey(book(accountId = 7L, name = "a.txt")),
        )
    }

    @Test
    fun coverPaletteIndexIsStableAndBounded() {
        val book = book(accountId = 7L, name = "a.txt")
        val index = BookshelfStateBuilder.coverPaletteIndex(book, paletteSize = 6)

        assertEquals(index, BookshelfStateBuilder.coverPaletteIndex(book, paletteSize = 6))
        assertTrue(index in 0 until 6)
        assertEquals(0, BookshelfStateBuilder.coverPaletteIndex(book, paletteSize = 0))
    }

    @Test
    fun coverPresentationClassifiesCurrentCoversAsGeneratedPlaceholders() {
        val epubCover = BookshelfStateBuilder.coverPresentation(book(name = "小说.EPUB"))
        val markdownCover = BookshelfStateBuilder.coverPresentation(book(name = "notes", path = "/Books/notes.md"))
        val unknownCover = BookshelfStateBuilder.coverPresentation(book(name = "scan.pdf"))

        assertEquals(BookshelfCoverSource.GENERATED_PLACEHOLDER, epubCover.source)
        assertEquals(BookshelfBookFileType.EPUB, epubCover.fileType)
        assertEquals("EPUB", epubCover.fileType.coverBadge)
        assertTrue(epubCover.fileType.canHaveEmbeddedCover)

        assertEquals(BookshelfCoverSource.GENERATED_PLACEHOLDER, markdownCover.source)
        assertEquals(BookshelfBookFileType.MARKDOWN, markdownCover.fileType)
        assertEquals("MD", markdownCover.fileType.coverBadge)

        assertEquals(BookshelfCoverSource.GENERATED_PLACEHOLDER, unknownCover.source)
        assertEquals(BookshelfBookFileType.UNKNOWN, unknownCover.fileType)
        assertFalse(unknownCover.fileType.canHaveEmbeddedCover)
    }

    @Test
    fun coverPresentationPrefersEmbeddedCoverOnlyForEpubWhenAvailable() {
        val epubCover = BookshelfStateBuilder.coverPresentation(
            book(name = "小说.epub"),
            hasEmbeddedCover = true,
        )
        val textCover = BookshelfStateBuilder.coverPresentation(
            book(name = "小说.txt"),
            hasEmbeddedCover = true,
        )

        assertEquals(BookshelfCoverSource.EPUB_EMBEDDED, epubCover.source)
        assertEquals(BookshelfBookFileType.EPUB, epubCover.fileType)
        assertEquals(BookshelfCoverSource.GENERATED_PLACEHOLDER, textCover.source)
        assertEquals(BookshelfBookFileType.TXT, textCover.fileType)
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
