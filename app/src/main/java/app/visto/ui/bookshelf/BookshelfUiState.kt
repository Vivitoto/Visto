package app.visto.ui.bookshelf

import app.visto.data.db.BookProgressEntity
import app.visto.ui.Strings
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

enum class BookshelfLayoutMode(val gridColumns: Int?) {
    LIST(null),
    GRID_3(3),
    GRID_5(5);

    fun next(): BookshelfLayoutMode = when (this) {
        LIST -> GRID_3
        GRID_3 -> GRID_5
        GRID_5 -> LIST
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

    private val displayTitleExtensions = setOf("txt", "md", "epub")

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
        return stripDisplayExtension(fileName(rawTitle))
    }

    fun coverTitlePresentation(book: BookProgressEntity): BookshelfCoverTitlePresentation =
        coverTitlePresentation(displayTitle(book))

    fun coverTitlePresentation(displayTitle: String): BookshelfCoverTitlePresentation {
        val title = displayTitle.trim()
        val bracketed = Regex("《([^》]+)》").find(title)
            ?: return BookshelfCoverTitlePresentation(title = title)
        val mainTitle = bracketed.groupValues[1].trim()
        if (mainTitle.isEmpty()) return BookshelfCoverTitlePresentation(title = title)
        val subtitle = title
            .removeRange(bracketed.range)
            .replace(Regex("\\s+"), " ")
            .trim()
            .takeIf { it.isNotEmpty() }
        return BookshelfCoverTitlePresentation(title = mainTitle, subtitle = subtitle)
    }

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

    private fun stripDisplayExtension(title: String): String {
        val trimmed = title.trim()
        val extension = supportedExtension(trimmed) ?: return trimmed
        val lastDot = fileName(trimmed).lastIndexOf('.')
        if (lastDot <= 0) return trimmed
        return trimmed.dropLast(extension.length + 1)
            .trimEnd()
            .ifBlank { trimmed }
    }

    private fun supportedExtension(value: String): String? {
        val name = fileName(value.trim())
        val lastDot = name.lastIndexOf('.')
        if (lastDot <= 0 || lastDot == name.lastIndex) return null
        val extension = name.substring(lastDot + 1).lowercase()
        return extension.takeIf { it in displayTitleExtensions }
    }

    private fun fileName(value: String): String =
        value.substringAfterLast('/').substringAfterLast('\\')
}
