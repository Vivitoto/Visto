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
            ?: "第${(book.chapterIndex + 1).coerceAtLeast(1)}章"
        val progress = if (book.totalChapters > 0) {
            val percent = (((book.chapterIndex + 1).coerceAtLeast(1).toFloat() / book.totalChapters) * 100)
                .toInt()
                .coerceIn(0, 100)
            " · 已读$percent%"
        } else {
            ""
        }
        return chapter + progress
    }

    fun relativeLastReadTime(lastReadAt: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - lastReadAt).coerceAtLeast(0L)
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour
        return when {
            diff < minute -> "刚刚"
            diff < hour -> "${diff / minute}分钟前"
            diff < day -> "${diff / hour}小时前"
            diff < 30 * day -> "${diff / day}天前"
            else -> "很久以前"
        }
    }
}
