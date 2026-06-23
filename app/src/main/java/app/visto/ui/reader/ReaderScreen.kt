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

@Composable
fun ReaderScreen(
    session: ReaderSession,
    onBack: () -> Unit,
    onChapterSelect: (Int) -> Unit,
    onSettingsToggle: () -> Unit,
    onSaveProgress: (ReaderSession) -> Unit,
    onViewportChange: (ReaderViewport) -> Unit = {},
) {
    val palette = session.readerPalette()
    val readerFontFamily = rememberReaderFontFamily(session.fontChoice)
    var chromeVisible by remember { mutableStateOf(true) }
    var currentPage by remember(session.filePath, session.currentChapterIndex, session.currentPage) {
        mutableStateOf(session.currentPage)
    }
    var showChapterList by remember { mutableStateOf(false) }

    val pages = session.pagesForCurrentChapter
    val safePage = currentPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
    val currentChapter = session.chapters.getOrNull(session.currentChapterIndex)

    LaunchedEffect(pages.size) {
        currentPage = currentPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
    }

    fun saveProgress(page: Int = safePage) {
        if (session.isLoading || session.errorMessage != null || pages.isEmpty()) return
        onSaveProgress(session.copy(currentPage = page.coerceIn(0, pages.lastIndex.coerceAtLeast(0))))
    }

    fun goToPreviousPage() {
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
                .background(palette.backgroundColor),
        ) {
            val density = LocalDensity.current
            val horizontalPadding = if (maxWidth < 360.dp) 18.dp else 22.dp
            val hiddenVerticalPadding = if (maxHeight < 600.dp) 24.dp else 28.dp
            val topContentPadding = if (chromeVisible) 64.dp else hiddenVerticalPadding
            val bottomContentPadding = if (chromeVisible) 104.dp else hiddenVerticalPadding
            val viewport = remember(
                maxWidth,
                maxHeight,
                horizontalPadding,
                topContentPadding,
                bottomContentPadding,
                density,
            ) {
                with(density) {
                    ReaderViewport(
                        widthPx = (maxWidth - horizontalPadding * 2)
                            .coerceAtLeast(1.dp)
                            .toPx()
                            .roundToInt(),
                        heightPx = (maxHeight - topContentPadding - bottomContentPadding)
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
                session.isLoading -> ReaderLoading(palette)
                session.errorMessage != null -> ReaderError(palette, session.errorMessage, onBack = ::closeReader)
                pages.isEmpty() -> ReaderEmpty(palette, onBack = ::closeReader)
                else -> {
                    val swipeThresholdPx = with(density) { 48.dp.toPx() }
                    val page = pages.getOrNull(safePage)
                    val pagePresentation = remember(page, currentChapter) {
                        ReaderPagePresenter.present(page, currentChapter)
                    }
                    val progressPercent = ReaderProgressEstimator.percent(
                        currentChapterIndex = session.currentChapterIndex,
                        currentPage = safePage,
                        currentChapterPageCount = pages.size,
                        totalChapters = session.chapters.size,
                    )
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
                            palette = palette,
                            fontSizeSp = session.fontSizeSp,
                            lineSpacing = session.lineSpacing,
                            fontFamily = readerFontFamily,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = horizontalPadding,
                                    top = topContentPadding,
                                    end = horizontalPadding,
                                    bottom = bottomContentPadding,
                                ),
                        )
                        ReaderPageFooter(
                            chapterTitle = currentChapter?.title ?: Strings.READER_CURRENT_CHAPTER,
                            progressPercent = progressPercent,
                            palette = palette,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(
                                    start = horizontalPadding,
                                    end = horizontalPadding,
                                    bottom = if (chromeVisible) 92.dp else 14.dp,
                                ),
                        )
                    }
                }
            }

            if (chromeVisible && !session.isLoading && session.errorMessage == null && pages.isNotEmpty()) {
                ReaderTopBar(
                    title = currentChapter?.title ?: session.fileName,
                    palette = palette,
                    onToggle = { chromeVisible = false },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 12.dp, top = 10.dp, end = 12.dp),
                )
                ReaderBottomBar(
                    palette = palette,
                    onChapterList = { showChapterList = true },
                    onSettings = onSettingsToggle,
                    onBack = ::closeReader,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
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

@Composable
private fun ReaderPageText(
    presentation: ReaderPagePresentation,
    palette: ReaderPalette,
    fontSizeSp: Int,
    lineSpacing: Float,
    fontFamily: FontFamily?,
    modifier: Modifier = Modifier,
) {
    if (!presentation.hasStyledTitle) {
        Text(
            text = presentation.body,
            color = palette.textColor,
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
            color = palette.textColor,
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
                color = palette.textColor,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * lineSpacing).sp,
                fontFamily = fontFamily,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ReaderLoading(palette: ReaderPalette) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = palette.textColor)
    }
}

@Composable
private fun ReaderError(palette: ReaderPalette, message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundColor)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = Strings.READER_LOAD_FAILED, color = palette.textColor, style = MaterialTheme.typography.titleLarge)
        Text(
            text = message,
            color = palette.textColor.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onBack, modifier = Modifier.padding(top = 20.dp)) {
            Text(Strings.BACK)
        }
    }
}

@Composable
private fun ReaderEmpty(palette: ReaderPalette, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundColor)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = Strings.READER_EMPTY, color = palette.textColor, style = MaterialTheme.typography.titleLarge)
        Button(onClick = onBack, modifier = Modifier.padding(top = 20.dp)) {
            Text(Strings.BACK)
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    palette: ReaderPalette,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = palette.toolbarColor,
        contentColor = palette.textColor,
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ReaderPageFooter(
    chapterTitle: String,
    progressPercent: Int,
    palette: ReaderPalette,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = chapterTitle,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textColor.copy(alpha = 0.46f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$progressPercent%",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textColor.copy(alpha = 0.46f),
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun ReaderBottomBar(
    palette: ReaderPalette,
    onChapterList: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            color = palette.toolbarColor,
            contentColor = palette.textColor,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onChapterList, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.List, contentDescription = Strings.READER_CHAPTERS)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = Strings.READER_CHAPTERS,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = Strings.SETTINGS_TITLE)
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = Strings.BACK)
                }
            }
        }
    }
}
