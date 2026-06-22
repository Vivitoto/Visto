package app.visto.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.visto.ui.Strings
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun ReaderScreen(
    session: ReaderSession,
    onBack: () -> Unit,
    onChapterSelect: (Int) -> Unit,
    onSettingsToggle: () -> Unit,
    onSaveProgress: (ReaderSession) -> Unit,
    onViewportChange: (ReaderViewport) -> Unit = {},
) {
    val theme = session.theme
    val readerFontFamily = rememberReaderFontFamily(session.fontChoice)
    var chromeVisible by remember { mutableStateOf(true) }
    var currentPage by remember(session.filePath, session.currentChapterIndex, session.currentPage) {
        mutableStateOf(session.currentPage)
    }
    var showChapterList by remember { mutableStateOf(false) }
    var turnFeedback by remember { mutableStateOf<PageTurnFeedback?>(null) }
    var nextFeedbackId by remember { mutableStateOf(0) }

    val pages = session.pagesForCurrentChapter
    val safePage = currentPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
    val currentChapter = session.chapters.getOrNull(session.currentChapterIndex)

    LaunchedEffect(pages.size) {
        currentPage = currentPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
    }

    LaunchedEffect(turnFeedback) {
        val feedback = turnFeedback ?: return@LaunchedEffect
        delay(520)
        if (turnFeedback == feedback) {
            turnFeedback = null
        }
    }

    fun showTurnFeedback(direction: PageTurnDirection) {
        nextFeedbackId += 1
        turnFeedback = PageTurnFeedback(direction = direction, id = nextFeedbackId)
    }

    fun saveProgress(page: Int = safePage) {
        if (session.isLoading || session.errorMessage != null || pages.isEmpty()) return
        onSaveProgress(session.copy(currentPage = page.coerceIn(0, pages.lastIndex.coerceAtLeast(0))))
    }

    fun goToPreviousPage() {
        showTurnFeedback(PageTurnDirection.Previous)
        val page = currentPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
        if (page > 0) {
            val target = page - 1
            currentPage = target
            saveProgress(target)
        } else if (session.currentChapterIndex > 0) {
            onChapterSelect(session.currentChapterIndex - 1)
        }
    }

    fun goToNextPage() {
        showTurnFeedback(PageTurnDirection.Next)
        val page = currentPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
        if (page < pages.lastIndex) {
            val target = page + 1
            currentPage = target
            saveProgress(target)
        } else if (session.currentChapterIndex < session.chapters.lastIndex) {
            onChapterSelect(session.currentChapterIndex + 1)
        }
    }

    fun closeReader() {
        saveProgress()
        onBack()
    }

    BackHandler(onBack = ::closeReader)

    Scaffold { innerPadding: PaddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(theme.backgroundColor),
        ) {
            val density = LocalDensity.current
            val horizontalPadding = 22.dp
            val verticalPadding = if (chromeVisible) 88.dp else 28.dp
            val viewport = remember(maxWidth, maxHeight, horizontalPadding, verticalPadding, density) {
                with(density) {
                    ReaderViewport(
                        widthPx = (maxWidth - horizontalPadding * 2)
                            .coerceAtLeast(1.dp)
                            .toPx()
                            .roundToInt(),
                        heightPx = (maxHeight - verticalPadding * 2)
                            .coerceAtLeast(1.dp)
                            .toPx()
                            .roundToInt(),
                        density = density.density * density.fontScale,
                    )
                }
            }

            LaunchedEffect(viewport, session.viewport) {
                if (viewport != session.viewport) {
                    onViewportChange(viewport)
                }
            }

            when {
                session.isLoading -> ReaderLoading(theme)
                session.errorMessage != null -> ReaderError(theme, session.errorMessage, onBack = closeReader)
                pages.isEmpty() -> ReaderEmpty(theme, onBack = closeReader)
                else -> {
                    val swipeThresholdPx = with(density) { 48.dp.toPx() }
                    val page = pages.getOrNull(safePage)
                    val pagePresentation = remember(page, currentChapter) {
                        ReaderPagePresenter.present(page, currentChapter)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(session.currentChapterIndex, pages.size, currentPage, swipeThresholdPx) {
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                                    onDragEnd = {
                                        when {
                                            totalDrag <= -swipeThresholdPx -> goToNextPage()
                                            totalDrag >= swipeThresholdPx -> goToPreviousPage()
                                        }
                                    },
                                    onDragCancel = { totalDrag = 0f },
                                )
                            }
                            .pointerInput(session.currentChapterIndex, pages.size, currentPage) {
                                detectTapGestures { offset ->
                                    val leftEdge = size.width / 3f
                                    val rightEdge = size.width * 2f / 3f
                                    when {
                                        offset.x < leftEdge -> goToPreviousPage()
                                        offset.x > rightEdge -> goToNextPage()
                                        else -> chromeVisible = !chromeVisible
                                    }
                                }
                            },
                    ) {
                        ReaderPageText(
                            presentation = pagePresentation,
                            theme = theme,
                            fontSizeSp = session.fontSizeSp,
                            lineSpacing = session.lineSpacing,
                            fontFamily = readerFontFamily,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                        )

                        turnFeedback?.let { feedback ->
                            ReaderTurnFeedback(
                                direction = feedback.direction,
                                theme = theme,
                                modifier = Modifier
                                    .align(
                                        if (feedback.direction == PageTurnDirection.Previous) {
                                            Alignment.CenterStart
                                        } else {
                                            Alignment.CenterEnd
                                        }
                                    )
                                    .padding(horizontal = 22.dp),
                            )
                        }
                    }
                }
            }

            if (chromeVisible && !session.isLoading && session.errorMessage == null && pages.isNotEmpty()) {
                ReaderTopBar(
                    title = currentChapter?.title ?: session.fileName,
                    theme = theme,
                    onToggle = { chromeVisible = false },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 12.dp, top = 12.dp, end = 12.dp),
                )
                ReaderBottomBar(
                    chapterTitle = currentChapter?.title ?: Strings.READER_CURRENT_CHAPTER,
                    page = safePage + 1,
                    totalPages = pages.size,
                    theme = theme,
                    onChapterList = { showChapterList = true },
                    onSettings = onSettingsToggle,
                    onBack = closeReader,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                )
            }
        }
    }

    if (showChapterList) {
        ChapterListSheet(
            chapters = session.chapters,
            currentIndex = session.currentChapterIndex,
            onSelect = onChapterSelect,
            onDismiss = { showChapterList = false },
        )
    }
}

