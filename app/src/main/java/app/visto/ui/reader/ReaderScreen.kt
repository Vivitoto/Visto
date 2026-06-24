package app.visto.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.visto.ui.Strings
import app.visto.ui.book.bookDisplayTitle
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    session: ReaderSession,
    onBack: () -> Unit,
    onChapterSelect: (Int, Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
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
        if (pages.isEmpty()) return
        val page = currentPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
        if (page > 0) {
            currentPage = page - 1
        }
        onPreviousPage()
    }

    fun goToNextPage() {
        if (pages.isEmpty()) return
        val page = currentPage.coerceIn(0, pages.lastIndex.coerceAtLeast(0))
        if (page < pages.lastIndex) {
            currentPage = page + 1
        }
        onNextPage()
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
            var measuredFooterHeight by remember { mutableStateOf(ReaderLayoutMetrics.FooterHeightReserve) }
            val layoutPadding = remember(maxWidth, maxHeight, measuredFooterHeight, session.pageMargins) {
                ReaderLayoutMetrics.contentPadding(
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                    measuredFooterHeight = measuredFooterHeight,
                    pageMargins = session.pageMargins,
                )
            }
            val viewport = remember(
                maxWidth,
                maxHeight,
                layoutPadding,
                density,
            ) {
                ReaderLayoutMetrics.viewport(
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                    padding = layoutPadding,
                    density = density,
                )
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
                    val pagePresentation = remember(page) {
                        ReaderPagePresentation(body = page?.text.orEmpty())
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
                                    start = layoutPadding.startContentPadding,
                                    top = layoutPadding.topContentPadding,
                                    end = layoutPadding.endContentPadding,
                                    bottom = layoutPadding.bottomContentPadding,
                                ),
                        )
                        ReaderPageFooter(
                            chapterTitle = currentChapter?.title ?: Strings.READER_CURRENT_CHAPTER,
                            progressPercent = progressPercent,
                            palette = palette,
                            onMeasuredHeight = { measuredFooterHeight = it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(
                                    start = layoutPadding.startContentPadding,
                                    end = layoutPadding.endContentPadding,
                                    bottom = layoutPadding.footerBottomPadding(chromeVisible),
                                ),
                        )
                    }
                }
            }

            if (chromeVisible && !session.isLoading && session.errorMessage == null && pages.isNotEmpty()) {
                ReaderTopBar(
                    title = bookDisplayTitle(session.fileName),
                    palette = palette,
                    onToggle = { chromeVisible = false },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 12.dp, top = ReaderLayoutMetrics.TopBarTopPadding, end = 12.dp),
                )
                ReaderBottomBar(
                    palette = palette,
                    onChapterList = { showChapterList = true },
                    onSettings = onSettingsToggle,
                    onBack = ::closeReader,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                            bottom = layoutPadding.bottomBarBottomPadding,
                        ),
                )
            }
        }
    }

    if (showChapterList) {
        ChapterListSheet(
            chapters = session.chapters,
            currentIndex = session.currentChapterIndex,
            onSelect = { onChapterSelect(it, 0) },
            onDismiss = { showChapterList = false },
        )
    }
}

internal data class ReaderContentPadding(
    val startContentPadding: Dp,
    val endContentPadding: Dp,
    val topContentPadding: Dp,
    val bottomContentPadding: Dp,
    val footerBottomPadding: Dp,
    val bottomBarBottomPadding: Dp,
) {
    @Suppress("UNUSED_PARAMETER")
    fun footerBottomPadding(chromeVisible: Boolean): Dp = footerBottomPadding
}

internal object ReaderLayoutMetrics {
    internal val BottomBarHorizontalPadding = 5.dp
    internal val BottomBarVerticalPadding = 5.dp
    internal val ChromeIconButtonWidth = 64.dp
    internal val ChromeIconButtonHeight = 40.dp
    internal val TopBarTopPadding = 10.dp
    private val FooterBottomGap = 12.dp
    private val BottomBarFooterGap = 10.dp
    internal val FooterHeightReserve = 28.dp
    internal val FooterTextGap = 12.dp

    /** Minimum bottom reserve: footer capsule plus breathable gaps above and below. */
    internal val BottomContentReserve: Dp get() = FooterBottomGap + FooterHeightReserve + FooterTextGap

