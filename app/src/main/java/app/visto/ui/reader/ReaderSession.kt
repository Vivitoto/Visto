package app.visto.ui.reader

import app.visto.core.book.Chapter
import app.visto.core.book.Page
import app.visto.core.book.TextPaginator

data class ReaderSession(
    val filePath: String,
    val fileName: String,
    val encoding: String,
    val fullText: String,
    val chapters: List<Chapter>,
    val currentChapterIndex: Int = 0,
    val currentPage: Int = 0,
    val pagesForCurrentChapter: List<Page> = emptyList(),
    val fontSizeSp: Int = 18,
    val lineSpacing: Float = 1.5f,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showToolbar: Boolean = true,
)

sealed class ReaderAction {
    data class Loaded(
        val encoding: String,
        val fullText: String,
        val chapters: List<Chapter>,
        val currentChapterIndex: Int = 0,
        val currentPage: Int = 0,
    ) : ReaderAction()

    data object ToggleToolbar : ReaderAction()
    data class GoToPage(val page: Int) : ReaderAction()
    data class GoToChapter(val index: Int) : ReaderAction()
    data class SetFontSize(val sp: Int) : ReaderAction()
    data class SetLineSpacing(val spacing: Float) : ReaderAction()
    data class SetTheme(val theme: ReaderTheme) : ReaderAction()
    data object NextPage : ReaderAction()
    data object PrevPage : ReaderAction()
    data object Retry : ReaderAction()
    data class Error(val message: String) : ReaderAction()
}

object ReaderReducer {
    private const val PAGE_WIDTH_PX = 360f
    private const val PAGE_HEIGHT_PX = 560f
    private const val DENSITY = 1f

    fun reduce(session: ReaderSession, action: ReaderAction): ReaderSession = when (action) {
        is ReaderAction.Loaded -> {
            val chapters = action.chapters.ifEmpty { session.chapters }
            val chapterIndex = action.currentChapterIndex.coerceInChapterRange(chapters)
            val pages = paginateChapter(
                fullText = action.fullText,
                chapters = chapters,
                chapterIndex = chapterIndex,
                fontSizeSp = session.fontSizeSp,
                lineSpacing = session.lineSpacing,
            )
            session.copy(
                encoding = action.encoding,
                fullText = action.fullText,
                chapters = chapters,
                currentChapterIndex = chapterIndex,
                currentPage = action.currentPage.coerceInPageRange(pages),
                pagesForCurrentChapter = pages,
                isLoading = false,
                errorMessage = null,
            )
        }
        ReaderAction.ToggleToolbar -> session.copy(showToolbar = !session.showToolbar)
        is ReaderAction.GoToPage -> session.copy(
            currentPage = action.page.coerceInPageRange(session.pagesForCurrentChapter),
        )
        is ReaderAction.GoToChapter -> {
            val chapterIndex = action.index.coerceInChapterRange(session.chapters)
            val pages = paginateChapter(session.fullText, session.chapters, chapterIndex, session.fontSizeSp, session.lineSpacing)
            session.copy(
                currentChapterIndex = chapterIndex,
                currentPage = 0,
                pagesForCurrentChapter = pages,
            )
        }
        is ReaderAction.SetFontSize -> {
            val fontSize = action.sp.coerceIn(14, 28)
            val pages = paginateChapter(session.fullText, session.chapters, session.currentChapterIndex, fontSize, session.lineSpacing)
            session.copy(
                fontSizeSp = fontSize,
                pagesForCurrentChapter = pages,
                currentPage = session.currentPage.coerceInPageRange(pages),
            )
        }
        is ReaderAction.SetLineSpacing -> {
            val spacing = action.spacing.coerceIn(1.0f, 2.4f)
            val pages = paginateChapter(session.fullText, session.chapters, session.currentChapterIndex, session.fontSizeSp, spacing)
            session.copy(
                lineSpacing = spacing,
                pagesForCurrentChapter = pages,
                currentPage = session.currentPage.coerceInPageRange(pages),
            )
        }
        is ReaderAction.SetTheme -> session.copy(theme = action.theme)
        ReaderAction.NextPage -> session.copy(
            currentPage = (session.currentPage + 1).coerceInPageRange(session.pagesForCurrentChapter),
        )
        ReaderAction.PrevPage -> session.copy(
            currentPage = (session.currentPage - 1).coerceInPageRange(session.pagesForCurrentChapter),
        )
        ReaderAction.Retry -> session.copy(isLoading = true, errorMessage = null)
        is ReaderAction.Error -> session.copy(isLoading = false, errorMessage = action.message)
    }

    private fun paginateChapter(
        fullText: String,
        chapters: List<Chapter>,
        chapterIndex: Int,
        fontSizeSp: Int,
        lineSpacing: Float,
    ): List<Page> {
        if (fullText.isEmpty()) return emptyList()
        val chapter = chapters.getOrNull(chapterIndex)
        val chapterText = if (chapter == null) {
            fullText
        } else {
            fullText.substring(
                chapter.startOffset.coerceIn(0, fullText.length),
                chapter.endOffset.coerceIn(0, fullText.length),
            )
        }
        return TextPaginator.paginate(
            text = chapterText,
            maxWidthPx = PAGE_WIDTH_PX,
            maxHeightPx = PAGE_HEIGHT_PX,
            fontSizeSp = fontSizeSp.toFloat(),
            lineSpacing = lineSpacing,
            density = DENSITY,
        )
    }

    private fun Int.coerceInChapterRange(chapters: List<Chapter>): Int = when {
        chapters.isEmpty() -> 0
        else -> coerceIn(0, chapters.lastIndex)
    }

    private fun Int.coerceInPageRange(pages: List<Page>): Int = when {
        pages.isEmpty() -> 0
        else -> coerceIn(0, pages.lastIndex)
    }
}
