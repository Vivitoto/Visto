package app.visto.ui.reader

import android.graphics.Typeface
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

/**
 * Reader page margins stored as actual total dp values.
 *
 * The settings sheet presents these as extra margin above a baseline, so the
 * user-facing slider starts at 0 while persisted values remain compatible with
 * older releases.
 */
data class ReaderPageMargins(
    val topDp: Int = DEFAULT_TOP_DP,
    val bottomDp: Int = DEFAULT_BOTTOM_DP,
    val startDp: Int = DEFAULT_HORIZONTAL_DP,
    val endDp: Int = DEFAULT_HORIZONTAL_DP,
) {
    fun clamped(): ReaderPageMargins = ReaderPageMargins(
        topDp = topDp.coerceIn(TOP_BASELINE_DP, TOP_BASELINE_DP + EXTRA_MAX_DP),
        bottomDp = bottomDp.coerceIn(BOTTOM_BASELINE_DP, BOTTOM_BASELINE_DP + EXTRA_MAX_DP),
        startDp = startDp.coerceIn(HORIZONTAL_BASELINE_DP, HORIZONTAL_BASELINE_DP + EXTRA_MAX_DP),
        endDp = endDp.coerceIn(HORIZONTAL_BASELINE_DP, HORIZONTAL_BASELINE_DP + EXTRA_MAX_DP),
    )

    companion object {
        const val EXTRA_MIN_DP = 0
        const val EXTRA_MAX_DP = 96

        /** Baseline total padding used when the user-facing slider is 0. */
        const val TOP_BASELINE_DP = 12
        const val HORIZONTAL_BASELINE_DP = 16
        const val BOTTOM_BASELINE_DP = 52

        const val DEFAULT_TOP_DP = TOP_BASELINE_DP
        const val DEFAULT_BOTTOM_DP = BOTTOM_BASELINE_DP
        const val DEFAULT_HORIZONTAL_DP = HORIZONTAL_BASELINE_DP

        fun topExtraDp(totalDp: Int): Int = (totalDp - TOP_BASELINE_DP).coerceIn(EXTRA_MIN_DP, EXTRA_MAX_DP)
        fun bottomExtraDp(totalDp: Int): Int = (totalDp - BOTTOM_BASELINE_DP).coerceIn(EXTRA_MIN_DP, EXTRA_MAX_DP)
        fun horizontalExtraDp(totalDp: Int): Int =
            (totalDp - HORIZONTAL_BASELINE_DP).coerceIn(EXTRA_MIN_DP, EXTRA_MAX_DP)

        val DEFAULT = ReaderPageMargins()
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
    val pendingRestoreStartChar: Int? = null,
    val pagesForCurrentChapter: List<Page> = emptyList(),
    val fontSizeSp: Int = 18,
    val lineSpacing: Float = 1.5f,
    val fontChoice: ReaderFontChoice = ReaderFontChoice.DEFAULT,
    val viewport: ReaderViewport = ReaderViewport.DEFAULT,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val textColor: ReaderTextColor = ReaderTextColor.DEFAULT,
    val backgroundStyle: ReaderBackgroundStyle = ReaderBackgroundStyle.DEFAULT,
    val pageMargins: ReaderPageMargins = ReaderPageMargins.DEFAULT,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showToolbar: Boolean = true,
)

internal data class ReaderPagePresentation(
    val body: String,
)

internal object ReaderProgressEstimator {
    fun percent(
        currentChapterIndex: Int,
        currentPage: Int,
        currentChapterPageCount: Int,
        totalChapters: Int,
    ): Int {
        if (totalChapters <= 0) return 0
        val chapterIndex = currentChapterIndex.coerceIn(0, totalChapters - 1)
        val pageCount = currentChapterPageCount.coerceAtLeast(1)
        val page = currentPage.coerceIn(0, pageCount - 1)
        val chapterProgress = (page + 1).toFloat() / pageCount.toFloat()
        return (((chapterIndex + chapterProgress) / totalChapters.toFloat()) * 100f)
            .toInt()
            .coerceIn(0, 100)
    }
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
        val initialPageStartChar: Int? = null,
    ) : ReaderSessionAction()

    data class LoadError(val message: String) : ReaderSessionAction()
    data class SelectChapter(val index: Int, val landingPage: Int = 0) : ReaderSessionAction()
    data object PrevPage : ReaderSessionAction()
    data object NextPage : ReaderSessionAction()
    data class SetFontSize(val sp: Int) : ReaderSessionAction()
    data class SetLineSpacing(val value: Float) : ReaderSessionAction()
    data class SetFontChoice(val choice: ReaderFontChoice) : ReaderSessionAction()
    data class SetViewport(val viewport: ReaderViewport) : ReaderSessionAction()
    data class SetTheme(val theme: ReaderTheme) : ReaderSessionAction()
    data class SetTextColor(val textColor: ReaderTextColor) : ReaderSessionAction()
    data class SetBackgroundStyle(val backgroundStyle: ReaderBackgroundStyle) : ReaderSessionAction()
    data class SetPageMarginTop(val dp: Int) : ReaderSessionAction()
    data class SetPageMarginBottom(val dp: Int) : ReaderSessionAction()
    data class SetPageMarginStart(val dp: Int) : ReaderSessionAction()
    data class SetPageMarginEnd(val dp: Int) : ReaderSessionAction()
}

