package app.visto.ui.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import app.visto.data.account.AlbumViewMode
import app.visto.ui.Strings
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Renders an album with two interchangeable view modes:
 *
 *  - [AlbumViewMode.FOLDERS]: a navigable file browser scoped to the album.
 *    Subdirectories and direct media at the current path are shown in one
 *    grid; tapping a folder navigates down, the screen's back action goes
 *    up (or back to the album list at the album root).
 *  - [AlbumViewMode.FLAT]: a recursive scan grouped by subfolder, useful
 *    when the album is one logical photo set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    state: AlbumDetailUiState,
    albumRootPath: String,
    imageLoader: ImageLoader,
    mediaUrlOf: (RemoteEntry) -> String,
    mediaCacheKeyOf: (RemoteEntry) -> String,
    folderPreviewPathsOf: ((RemoteEntry) -> List<String>)? = null,
    mediaUrlOfPath: ((String) -> String)? = null,
    mediaCacheKeyOfPath: ((String) -> String)? = null,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenFolder: (RemoteEntry) -> Unit,
    onOpenMedia: (RemoteEntry) -> Unit,
    onSwitchToFolders: () -> Unit,
    onSwitchToFlat: () -> Unit,
) {
    val displayTitle = when (state.viewMode) {
        AlbumViewMode.FOLDERS -> {
            val relative = relativePath(albumRootPath, state.folderView.currentPath)
            if (relative.isEmpty()) state.title else "${state.title} / $relative"
        }
        AlbumViewMode.FLAT -> state.title
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = displayTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    when (state.viewMode) {
                        AlbumViewMode.FOLDERS -> IconButton(onClick = onSwitchToFlat) {
                            Icon(
                                Icons.Filled.GridView,
                                contentDescription = Strings.ALBUM_VIEW_MODE_FLAT,
                            )
                        }
                        AlbumViewMode.FLAT -> IconButton(onClick = onSwitchToFolders) {
                            Icon(
                                Icons.Filled.AccountTree,
                                contentDescription = Strings.ALBUM_VIEW_MODE_FOLDERS,
                            )
                        }
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = Strings.ALBUM_DETAIL_REFRESH)
                    }
                },
            )
        },
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            ProgressBar(state)
            when {
                state.errorMessage != null -> ErrorState(
                    message = state.errorMessage,
                    onRetry = onRefresh,
                )
                state.isEmpty -> EmptyAlbum(state.viewMode)
                state.viewMode == AlbumViewMode.FOLDERS -> FolderGrid(
                    folderView = state.folderView,
                    imageLoader = imageLoader,
                    mediaUrlOf = mediaUrlOf,
                    mediaCacheKeyOf = mediaCacheKeyOf,
                    onOpenFolder = onOpenFolder,
                    onOpenMedia = onOpenMedia,
                )
                state.viewMode == AlbumViewMode.FLAT -> FolderIconGrid(
                    folderView = state.folderView,
                    imageLoader = imageLoader,
                    mediaUrlOf = mediaUrlOf,
                    mediaCacheKeyOf = mediaCacheKeyOf,
                    folderPreviewPathsOf = folderPreviewPathsOf,
                    mediaUrlOfPath = mediaUrlOfPath,
                    mediaCacheKeyOfPath = mediaCacheKeyOfPath,
                    onOpenFolder = onOpenFolder,
                    onOpenMedia = onOpenMedia,
                )
            }
        }
    }
}

@Composable
private fun FolderGrid(
    folderView: AlbumFolderViewState,
    imageLoader: ImageLoader,
    mediaUrlOf: (RemoteEntry) -> String,
    mediaCacheKeyOf: (RemoteEntry) -> String,
    onOpenFolder: (RemoteEntry) -> Unit,
    onOpenMedia: (RemoteEntry) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (folderView.folders.isNotEmpty()) {
            items(
                folderView.folders,
                span = { GridItemSpan(maxLineSpan) },
                key = { "folder:${it.path}" },
            ) { folder ->
                FolderRow(folder = folder, onClick = { onOpenFolder(folder) })
            }
            if (folderView.media.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "folder-media-divider") {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
        items(folderView.media, key = { "media:${it.path}" }) { item ->
            AlbumMediaTile(
                item = item,
                imageLoader = imageLoader,
                mediaUrl = mediaUrlOf(item),
                mediaCacheKey = mediaCacheKeyOf(item),
                onClick = { onOpenMedia(item) },
            )
        }
    }
}

@Composable
private fun FolderRow(folder: RemoteEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = folder.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FolderIconGrid(
    folderView: AlbumFolderViewState,
    imageLoader: ImageLoader,
    mediaUrlOf: (RemoteEntry) -> String,
    mediaCacheKeyOf: (RemoteEntry) -> String,
    folderPreviewPathsOf: ((RemoteEntry) -> List<String>)? = null,
    mediaUrlOfPath: ((String) -> String)? = null,
    mediaCacheKeyOfPath: ((String) -> String)? = null,
    onOpenFolder: (RemoteEntry) -> Unit,
    onOpenMedia: (RemoteEntry) -> Unit,
) {
    // 2 columns: bigger tiles so the 2x2 mosaic is actually legible.
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(folderView.folders, key = { "folder-icon:${it.path}" }) { folder ->
            val previewPaths = folderPreviewPathsOf?.invoke(folder).orEmpty()
            FolderMosaicTile(
                folder = folder,
                imageLoader = imageLoader,
                previewPaths = previewPaths,
                previewUrls = previewPaths.mapNotNull { p -> mediaUrlOfPath?.invoke(p) },
                previewCacheKeys = previewPaths.mapNotNull { p -> mediaCacheKeyOfPath?.invoke(p) },
                onClick = { onOpenFolder(folder) },
            )
        }
        items(folderView.media, key = { "media:${it.path}" }) { item ->
            AlbumMediaTile(
                item = item,
                imageLoader = imageLoader,
                mediaUrl = mediaUrlOf(item),
                mediaCacheKey = mediaCacheKeyOf(item),
                onClick = { onOpenMedia(item) },
            )
        }
    }
}

@Composable
private fun FolderMosaicTile(
    folder: RemoteEntry,
    imageLoader: ImageLoader,
    previewPaths: List<String>,
    previewUrls: List<String>,
    previewCacheKeys: List<String>,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            when {
                previewPaths.size >= 4 -> Mosaic2x2(
                    urls = previewUrls.take(4),
                    keys = previewCacheKeys.take(4),
                    imageLoader = imageLoader,
                )
                previewPaths.size == 3 -> Mosaic3(
                    urls = previewUrls.take(3),
                    keys = previewCacheKeys.take(3),
                    imageLoader = imageLoader,
                )
                previewPaths.size == 2 -> Mosaic2(
                    urls = previewUrls.take(2),
                    keys = previewCacheKeys.take(2),
                    imageLoader = imageLoader,
                )
                previewPaths.size == 1 -> MosaicSingle(
                    url = previewUrls[0],
                    key = previewCacheKeys[0],
                    imageLoader = imageLoader,
                )
                else -> Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize(0.46f),
                )
            }
        }
        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Mosaic2x2(urls: List<String>, keys: List<String>, imageLoader: ImageLoader) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        urls.chunked(2).forEach { row ->
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEachIndexed { i, url ->
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        MosaicCell(url = url, key = keys.getOrNull(i) ?: url, imageLoader = imageLoader)
                    }
                }
            }
        }
    }
}