    @Suppress("UNUSED_PARAMETER")
    fun contentPadding(
        maxWidth: Dp,
        maxHeight: Dp,
        measuredFooterHeight: Dp = FooterHeightReserve,
        pageMargins: ReaderPageMargins = ReaderPageMargins.DEFAULT,
    ): ReaderContentPadding {
        val margins = pageMargins.clamped()
        val safeFooterHeight = FooterHeightReserve.coerceAtLeast(measuredFooterHeight)
        val bottomReserve = FooterBottomGap + safeFooterHeight + FooterTextGap
        val bottomBarBottomPadding = FooterBottomGap + safeFooterHeight + BottomBarFooterGap
        return ReaderContentPadding(
            startContentPadding = margins.startDp.dp,
            endContentPadding = margins.endDp.dp,
            topContentPadding = margins.topDp.dp,
            bottomContentPadding = margins.bottomDp.dp.coerceAtLeast(bottomReserve),
            footerBottomPadding = FooterBottomGap,
            bottomBarBottomPadding = bottomBarBottomPadding,
        )
    }

    fun viewport(
        maxWidth: Dp,
        maxHeight: Dp,
        padding: ReaderContentPadding,
        density: Density,
    ): ReaderViewport {
        val viewportDensity = density.density * density.fontScale
        return with(density) {
            val height = maxHeight -
                padding.topContentPadding -
                padding.bottomContentPadding
            ReaderViewport(
                widthPx = (maxWidth - padding.startContentPadding - padding.endContentPadding)
                    .coerceAtLeast(1.dp)
                    .toPx()
                    .roundToInt(),
                heightPx = height
                    .coerceAtLeast(1.dp)
                    .toPx()
                    .roundToInt(),
                density = viewportDensity,
            )
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun ReaderPageText(
    presentation: ReaderPagePresentation,
    palette: ReaderPalette,
    fontSizeSp: Int,
    lineSpacing: Float,
    fontFamily: FontFamily?,
    modifier: Modifier = Modifier,
) {
    Text(
        text = presentation.body,
        color = palette.textColor,
        style = TextStyle(
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * lineSpacing).sp,
            fontFamily = fontFamily,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        modifier = modifier,
    )
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
        color = palette.toolbarColor.copy(alpha = if (palette.isDark) 0.84f else 0.88f),
        contentColor = palette.textColor,
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 2.dp,
        shadowElevation = 5.dp,
        border = BorderStroke(1.dp, palette.textColor.copy(alpha = if (palette.isDark) 0.08f else 0.06f)),
        modifier = modifier
            .widthIn(max = 540.dp)
            .fillMaxWidth(0.90f)
            .clickable(onClick = onToggle),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = palette.textColor.copy(alpha = 0.90f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun ReaderPageFooter(
    chapterTitle: String,
    progressPercent: Int,
    palette: ReaderPalette,
    onMeasuredHeight: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Surface(
        color = palette.toolbarColor.copy(alpha = if (palette.isDark) 0.28f else 0.36f),
        contentColor = palette.textColor.copy(alpha = 0.58f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.textColor.copy(alpha = if (palette.isDark) 0.05f else 0.04f)),
        modifier = modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier.onSizeChanged { size ->
                with(density) {
                    onMeasuredHeight(size.height.toDp())
                }
            },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chapterTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textColor.copy(alpha = 0.58f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$progressPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textColor.copy(alpha = 0.58f),
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
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
    Surface(
        color = palette.toolbarColor.copy(alpha = if (palette.isDark) 0.82f else 0.88f),
        contentColor = palette.textColor,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 3.dp,
        shadowElevation = 7.dp,
        border = BorderStroke(1.dp, palette.textColor.copy(alpha = if (palette.isDark) 0.08f else 0.06f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = ReaderLayoutMetrics.BottomBarHorizontalPadding,
                vertical = ReaderLayoutMetrics.BottomBarVerticalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReaderChromeIconButton(
                icon = Icons.Filled.List,
                contentDescription = Strings.READER_CHAPTERS,
                palette = palette,
                onClick = onChapterList,
            )
            ReaderChromeIconButton(
                icon = Icons.Filled.Settings,
                contentDescription = Strings.SETTINGS_TITLE,
                palette = palette,
                onClick = onSettings,
            )
            ReaderChromeIconButton(
                icon = Icons.Filled.ArrowBack,
                contentDescription = Strings.BACK,
                palette = palette,
                onClick = onBack,
            )
        }
    }
}

@Composable
private fun ReaderChromeIconButton(
    icon: ImageVector,
    contentDescription: String,
    palette: ReaderPalette,
    onClick: () -> Unit,
) {
    Surface(
        color = palette.textColor.copy(alpha = if (palette.isDark) 0.12f else 0.07f),
        contentColor = palette.textColor.copy(alpha = 0.88f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, palette.textColor.copy(alpha = if (palette.isDark) 0.08f else 0.05f)),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(
                width = ReaderLayoutMetrics.ChromeIconButtonWidth,
                height = ReaderLayoutMetrics.ChromeIconButtonHeight,
            ),
        ) {
            Icon(icon, contentDescription = contentDescription)
        }
    }
}
