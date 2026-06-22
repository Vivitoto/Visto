package app.visto.ui.reader

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

@Composable
fun ReaderScreen(
    session: ReaderSession,
    onAction: (ReaderAction) -> Unit,
    onBack: () -> Unit,
    onSaveProgress: () -> Unit,
) {
    HideStatusBar()

    val colors = session.theme.colors()
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var showSettings by remember { mutableStateOf(false) }
    var showChapters by remember { mutableStateOf(false) }
    var dragDistance by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .onSizeChanged { viewportSize = it }
            .pointerInput(viewportSize, session.currentPage, session.showToolbar) {
                detectTapGestures { offset ->
                    val width = viewportSize.width.takeIf { it > 0 } ?: size.width
                    when {
                        offset.x <= width * 0.20f -> onAction(ReaderAction.PrevPage)
                        offset.x >= width * 0.80f -> onAction(ReaderAction.NextPage)
                        else -> onAction(ReaderAction.ToggleToolbar)
                    }
                }
            }
            .pointerInput(session.currentPage) {
                detectHorizontalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragDistance += dragAmount },
                    onDragEnd = {
                        when {
                            dragDistance < -48f -> onAction(ReaderAction.NextPage)
                            dragDistance > 48f -> onAction(ReaderAction.PrevPage)
                        }
                        dragDistance = 0f
                    },
                    onDragCancel = { dragDistance = 0f },
                )
            },
    ) {
        when {
            session.isLoading -> LoadingState(colors = colors)
            session.errorMessage != null -> ErrorState(
                message = session.errorMessage,
                colors = colors,
                onRetry = { onAction(ReaderAction.Retry) },
            )
            session.pagesForCurrentChapter.isEmpty() -> EmptyState(colors = colors)
            else -> ReaderPageContent(session = session, colors = colors)
        }

        if (session.showToolbar) {
            ReaderTopToolbar(
                session = session,
                colors = colors,
                onBack = {
                    onSaveProgress()
                    onBack()
                },
                modifier = Modifier.align(Alignment.TopCenter),
            )
            ReaderBottomToolbar(
                session = session,
                colors = colors,
                onOpenChapters = { showChapters = true },
                onOpenSettings = { showSettings = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showSettings) {
        ReaderSettingsSheet(
            fontSizeSp = session.fontSizeSp,
            lineSpacing = session.lineSpacing,
            theme = session.theme,
            onDismiss = { showSettings = false },
            onApply = { fontSizeSp, lineSpacing, theme ->
                onAction(ReaderAction.SetFontSize(fontSizeSp))
                onAction(ReaderAction.SetLineSpacing(lineSpacing))
                onAction(ReaderAction.SetTheme(theme))
            },
        )
    }

    if (showChapters) {
        ChapterListSheet(
            chapters = session.chapters,
            currentChapterIndex = session.currentChapterIndex,
            onSelectChapter = { onAction(ReaderAction.GoToChapter(it)) },
            onDismiss = { showChapters = false },
        )
    }
}

@Composable
private fun HideStatusBar() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.hide(WindowInsetsCompat.Type.statusBars())
            onDispose { controller.show(WindowInsetsCompat.Type.statusBars()) }
        } else {
            onDispose { }
        }
    }
}

@Composable
private fun ReaderTopToolbar(
    session: ReaderSession,
    colors: ReaderColors,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = colors.toolbarBackground,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回", tint = colors.toolbarText)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.chapters.getOrNull(session.currentChapterIndex)?.title ?: session.fileName,
                        color = colors.toolbarText,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${(session.currentPage + 1).coerceAtLeast(1)} / ${session.pagesForCurrentChapter.size.coerceAtLeast(1)}",
                        color = colors.toolbarText.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            HorizontalDivider(color = colors.divider)
        }
    }
}

@Composable
private fun ReaderBottomToolbar(
    session: ReaderSession,
    colors: ReaderColors,
    onOpenChapters: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = colors.toolbarBackground,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            HorizontalDivider(color = colors.divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${session.currentChapterIndex + 1} / ${session.chapters.size.coerceAtLeast(1)} 章 · ${session.currentPage + 1} / ${session.pagesForCurrentChapter.size.coerceAtLeast(1)} 页",
                    color = colors.toolbarText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenChapters) {
                    Icon(Icons.Filled.MenuBook, contentDescription = "目录", tint = colors.toolbarText)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "设置", tint = colors.toolbarText)
                }
            }
        }
    }
}

@Composable
private fun ReaderPageContent(
    session: ReaderSession,
    colors: ReaderColors,
) {
    val pageText = session.pagesForCurrentChapter.getOrNull(session.currentPage)?.text.orEmpty()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 88.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = pageText,
            color = colors.text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = session.fontSizeSp.sp,
                lineHeight = (session.fontSizeSp * session.lineSpacing).sp,
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LoadingState(colors: ReaderColors) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = colors.text)
    }
}

@Composable
private fun ErrorState(
    message: String,
    colors: ReaderColors,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = colors.text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(16.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun EmptyState(colors: ReaderColors) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = colors.toolbarBackground,
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                text = "无内容",
                color = colors.toolbarText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}
