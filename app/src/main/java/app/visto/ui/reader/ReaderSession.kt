package app.visto.ui.reader

import app.visto.core.book.Chapter
import app.visto.core.book.ChapterParser
import app.visto.core.book.Page
import app.visto.core.book.TextPaginator

/** Immutable state for the plain-text reader screen. */
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

sealed class ReaderSessionAction {
    data class LoadResult(
        val filePath: String,
        val fileName: String,
        val encoding: String,
        val fullText: String,
        val chapters: List<Chapter> = ChapterParser.parse(fullText),
        val initialChapterIndex: Int = 0,
        val initialPage: Int = 0,
    ) : ReaderSessionAction()

    data class LoadError(val message: String) : ReaderSessionAction()
    data class SelectChapter(val index: Int) : ReaderSessionAction()
    data object PrevPage : ReaderSessionAction()
    data object NextPage : ReaderSessionAction()
    data class SetFontSize(val sp: Int) : ReaderSessionAction()
    data class SetLineSpacing(val value: Float) : ReaderSessionAction()
    data class SetTheme(val theme: ReaderTheme) : ReaderSessionAction()
}

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
    fun reduce(session: ReaderSession, action: ReaderAction): ReaderSession = when (action) {
        is ReaderAction.Loaded -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.LoadResult(
                filePath = session.filePath,
                fileName = session.fileName,
                encoding = action.encoding,
                fullText = action.fullText,
                chapters = action.chapters,
                initialChapterIndex = action.currentChapterIndex,
                initialPage = action.currentPage,
            ),
        )
        ReaderAction.ToggleToolbar -> session.copy(showToolbar = !session.showToolbar)
        is ReaderAction.GoToPage -> session.copy(
            currentPage = action.page.coerceInPageRange(session.pagesForCurrentChapter),
        )
        is ReaderAction.GoToChapter -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SelectChapter(action.index),
        )
        is ReaderAction.SetFontSize -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetFontSize(action.sp),
        )
        is ReaderAction.SetLineSpacing -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetLineSpacing(action.spacing),
        )
        is ReaderAction.SetTheme -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetTheme(action.theme),
        )
        ReaderAction.NextPage -> ReaderSessionReducer.reduce(session, ReaderSessionAction.NextPage)
        ReaderAction.PrevPage -> ReaderSessionReducer.reduce(session, ReaderSessionAction.PrevPage)
        ReaderAction.Retry -> session.copy(isLoading = true, errorMessage = null)
        is ReaderAction.Error -> session.copy(isLoading = false, errorMessage = action.message)
    }

    private fun Int.coerceInPageRange(pages: List<Page>): Int = when {
        pages.isEmpty() -> 0
        else -> coerceIn(0, pages.lastIndex)
    }
}

object ReaderSessionReducer {
    private const val DEFAULT_FONT_SIZE_SP = 18
    private const val DEFAULT_LINE_SPACING = 1.5f
    private const val PAGE_WIDTH_PX = 360f
    private const val PAGE_HEIGHT_PX = 640f
    private const val DENSITY = 1f

    fun initial(loading: Boolean): ReaderSession = ReaderSession(
        filePath = "",
        fileName = "",
        encoding = "UTF-8",
        fullText = "",
        chapters = emptyList(),
        currentChapterIndex = 0,
        currentPage = 0,
        pagesForCurrentChapter = emptyList(),
        fontSizeSp = DEFAULT_FONT_SIZE_SP,
        lineSpacing = DEFAULT_LINE_SPACING,
        theme = ReaderTheme.LIGHT,
        isLoading = loading,
        errorMessage = null,
    )

    fun reduce(state: ReaderSession, action: ReaderSessionAction): ReaderSession = when (action) {
        is ReaderSessionAction.LoadResult -> {
            val chapters = action.chapters.ifEmpty { ChapterParser.parse(action.fullText) }
            repaginate(
                state.copy(
                    filePath = action.filePath,
                    fileName = action.fileName,
                    encoding = action.encoding,
                    fullText = action.fullText,
                    chapters = chapters,
                    currentChapterIndex = action.initialChapterIndex.coerceInChapterBounds(chapters),
                    currentPage = action.initialPage.coerceAtLeast(0),
                    isLoading = false,
                    errorMessage = null,
                ),
                resetPage = false,
            )
        }
        is ReaderSessionAction.LoadError -> state.copy(isLoading = false, errorMessage = action.message)
        is ReaderSessionAction.SelectChapter -> {
            val index = action.index.coerceInChapterBounds(state.chapters)
            repaginate(state.copy(currentChapterIndex = index, currentPage = 0), resetPage = true)
        }
        ReaderSessionAction.PrevPage -> previousPage(state)
        ReaderSessionAction.NextPage -> nextPage(state)
        is ReaderSessionAction.SetFontSize -> repaginate(
            state.copy(fontSizeSp = action.sp.coerceIn(14, 28), currentPage = 0),
            resetPage = true,
        )
        is ReaderSessionAction.SetLineSpacing -> repaginate(
            state.copy(lineSpacing = action.value.coerceIn(1.0f, 2.4f), currentPage = 0),
            resetPage = true,
        )
        is ReaderSessionAction.SetTheme -> state.copy(theme = action.theme)
    }

    private fun previousPage(state: ReaderSession): ReaderSession {
        if (state.currentPage > 0) return state.copy(currentPage = state.currentPage - 1)
        if (state.currentChapterIndex <= 0) return state.copy(currentPage = 0)

        val previousChapterState = repaginate(
            state.copy(currentChapterIndex = state.currentChapterIndex - 1, currentPage = 0),
            resetPage = true,
        )
        return previousChapterState.copy(
            currentPage = previousChapterState.pagesForCurrentChapter.lastIndex.coerceAtLeast(0),
        )
    }

    private fun nextPage(state: ReaderSession): ReaderSession {
        val lastPage = state.pagesForCurrentChapter.lastIndex
        if (state.currentPage < lastPage) return state.copy(currentPage = state.currentPage + 1)
        if (state.currentChapterIndex >= state.chapters.lastIndex) return state.copy(currentPage = lastPage.coerceAtLeast(0))

        return repaginate(
            state.copy(currentChapterIndex = state.currentChapterIndex + 1, currentPage = 0),
            resetPage = true,
        )
    }

    private fun repaginate(state: ReaderSession, resetPage: Boolean): ReaderSession {
        if (state.chapters.isEmpty()) return state.copy(pagesForCurrentChapter = emptyList(), currentPage = 0)

        val chapterIndex = state.currentChapterIndex.coerceInChapterBounds(state.chapters)
        val chapter = state.chapters[chapterIndex]
        val start = chapter.startOffset.coerceIn(0, state.fullText.length)
        val end = chapter.endOffset.coerceIn(start, state.fullText.length)
        val chapterText = state.fullText.substring(start, end)
        val pages = TextPaginator.paginate(
            text = chapterText,
            maxWidthPx = PAGE_WIDTH_PX,
            maxHeightPx = PAGE_HEIGHT_PX,
            fontSizeSp = state.fontSizeSp.toFloat(),
            lineSpacing = state.lineSpacing,
            density = DENSITY,
        )
        val requestedPage = if (resetPage) 0 else state.currentPage
        return state.copy(
            currentChapterIndex = chapterIndex,
            currentPage = requestedPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0)),
            pagesForCurrentChapter = pages,
        )
    }

    private fun Int.coerceInChapterBounds(chapters: List<Chapter>): Int =
        if (chapters.isEmpty()) 0 else coerceIn(0, chapters.lastIndex)
}