@Composable
private fun Mosaic3(urls: List<String>, keys: List<String>, imageLoader: ImageLoader) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                MosaicCell(url = urls[0], key = keys[0], imageLoader = imageLoader)
            }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                MosaicCell(url = urls[1], key = keys[1], imageLoader = imageLoader)
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            MosaicCell(url = urls[2], key = keys[2], imageLoader = imageLoader)
        }
    }
}

@Composable
private fun Mosaic2(urls: List<String>, keys: List<String>, imageLoader: ImageLoader) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        urls.forEachIndexed { i, url ->
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                MosaicCell(url = url, key = keys[i], imageLoader = imageLoader)
            }
        }
    }
}

@Composable
private fun MosaicSingle(url: String, key: String, imageLoader: ImageLoader) {
    Box(modifier = Modifier.fillMaxSize()) {
        MosaicCell(url = url, key = key, imageLoader = imageLoader)
    }
}

@Composable
private fun MosaicCell(url: String, key: String, imageLoader: ImageLoader) {
    val request = ImageRequest.Builder(LocalContext.current)
        .data(url)
        .memoryCacheKey(key)
        .diskCacheKey(key)
        .crossfade(true)
        .size(256)
        .build()
    SubcomposeAsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
        loading = { /* blank */ },
        error = {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxSize(0.4f),
            )
        },
    )
}

@Composable
private fun ProgressBar(state: AlbumDetailUiState) {
    if (!state.isLoading) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(text = Strings.ALBUM_DETAIL_LOADING, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyAlbum(viewMode: AlbumViewMode) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(Strings.ALBUM_FOLDER_EMPTY)
    }
}

private fun relativePath(root: String, current: String): String {
    val normalizedRoot = root.trimEnd('/')
    val normalizedCurrent = current.trimEnd('/')
    if (normalizedRoot.isEmpty() || normalizedRoot == "/") {
        return normalizedCurrent.trimStart('/')
    }
    if (normalizedCurrent == normalizedRoot) return ""
    val prefix = "$normalizedRoot/"
    return if (normalizedCurrent.startsWith(prefix)) {
        normalizedCurrent.removePrefix(prefix)
    } else {
        normalizedCurrent.trimStart('/')
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = message)
        Button(onClick = onRetry) { Text(Strings.RETRY) }
    }
}

@Composable
private fun SectionHeader(section: AlbumDetailSection) {
    val display = if (section.title.isEmpty()) Strings.ALBUM_DETAIL_ROOT_SECTION else section.title
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
        Text(text = display, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = Strings.albumDetailSectionCount(section.media.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AlbumMediaTile(
    item: RemoteEntry,
    imageLoader: ImageLoader,
    mediaUrl: String,
    mediaCacheKey: String,
    onClick: () -> Unit,
) {
    val isVideo = item.mediaType == MediaType.VIDEO
    val isAnimated = item.mediaType == MediaType.ANIMATED_IMAGE
    val badge = when {
        isAnimated -> "GIF"
        isVideo -> null
        else -> null
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomEnd,
    ) {
        if (isVideo) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "VIDEO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val request = ImageRequest.Builder(LocalContext.current)
                .data(mediaUrl)
                .memoryCacheKey(mediaCacheKey)
                .diskCacheKey(mediaCacheKey)
                .crossfade(true)
                .build()
            SubcomposeAsyncImage(
                model = request,
                imageLoader = imageLoader,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
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
        if (item.mediaType == MediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.68f))
                    .padding(8.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
        } else {
            badge?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
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
