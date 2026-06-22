package app.visto.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReaderScreen(
    session: ReaderSession,
    onBack: () -> Unit,
    onChapterSelect: (Int) -> Unit,
    onSettingsToggle: () -> Unit,
    onSaveProgress: (ReaderSession) -> Unit,
) {
    val theme = session.theme
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

    fun closeReader() {
        saveProgress()
        onBack()
    }

    BackHandler(onBack = ::closeReader)

    Scaffold { innerPadding: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(theme.backgroundColor),
        ) {
            when {
                session.isLoading -> ReaderLoading(theme)
                session.errorMessage != null -> ReaderError(theme, session.errorMessage, onBack = closeReader)
                pages.isEmpty() -> ReaderEmpty(theme, onBack = closeReader)
                else -> {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(session.currentChapterIndex, pages.size) {
                                detectTapGestures { offset ->
                                    val leftEdge = size.width / 3f
                                    val rightEdge = size.width * 2f / 3f
                                    when {
                                        offset.x < leftEdge -> {
                                            if (currentPage > 0) {
                                                currentPage -= 1
                                                saveProgress(currentPage)
                                            } else if (session.currentChapterIndex > 0) {
                                                onChapterSelect(session.currentChapterIndex - 1)
                                            }
                                        }
                                        offset.x > rightEdge -> {
                                            if (currentPage < pages.lastIndex) {
                                                currentPage += 1
                                                saveProgress(currentPage)
                                            } else if (session.currentChapterIndex < session.chapters.lastIndex) {
                                                onChapterSelect(session.currentChapterIndex + 1)
                                            }
                                        }
                                        else -> chromeVisible = !chromeVisible
                                    }
                                }
                            },
                    ) {
                        val horizontalPadding = 22.dp
                        val verticalPadding = if (chromeVisible) 88.dp else 28.dp
                        Text(
                            text = pages.getOrNull(safePage)?.text.orEmpty(),
                            color = theme.textColor,
                            fontSize = session.fontSizeSp.sp,
                            lineHeight = (session.fontSizeSp * session.lineSpacing).sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                        )
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
                    chapterTitle = currentChapter?.title ?: "当前章节",
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
        Text(text = "加载失败", color = theme.textColor, style = MaterialTheme.typography.titleLarge)
        Text(
            text = message,
            color = theme.textColor.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onBack, modifier = Modifier.padding(top = 20.dp)) {
            Text("返回")
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
        Text(text = "暂无可阅读内容", color = theme.textColor, style = MaterialTheme.typography.titleLarge)
        Button(onClick = onBack, modifier = Modifier.padding(top = 20.dp)) {
            Text("返回")
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
                text = "$chapterTitle $page/$totalPages 页",
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
                    Icon(Icons.Filled.List, contentDescription = "目录")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "目录", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = "设置")
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        }
    }
}