sealed class ReaderAction {
    data class Loaded(
        val encoding: String,
        val fullText: String,
        val chapters: List<Chapter>,
        val currentChapterIndex: Int = 0,
        val currentPage: Int = 0,
        val currentPageStartChar: Int? = null,
    ) : ReaderAction()

    data object ToggleToolbar : ReaderAction()
    data class GoToPage(val page: Int) : ReaderAction()
    data class GoToChapter(val index: Int, val landingPage: Int = 0) : ReaderAction()
    data class SetFontSize(val sp: Int) : ReaderAction()
    data class SetLineSpacing(val spacing: Float) : ReaderAction()
    data class SetFontChoice(val choice: ReaderFontChoice) : ReaderAction()
    data class SetViewport(val viewport: ReaderViewport) : ReaderAction()
    data class SetTheme(val theme: ReaderTheme) : ReaderAction()
    data class SetTextColor(val textColor: ReaderTextColor) : ReaderAction()
    data class SetBackgroundStyle(val backgroundStyle: ReaderBackgroundStyle) : ReaderAction()
    data class SetPageMarginTop(val dp: Int) : ReaderAction()
    data class SetPageMarginBottom(val dp: Int) : ReaderAction()
    data class SetPageMarginStart(val dp: Int) : ReaderAction()
    data class SetPageMarginEnd(val dp: Int) : ReaderAction()
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
                initialPageStartChar = action.currentPageStartChar,
            ),
        )
        ReaderAction.ToggleToolbar -> session.copy(showToolbar = !session.showToolbar)
        is ReaderAction.GoToPage -> session.copy(
            currentPage = action.page.coerceInPageRange(session.pagesForCurrentChapter),
            pendingRestoreStartChar = null,
        )
        is ReaderAction.GoToChapter -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SelectChapter(action.index, action.landingPage),
        )
        is ReaderAction.SetFontSize -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetFontSize(action.sp),
        )
        is ReaderAction.SetLineSpacing -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetLineSpacing(action.spacing),
        )
        is ReaderAction.SetFontChoice -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetFontChoice(action.choice),
        )
        is ReaderAction.SetViewport -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetViewport(action.viewport),
        )
        is ReaderAction.SetTheme -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetTheme(action.theme),
        )
        is ReaderAction.SetTextColor -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetTextColor(action.textColor),
        )
        is ReaderAction.SetBackgroundStyle -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetBackgroundStyle(action.backgroundStyle),
        )
        is ReaderAction.SetPageMarginTop -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetPageMarginTop(action.dp),
        )
        is ReaderAction.SetPageMarginBottom -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetPageMarginBottom(action.dp),
        )
        is ReaderAction.SetPageMarginStart -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetPageMarginStart(action.dp),
        )
        is ReaderAction.SetPageMarginEnd -> ReaderSessionReducer.reduce(
            session,
            ReaderSessionAction.SetPageMarginEnd(action.dp),
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
        pendingRestoreStartChar = null,
        pagesForCurrentChapter = emptyList(),
        fontSizeSp = DEFAULT_FONT_SIZE_SP,
        lineSpacing = DEFAULT_LINE_SPACING,
        fontChoice = ReaderFontChoice.DEFAULT,
        viewport = ReaderViewport.DEFAULT,
        theme = ReaderTheme.LIGHT,
        textColor = ReaderTextColor.DEFAULT,
        backgroundStyle = ReaderBackgroundStyle.DEFAULT,
        pageMargins = ReaderPageMargins.DEFAULT,
        isLoading = loading,
        errorMessage = null,
    )

    fun reduce(state: ReaderSession, action: ReaderSessionAction): ReaderSession = when (action) {
        is ReaderSessionAction.LoadResult -> {
            val chapters = action.chapters.ifEmpty { ChapterParser.parse(action.fullText) }
            val chapterIndex = action.initialPageStartChar
                .toChapterIndex(chapters, action.fullText.length)
                ?: action.initialChapterIndex.coerceInChapterBounds(chapters)
            val targetStartChar = action.initialPageStartChar.toChapterLocalStartChar(
                chapter = chapters.getOrNull(chapterIndex),
                fullTextLength = action.fullText.length,
            )
            repaginate(
                state.copy(
                    filePath = action.filePath,
                    fileName = action.fileName,
                    encoding = action.encoding,
                    fullText = action.fullText,
                    chapters = chapters,
                    currentChapterIndex = chapterIndex,
                    currentPage = action.initialPage.coerceAtLeast(0),
                    pendingRestoreStartChar = targetStartChar,
                    isLoading = false,
                    errorMessage = null,
                ),
                resetPage = false,
                targetStartChar = targetStartChar,
            )
        }
        is ReaderSessionAction.LoadError -> state.copy(isLoading = false, errorMessage = action.message)
        is ReaderSessionAction.SelectChapter -> {
            val index = action.index.coerceInChapterBounds(state.chapters)
            repaginate(
                state.copy(
                    currentChapterIndex = index,
                    currentPage = action.landingPage.coerceAtLeast(0),
                    pendingRestoreStartChar = null,
                ),
                resetPage = false,
            )
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
                    targetStartChar = state.currentPageAnchorStartChar(),
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
                    targetStartChar = state.currentPageAnchorStartChar(),
                )
            }
        }
        is ReaderSessionAction.SetFontChoice -> {
            if (action.choice == state.fontChoice) {
                state
            } else {
                repaginate(
                    state.copy(fontChoice = action.choice),
                    resetPage = false,
                    targetStartChar = state.currentPageAnchorStartChar(),
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
                    targetStartChar = state.currentPageAnchorStartChar(),
                )
            }
        }
        is ReaderSessionAction.SetTheme -> state.copy(theme = action.theme)
        is ReaderSessionAction.SetTextColor -> state.copy(textColor = action.textColor)
        is ReaderSessionAction.SetBackgroundStyle -> state.copy(backgroundStyle = action.backgroundStyle)
        is ReaderSessionAction.SetPageMarginTop -> setPageMargins(
            state,
            state.pageMargins.copy(topDp = action.dp),
        )
        is ReaderSessionAction.SetPageMarginBottom -> setPageMargins(
            state,
            state.pageMargins.copy(bottomDp = action.dp),
        )
        is ReaderSessionAction.SetPageMarginStart -> setPageMargins(
            state,
            state.pageMargins.copy(startDp = action.dp),
        )
        is ReaderSessionAction.SetPageMarginEnd -> setPageMargins(
            state,
            state.pageMargins.copy(endDp = action.dp),
        )
    }

    private fun previousPage(state: ReaderSession): ReaderSession {
        if (state.currentPage > 0) {
            return state.copy(currentPage = state.currentPage - 1, pendingRestoreStartChar = null)
        }
        if (state.currentChapterIndex <= 0) {
            return state.copy(currentPage = 0, pendingRestoreStartChar = null)
        }

        val previousChapterState = repaginate(
            state.copy(
                currentChapterIndex = state.currentChapterIndex - 1,
                currentPage = 0,
                pendingRestoreStartChar = null,
            ),
            resetPage = true,
        )
        return previousChapterState.copy(
            currentPage = previousChapterState.pagesForCurrentChapter.lastIndex.coerceAtLeast(0),
            pendingRestoreStartChar = null,
        )
    }

    private fun nextPage(state: ReaderSession): ReaderSession {
        val lastPage = state.pagesForCurrentChapter.lastIndex
        if (state.currentPage < lastPage) {
            return state.copy(currentPage = state.currentPage + 1, pendingRestoreStartChar = null)
        }
        if (state.currentChapterIndex >= state.chapters.lastIndex) {
            return state.copy(currentPage = lastPage.coerceAtLeast(0), pendingRestoreStartChar = null)
        }

        return repaginate(
            state.copy(
                currentChapterIndex = state.currentChapterIndex + 1,
                currentPage = 0,
                pendingRestoreStartChar = null,
            ),
            resetPage = true,
        )
    }

    private fun setPageMargins(state: ReaderSession, margins: ReaderPageMargins): ReaderSession {
        val clamped = margins.clamped()
        if (clamped == state.pageMargins) {
            return state
        }
        return repaginate(
            state.copy(pageMargins = clamped),
            resetPage = false,
            targetStartChar = state.currentPageAnchorStartChar(),
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
            typeface = state.fontChoice.paginationTypeface(),
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

    private fun Int?.toChapterIndex(chapters: List<Chapter>, fullTextLength: Int): Int? {
        if (this == null || chapters.isEmpty()) return null
        val absoluteStart = coerceIn(0, fullTextLength)
        return chapters.indexOfLast { chapter ->
            val start = chapter.startOffset.coerceIn(0, fullTextLength)
            val end = chapter.endOffset.coerceIn(start, fullTextLength)
            absoluteStart in start..end
        }.takeIf { it >= 0 }
    }

    private fun ReaderSession.currentPageStartChar(): Int? =
        pagesForCurrentChapter
            .getOrNull(currentPage.coerceIn(0, pagesForCurrentChapter.lastIndex.coerceAtLeast(0)))
            ?.startChar

    private fun ReaderSession.currentPageAnchorStartChar(): Int? =
        pendingRestoreStartChar ?: currentPageStartChar()

    private fun List<Page>.indexAtStartOffset(offset: Int): Int =
        indexOfLast { it.startChar <= offset }.takeIf { it >= 0 } ?: 0

    private fun Int?.toChapterLocalStartChar(chapter: Chapter?, fullTextLength: Int): Int? {
        if (this == null || chapter == null) return null
        val start = chapter.startOffset.coerceIn(0, fullTextLength)
        val end = chapter.endOffset.coerceIn(start, fullTextLength)
        if (this < start || this > end) return null
        return (this - start).coerceIn(0, end - start)
    }

    private fun ReaderViewport.sanitized(): ReaderViewport = ReaderViewport(
        widthPx = widthPx.coerceAtLeast(1),
        heightPx = heightPx.coerceAtLeast(1),
        density = density.takeIf { it > 0f } ?: ReaderViewport.DEFAULT.density,
    )

}

internal fun ReaderSession.currentAbsolutePageStartChar(): Int? {
    val chapter = chapters.getOrNull(currentChapterIndex) ?: return null
    val start = chapter.startOffset.coerceIn(0, fullText.length)
    val end = chapter.endOffset.coerceIn(start, fullText.length)
    val localStart = pendingRestoreStartChar
        ?: pagesForCurrentChapter
            .getOrNull(currentPage.coerceIn(0, pagesForCurrentChapter.lastIndex.coerceAtLeast(0)))
            ?.startChar
        ?: return null
    return start + localStart.coerceIn(0, end - start)
}

internal fun ReaderFontChoice.paginationTypeface(): Typeface? = when (this) {
    ReaderFontChoice.SystemDefault -> null
    ReaderFontChoice.Sans -> Typeface.SANS_SERIF
    ReaderFontChoice.Serif -> Typeface.SERIF
    is ReaderFontChoice.Custom -> null
}
