package app.visto.ui.bookshelf

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.visto.data.db.BookProgressEntity
import app.visto.ui.Strings
import app.visto.ui.book.bookDisplayTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** Visible state for the local bookshelf tab. */
data class BookshelfUiState(
    val books: List<BookProgressEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isScanning: Boolean = false,
    val scanMessage: String? = null,
)

enum class BookshelfLayoutMode(
    val gridMinCellWidth: Dp?,
    val displayLabel: String?,
) {
    LIST(null, null),
    GRID_STANDARD(112.dp, "标准"),
    GRID_COMPACT(72.dp, "紧凑");

    fun next(): BookshelfLayoutMode = when (this) {
        LIST -> GRID_STANDARD
        GRID_STANDARD -> GRID_COMPACT
        GRID_COMPACT -> LIST
    }
}

enum class BookshelfBookFileType(
    val coverBadge: String,
    val canHaveEmbeddedCover: Boolean,
) {
    TXT("TXT", false),
    MARKDOWN("MD", false),
    EPUB("EPUB", true),
    UNKNOWN("BOOK", false),
}

enum class BookshelfCoverSource {
    GENERATED_PLACEHOLDER,
    EPUB_EMBEDDED,
}

data class BookshelfCoverPresentation(
    val source: BookshelfCoverSource,
    val fileType: BookshelfBookFileType,
)

data class BookshelfCoverTitlePresentation(
    val title: String,
    val subtitle: String? = null,
)

/** Pure state builder/reducer helpers for bookshelf data and display text. */
object BookshelfStateBuilder {

    fun fromBooks(books: List<BookProgressEntity>): BookshelfUiState = BookshelfUiState(
        books = books,
        isLoading = false,
        errorMessage = null,
    )

    fun fromFlow(flow: Flow<List<BookProgressEntity>>): Flow<BookshelfUiState> = flow
        .map { fromBooks(it) }
        .onStart { emit(BookshelfUiState(isLoading = true)) }
        .catch { e ->
            emit(
                BookshelfUiState(
                    isLoading = false,
                    errorMessage = e.message ?: Strings.ERR_UNEXPECTED,
                )
            )
        }

    fun displayTitle(book: BookProgressEntity): String {
        val rawTitle = book.name
            .takeIf { it.isNotBlank() }
            ?: book.path
        return bookDisplayTitle(rawTitle)
    }

    fun coverTitlePresentation(book: BookProgressEntity): BookshelfCoverTitlePresentation =
        coverTitlePresentation(displayTitle(book))

    fun coverTitlePresentation(displayTitle: String): BookshelfCoverTitlePresentation =
        BookshelfCoverTitlePresentation(title = bookDisplayTitle(displayTitle))

    fun progressSummary(book: BookProgressEntity): String {
        val chapter = book.chapterTitle
            ?.takeIf { it.isNotBlank() }
            ?: Strings.bookshelfChapterNumber(book.chapterIndex)
        val progress = readingProgressPercent(book)
            ?.let(Strings::bookshelfProgressPercent)
            ?: ""
        return chapter + progress
    }

    fun readingProgressFraction(book: BookProgressEntity): Float {
        if (book.totalChapters <= 0) return 0f
        val currentChapter = (book.chapterIndex + 1).coerceAtLeast(1)
        return (currentChapter.toFloat() / book.totalChapters.toFloat()).coerceIn(0f, 1f)
    }

    fun readingProgressPercentLabel(book: BookProgressEntity): String? =
        readingProgressPercent(book)?.let(Strings::bookshelfCoverProgressPercent)

    fun stableBookKey(book: BookProgressEntity): String =
        if (book.id != 0L) {
            "book:${book.id}"
        } else {
            "book:${book.accountId}:${book.path}"
        }

    fun coverPaletteIndex(book: BookProgressEntity, paletteSize: Int): Int {
        if (paletteSize <= 0) return 0
        return Math.floorMod("${book.accountId}:${book.path}:${book.name}".hashCode(), paletteSize)
    }

    fun coverPresentation(
        book: BookProgressEntity,
        hasEmbeddedCover: Boolean = false,
    ): BookshelfCoverPresentation {
        val fileType = bookFileType(book)
        return BookshelfCoverPresentation(
            source = if (fileType.canHaveEmbeddedCover && hasEmbeddedCover) {
                BookshelfCoverSource.EPUB_EMBEDDED
            } else {
                BookshelfCoverSource.GENERATED_PLACEHOLDER
            },
            fileType = fileType,
        )
    }

    fun bookFileType(book: BookProgressEntity): BookshelfBookFileType {
        val extension = supportedExtension(book.name)
            ?: supportedExtension(book.path)
            ?: return BookshelfBookFileType.UNKNOWN
        return when (extension) {
            "txt" -> BookshelfBookFileType.TXT
            "md" -> BookshelfBookFileType.MARKDOWN
            "epub" -> BookshelfBookFileType.EPUB
            else -> BookshelfBookFileType.UNKNOWN
        }
    }

    fun relativeLastReadTime(lastReadAt: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - lastReadAt).coerceAtLeast(0L)
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour
        return when {
            diff < minute -> Strings.BOOKSHELF_JUST_NOW
            diff < hour -> Strings.bookshelfMinutesAgo(diff / minute)
            diff < day -> Strings.bookshelfHoursAgo(diff / hour)
            diff < 30 * day -> Strings.bookshelfDaysAgo(diff / day)
            else -> Strings.BOOKSHELF_LONG_AGO
        }
    }

    private fun readingProgressPercent(book: BookProgressEntity): Int? {
        if (book.totalChapters <= 0) return null
        return (((book.chapterIndex + 1).coerceAtLeast(1).toFloat() / book.totalChapters) * 100)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun supportedExtension(value: String): String? {
        val name = value.trim().substringAfterLast('/').substringAfterLast('\\')
        val lastDot = name.lastIndexOf('.')
        if (lastDot <= 0 || lastDot == name.lastIndex) return null
        val extension = name.substring(lastDot + 1).lowercase()
        return extension.takeIf { it in setOf("txt", "md", "epub") }
    }
}
