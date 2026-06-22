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

    fun progressSummary(book: BookProgressEntity): String {
        val chapter = book.chapterTitle
            ?.takeIf { it.isNotBlank() }
            ?: Strings.bookshelfChapterNumber(book.chapterIndex)
        val progress = if (book.totalChapters > 0) {
            val percent = (((book.chapterIndex + 1).coerceAtLeast(1).toFloat() / book.totalChapters) * 100)
                .toInt()
                .coerceIn(0, 100)
            Strings.bookshelfProgressPercent(percent)
        } else {
            ""
        }
        return chapter + progress
    }

    fun readingProgressFraction(book: BookProgressEntity): Float {
        if (book.totalChapters <= 0) return 0f
        val currentChapter = (book.chapterIndex + 1).coerceAtLeast(1)
        return (currentChapter.toFloat() / book.totalChapters.toFloat()).coerceIn(0f, 1f)
    }

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
}