private enum class PageTurnDirection {
    Previous,
    Next,
}

private data class PageTurnFeedback(
    val direction: PageTurnDirection,
    val id: Int,
)

@Composable
private fun ReaderPageText(
    presentation: ReaderPagePresentation,
    theme: ReaderTheme,
    fontSizeSp: Int,
    lineSpacing: Float,
    fontFamily: FontFamily?,
    modifier: Modifier = Modifier,
) {
    if (!presentation.hasStyledTitle) {
        Text(
            text = presentation.body,
            color = theme.textColor,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * lineSpacing).sp,
            fontFamily = fontFamily,
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier) {
        Text(
            text = presentation.title.orEmpty(),
            color = theme.textColor,
            fontSize = (fontSizeSp + 5).sp,
            fontWeight = FontWeight.Bold,
            lineHeight = (fontSizeSp * 1.35f).sp,
            fontFamily = fontFamily,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.size(18.dp))
        if (presentation.body.isNotEmpty()) {
            Text(
                text = presentation.body,
                color = theme.textColor,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * lineSpacing).sp,
                fontFamily = fontFamily,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ReaderTurnFeedback(
    direction: PageTurnDirection,
    theme: ReaderTheme,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = theme.toolbarColor.copy(alpha = 0.86f),
        contentColor = theme.textColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier,
    ) {
        Text(
            text = when (direction) {
                PageTurnDirection.Previous -> Strings.READER_PREVIOUS_PAGE
                PageTurnDirection.Next -> Strings.READER_NEXT_PAGE
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ReaderLoading(theme: ReaderTheme) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = theme.textColor)
    }
}

@Composable
private fun ReaderError(theme: ReaderTheme, message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = Strings.READER_LOAD_FAILED, color = theme.textColor, style = MaterialTheme.typography.titleLarge)
        Text(
            text = message,
            color = theme.textColor.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onBack, modifier = Modifier.padding(top = 20.dp)) {
            Text(Strings.BACK)
        }
    }
}

@Composable
private fun ReaderEmpty(theme: ReaderTheme, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = Strings.READER_EMPTY, color = theme.textColor, style = MaterialTheme.typography.titleLarge)
        Button(onClick = onBack, modifier = Modifier.padding(top = 20.dp)) {
            Text(Strings.BACK)
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    theme: ReaderTheme,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = theme.toolbarColor,
        contentColor = theme.textColor,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ReaderBottomBar(
    chapterTitle: String,
    page: Int,
    totalPages: Int,
    theme: ReaderTheme,
    onChapterList: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            color = theme.toolbarColor,
            contentColor = theme.textColor,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = Strings.readerPageStatus(chapterTitle, page, totalPages),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
        Surface(
            color = theme.toolbarColor,
            contentColor = theme.textColor,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onChapterList, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.List, contentDescription = Strings.READER_CHAPTERS)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = Strings.READER_CHAPTERS, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = Strings.SETTINGS_TITLE)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = Strings.BACK)
                }
            }
        }
    }
}
