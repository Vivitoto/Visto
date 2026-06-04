package app.visto.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import app.visto.ui.Strings
import app.visto.ui.components.PausableAsyncImage
import app.visto.ui.components.rememberThumbnailAnimationsEnabled
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Precision

/**
 * Renders the directory browser: folders first, then a media grid.
 *
 * Implemented as a single LazyVerticalGrid so the folder list and media
 * grid live inside one scroll container. Headers and folder rows span all
 * columns; only media tiles fill grid cells.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    state: BrowserUiState,
    imageLoader: ImageLoader,
    blurThumbnails: Boolean,
    mediaUrlOf: (RemoteEntry) -> String,
    mediaCacheKeyOf: (RemoteEntry) -> String,
    onBack: () -> Unit,
    onOpenFolder: (RemoteEntry) -> Unit,
    onOpenMedia: (RemoteEntry) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    canGoBack: Boolean = true,
    bottomBar: @Composable () -> Unit = {},
) {
    val gridState = rememberLazyGridState()
    val playThumbnailAnimations = rememberThumbnailAnimationsEnabled(gridState.isScrollInProgress)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.currentPath, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = Strings.ALBUM_DETAIL_REFRESH)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = Strings.SETTINGS_TITLE)
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.isRefreshing && !state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (state.isLoading && state.isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            state.errorMessage?.let {
                Text(text = it, modifier = Modifier.padding(16.dp))
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
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.folders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "header-folders") {
                        SectionHeader(Strings.BROWSER_FOLDERS)
                    }
                    items(state.folders, span = { GridItemSpan(maxLineSpan) }, key = { "folder-${it.path}" }) { folder ->
                        FolderRow(folder, onClick = { onOpenFolder(folder) })
                        HorizontalDivider()
                    }
                }
                if (state.media.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "header-media") {
                        SectionHeader(Strings.BROWSER_MEDIA)
                    }
                    itemsIndexed(state.media, key = { _, it -> "media-${it.path}" }) { index, item ->
                        MediaTile(
                            media = item,
                            imageLoader = imageLoader,
                            mediaUrl = mediaUrlOf(item),
                            mediaCacheKey = mediaCacheKeyOf(item),
                            blurThumbnails = blurThumbnails,
                            playThumbnailAnimations = playThumbnailAnimations,
                            resumeDelayMs = (index % 12) * 28L,
                            onClick = { onOpenMedia(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun MediaTile(
    media: RemoteEntry,
    imageLoader: ImageLoader,
    mediaUrl: String,
    mediaCacheKey: String,
    blurThumbnails: Boolean,
    playThumbnailAnimations: Boolean,
    resumeDelayMs: Long = 0,
    onClick: () -> Unit,
) {
    val isVideo = media.mediaType == MediaType.VIDEO
    val typeBadge = when (media.mediaType) {
        MediaType.ANIMATED_IMAGE -> "GIF"
        MediaType.VIDEO -> "VIDEO"
        else -> null
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.BottomEnd,
        ) {
            if (isVideo) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "VIDEO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (media.mediaType != MediaType.OTHER && media.mediaType != MediaType.UNKNOWN) {
                val request = ImageRequest.Builder(LocalContext.current)
                    .data(mediaUrl)
                    .memoryCacheKey(mediaCacheKey)
                    .diskCacheKey(mediaCacheKey)
                    .crossfade(false)
                    .size(320)
                    .precision(Precision.INEXACT)
                    .build()
                PausableAsyncImage(
                    model = request,
                    imageLoader = imageLoader,
                    contentDescription = media.name,
                    contentScale = ContentScale.Crop,
                    playAnimations = playThumbnailAnimations && !blurThumbnails,
                    resumeDelayMs = resumeDelayMs,
                    modifier = Modifier.fillMaxSize().then(if (blurThumbnails) Modifier.blur(12.dp) else Modifier),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator() }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { Text(text = "!") }
                    },
                )
            }
            typeBadge?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .padding(4.dp),
                )
            }
        }
        Text(
            text = media.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Suppress("unused")
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return "%.1f %s".format(value, units[unit])
}
