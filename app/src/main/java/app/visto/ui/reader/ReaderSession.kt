package app.visto.ui.reader

import app.visto.core.book.Chapter
import app.visto.core.book.ChapterParser
import app.visto.core.book.Page
import app.visto.core.book.TextPaginator
import kotlin.math.abs

/** Pixel dimensions used for reader pagination. */
data class ReaderViewport(
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
) {
    companion object {
        val DEFAULT = ReaderViewport(widthPx = 360, heightPx = 640, density = 1f)
    }
}

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
    val viewport: ReaderViewport = ReaderViewport.DEFAULT,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showToolbar: Boolean = true,
)

internal data class ReaderPagePresentation(
    val title: String?,
    val body: String,
) {
    val hasStyledTitle: Boolean = title != null
}

internal object ReaderPagePresenter {
    fun present(page: Page?, chapter: Chapter?): ReaderPagePresentation {
        val text = page?.text.orEmpty()
        if (page == null || chapter == null || page.startChar != 0 || text.isEmpty()) {
            return ReaderPagePresentation(title = null, body = text)
        }

        val firstLineEnd = text.indexOf('\n').takeIf { it >= 0 } ?: text.length
        val firstLine = text.substring(0, firstLineEnd).trimReaderHeadingWhitespace()
        val chapterTitle = chapter.title.trimReaderHeadingWhitespace()
        if (firstLine != chapterTitle) {
            return ReaderPagePresentation(title = null, body = text)
        }

        val bodyStart = if (firstLineEnd < text.length && text[firstLineEnd] == '\n') {
            firstLineEnd + 1
        } else {
            firstLineEnd
        }
        return ReaderPagePresentation(
            title = chapter.title,
            body = text.substring(bodyStart),
        )
    }

    private fun String.trimReaderHeadingWhitespace(): String =
        trim { it == ' ' || it == '\t' || it == '\u3000' }
}

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
    data class SetViewport(val viewport: ReaderViewport) : ReaderSessionAction()
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
    data class SetViewport(val viewport: ReaderViewport) : ReaderAction()
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
        is ReaderAction.SetViewport -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetViewport(action.viewport),
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
        viewport = ReaderViewport.DEFAULT,
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
        is ReaderSessionAction.SetFontSize -> {
            val fontSize = action.sp.coerceIn(14, 28)
            if (fontSize == state.fontSizeSp) {
                state
            } else {
                repaginate(
                    state.copy(fontSizeSp = fontSize),
                    resetPage = false,
                    targetStartChar = state.currentPageStartChar(),
                )
            }
        }
        is ReaderSessionAction.SetLineSpacing -> {
            val lineSpacing = action.value.coerceIn(1.0f, 2.4f)
            if (abs(lineSpacing - state.lineSpacing) < 0.001f) {
                state
            } else {
                repaginate(
                    state.copy(lineSpacing = lineSpacing),
                    resetPage = false,
                    targetStartChar = state.currentPageStartChar(),
                )
            }
        }
        is ReaderSessionAction.SetViewport -> {
            val viewport = action.viewport.sanitized()
            if (viewport == state.viewport) {
                state
            } else {
                repaginate(
                    state.copy(viewport = viewport),
                    resetPage = false,
                    targetStartChar = state.currentPageStartChar(),
                )
            }
        }
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

    private fun repaginate(
        state: ReaderSession,
        resetPage: Boolean,
        targetStartChar: Int? = null,
    ): ReaderSession {
        if (state.chapters.isEmpty()) {
            return state.copy(
                pagesForCurrentChapter = emptyList(),
                currentPage = if (resetPage) 0 else state.currentPage.coerceAtLeast(0),
            )
        }

        val chapterIndex = state.currentChapterIndex.coerceInChapterBounds(state.chapters)
        val chapter = state.chapters[chapterIndex]
        val start = chapter.startOffset.coerceIn(0, state.fullText.length)
        val end = chapter.endOffset.coerceIn(start, state.fullText.length)
        val chapterText = state.fullText.substring(start, end)
        val pages = TextPaginator.paginate(
            text = chapterText,
            maxWidthPx = state.viewport.widthPx.toFloat(),
            maxHeightPx = state.viewport.heightPx.toFloat(),
            fontSizeSp = state.fontSizeSp.toFloat(),
            lineSpacing = state.lineSpacing,
            density = state.viewport.density,
        )
        val requestedPage = when {
            resetPage -> 0
            targetStartChar != null -> pages.indexAtStartOffset(targetStartChar)
            else -> state.currentPage
        }
        return state.copy(
            currentChapterIndex = chapterIndex,
            currentPage = requestedPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0)),
            pagesForCurrentChapter = pages,
        )
    }

    private fun Int.coerceInChapterBounds(chapters: List<Chapter>): Int =
        if (chapters.isEmpty()) 0 else coerceIn(0, chapters.lastIndex)

    private fun ReaderSession.currentPageStartChar(): Int? =
        pagesForCurrentChapter
            .getOrNull(currentPage.coerceIn(0, pagesForCurrentChapter.lastIndex.coerceAtLeast(0)))
            ?.startChar

    private fun List<Page>.indexAtStartOffset(offset: Int): Int =
        indexOfLast { it.startChar <= offset }.takeIf { it >= 0 } ?: 0

    private fun ReaderViewport.sanitized(): ReaderViewport = ReaderViewport(
        widthPx = widthPx.coerceAtLeast(1),
        heightPx = heightPx.coerceAtLeast(1),
        density = density.takeIf { it > 0f } ?: ReaderViewport.DEFAULT.density,
    )
}
