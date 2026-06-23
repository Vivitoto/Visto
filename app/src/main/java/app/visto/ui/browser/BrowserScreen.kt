package app.visto.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import app.visto.ui.Strings
import app.visto.ui.layout.VistoLayoutMetrics

/**
 * Renders the WebDAV browser as a plain file/path list.
 *
 * The browser is intentionally not a gallery: folders and supported media are
 * shown as rows so it behaves like a lightweight path picker/file explorer.
 */
@Composable
fun BrowserScreen(
    state: BrowserUiState,
    onBack: () -> Unit,
    onGoRoot: () -> Unit,
    onOpenFolder: (RemoteEntry) -> Unit,
    onOpenMedia: (RemoteEntry) -> Unit,
    onOpenBook: (RemoteEntry) -> Unit,
    onRefresh: () -> Unit,
    canGoBack: Boolean = true,
    canGoRoot: Boolean = false,
    bottomBar: @Composable () -> Unit = {},
) {
    val density = LocalDensity.current
    var fabHeight by remember { mutableStateOf(VistoLayoutMetrics.DefaultFloatingActionButtonHeight) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onRefresh,
                modifier = Modifier.onSizeChanged { size ->
                    with(density) {
                        fabHeight = size.height.toDp()
                    }
                },
            ) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = Strings.ALBUM_DETAIL_REFRESH)
                }
            }
        },
        bottomBar = bottomBar,
    ) { innerPadding: PaddingValues ->
        val bottomContentPadding = VistoLayoutMetrics.scrollEndPadding(
            floatingActionButtonHeight = fabHeight,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 8.dp),
        ) {
            if (state.isRefreshing && !state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (state.isLoading && state.isEmpty) {
                BrowserSkeleton()
                return@Column
            }
            state.errorMessage?.let {
                BrowserErrorState(message = it, onRetry = onRefresh)
            }
            if (!state.isLoading && state.isEmpty && state.errorMessage == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = Strings.BROWSER_EMPTY)
                }
                return@Column
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = bottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item(key = "browser-path") {
                    BrowserPathRow(
                        currentPath = state.currentPath,
                        canGoBack = canGoBack,
                        canGoRoot = canGoRoot,
                        onBack = onBack,
                        onGoRoot = onGoRoot,
                    )
                }
                if (state.folders.isNotEmpty()) {
                    item(key = "header-folders") { SectionHeader(Strings.BROWSER_FOLDERS) }
                    items(state.folders, key = { "folder-${it.path}" }) { folder ->
                        FolderRow(folder, onClick = { onOpenFolder(folder) })
                        HorizontalDivider()
                    }
                }
                if (state.media.isNotEmpty()) {
                    item(key = "header-media") { SectionHeader(Strings.BROWSER_MEDIA) }
                    items(state.media, key = { "media-${it.path}" }) { media ->
                        MediaFileRow(media, onClick = { onOpenMedia(media) })
                        HorizontalDivider()
                    }
                }
                if (state.books.isNotEmpty()) {
                    item(key = "header-books") { SectionHeader(Strings.BROWSER_BOOKS) }
                    items(state.books, key = { "book-${it.path}" }) { book ->
                        BookRow(book, onClick = { onOpenBook(book) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowserSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(8) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)),
            )
        }
    }
}

@Composable
private fun BrowserErrorState(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Button(onClick = onRetry) {
            Text(Strings.BROWSER_RELOAD)
        }
    }
}

@Composable
private fun BrowserPathRow(
    currentPath: String,
    canGoBack: Boolean,
    canGoRoot: Boolean,
    onBack: () -> Unit,
    onGoRoot: () -> Unit,
) {
    val folderName = currentPath
        .trimEnd('/')
        .substringAfterLast('/')
        .ifBlank { Strings.ALBUM_DETAIL_ROOT_SECTION }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canGoBack) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = Strings.BROWSER_GO_UP,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (canGoBack) Modifier.clickable(onClick = onBack) else Modifier),
        ) {
            Text(
                text = folderName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.84f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = currentPath,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onGoRoot, enabled = canGoRoot) {
            Icon(
                Icons.Filled.Home,
                contentDescription = Strings.BROWSER_GO_ROOT,
                tint = if (canGoRoot) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FolderRow(folder: RemoteEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = folder.name,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun BookRow(book: RemoteEntry, onClick: () -> Unit) {
    val typeLabel = when (book.mediaType) {
        MediaType.EPUB_BOOK -> Strings.BROWSER_BOOK_TYPE_EPUB
        else -> Strings.BROWSER_BOOK_TYPE_TXT
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.Book,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun MediaFileRow(media: RemoteEntry, onClick: () -> Unit) {
    val typeLabel = when (media.mediaType) {
        MediaType.IMAGE -> "IMG"
        MediaType.ANIMATED_IMAGE -> "GIF"
        MediaType.VIDEO -> "VID"
        else -> "FILE"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = typeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .padding(horizontal = 7.dp, vertical = 4.dp),
        )
        Text(
            text = media.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}
