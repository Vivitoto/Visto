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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import app.visto.core.sort.SortMode
import app.visto.data.account.AlbumViewMode
import app.visto.data.account.GridDensity
import app.visto.data.account.nextAlbumFolderGridDensityOrNull
import app.visto.data.thumbnail.AnimatedThumbnailCache
import app.visto.data.thumbnail.GeneratedThumbnailCache
import app.visto.ui.Strings
import app.visto.ui.components.AnimatedThumbnailImage
import app.visto.ui.components.GeneratedThumbnailImage
import app.visto.ui.components.PausableAsyncImage
import app.visto.ui.components.rememberThumbnailAnimationsEnabled
import app.visto.ui.layout.VistoLayoutMetrics
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Precision
import okhttp3.OkHttpClient

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
    okHttpClient: OkHttpClient,
    blurThumbnails: Boolean,
    gridDensity: GridDensity,
    thumbnailCacheLimitBytes: Long,
    mediaUrlOf: (RemoteEntry) -> String,
    mediaCacheKeyOf: (RemoteEntry) -> String,
    folderPreviewPathsOf: ((RemoteEntry) -> List<String>)? = null,
    mediaUrlOfPath: ((String) -> String)? = null,
    mediaCacheKeyOfPath: ((String) -> String)? = null,
    sortMode: SortMode = SortMode.DEFAULT,
    onSortModeChange: (SortMode) -> Unit = {},
    onGridDensityChange: (GridDensity) -> Unit = {},
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
                    SortMenuAction(
                        sortMode = sortMode,
                        onChange = onSortModeChange,
                    )
                    ViewCycleAction(
                        viewMode = state.viewMode,
                        gridDensity = gridDensity,
                        onGridDensityChange = onGridDensityChange,
                        onSwitchToFolders = onSwitchToFolders,
                        onSwitchToFlat = onSwitchToFlat,
                    )
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
                state.isLoading && state.folderView.folders.isEmpty() && state.folderView.media.isEmpty() -> AlbumDetailSkeleton()
                state.errorMessage != null -> ErrorState(
                    message = state.errorMessage,
                    onRetry = onRefresh,
                )
                state.isEmpty -> EmptyAlbum(state.viewMode)
                state.viewMode == AlbumViewMode.FOLDERS -> FolderGrid(
                    folderView = state.folderView,
                    sortMode = sortMode,
                    onOpenFolder = onOpenFolder,
                    onOpenMedia = onOpenMedia,
                )
                state.viewMode == AlbumViewMode.FLAT -> FolderIconGrid(
                    folderView = state.folderView,
                    imageLoader = imageLoader,
                    okHttpClient = okHttpClient,
                    blurThumbnails = blurThumbnails,
                    gridDensity = gridDensity,
                    sortMode = sortMode,
                    mediaUrlOf = mediaUrlOf,
                    mediaCacheKeyOf = mediaCacheKeyOf,
                    thumbnailCacheLimitBytes = thumbnailCacheLimitBytes,
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
private fun ViewCycleAction(
    viewMode: AlbumViewMode,
    gridDensity: GridDensity,
    onGridDensityChange: (GridDensity) -> Unit,
    onSwitchToFolders: () -> Unit,
    onSwitchToFlat: () -> Unit,
) {
    val isList = viewMode == AlbumViewMode.FOLDERS
    val nextGridDensity = gridDensity.nextAlbumFolderGridDensityOrNull()
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    if (isList) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                )
                .clickable(
                    onClick = {
                        when {
                            isList -> {
                                onGridDensityChange(GridDensity.COMFORTABLE)
                                onSwitchToFlat()
                            }
                            nextGridDensity != null -> onGridDensityChange(nextGridDensity)
                            else -> onSwitchToFolders()
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isList) Icons.Filled.GridView else Icons.AutoMirrored.Filled.List,
                tint = if (isList) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                contentDescription = when {
                    isList -> "切换到网格：舒适"
                    nextGridDensity != null -> "切换到网格：${nextGridDensity.displayLabel}"
                    else -> Strings.ALBUM_VIEW_MODE_FOLDERS
                },
                modifier = Modifier.size(22.dp),
            )
        }
        Box {
            IconButton(
                onClick = { open = true },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = Strings.ALBUM_VIEW_MODE_MENU,
                    modifier = Modifier.size(20.dp),
                )
            }
            AlbumViewModeDropdown(
                expanded = open,
                viewMode = viewMode,
                gridDensity = gridDensity,
                onDismiss = { open = false },
                onSelectFolders = {
                    open = false
                    onSwitchToFolders()
                },
                onSelectGridDensity = { density ->
                    open = false
                    onGridDensityChange(density)
                    onSwitchToFlat()
                },
            )
        }
    }
}

@Composable
private fun AlbumViewModeDropdown(
    expanded: Boolean,
    viewMode: AlbumViewMode,
    gridDensity: GridDensity,
    onDismiss: () -> Unit,
    onSelectFolders: () -> Unit,
    onSelectGridDensity: (GridDensity) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(Strings.ALBUM_VIEW_MODE_FOLDERS) },
            onClick = onSelectFolders,
            leadingIcon = if (viewMode == AlbumViewMode.FOLDERS) {
                { Icon(Icons.Filled.Check, contentDescription = null) }
            } else null,
        )
        listOf(
            GridDensity.COMFORTABLE to "宽松/舒适网格",
            GridDensity.STANDARD to "标准网格",
            GridDensity.COMPACT to "紧凑网格",
        ).forEach { (density, label) ->
            val selected = viewMode == AlbumViewMode.FLAT && gridDensity == density
            DropdownMenuItem(
                text = { Text(label) },
                onClick = { onSelectGridDensity(density) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else null,
            )
        }
    }
}

@Composable
private fun AlbumDetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)),
                )
            }
        }
    }
}

@Composable
private fun FolderGrid(
    folderView: AlbumFolderViewState,
    sortMode: SortMode,
    onOpenFolder: (RemoteEntry) -> Unit,
    onOpenMedia: (RemoteEntry) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(folderView.currentPath, sortMode) {
        listState.scrollToItem(0)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        if (folderView.folders.isNotEmpty()) {
            listItems(
                folderView.folders,
                key = { "folder:${it.path}" },
            ) { folder ->
                FolderRow(folder = folder, onClick = { onOpenFolder(folder) })
                HorizontalDivider()
            }
            if (folderView.media.isNotEmpty()) {
                item(key = "folder-media-divider") {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
        listItems(folderView.media, key = { "media:${it.path}" }) { item ->
            AlbumMediaRow(
                item = item,
                onClick = { onOpenMedia(item) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun AlbumMediaRow(item: RemoteEntry, onClick: () -> Unit) {
    val typeLabel = when (item.mediaType) {
        MediaType.IMAGE -> "IMG"
        MediaType.ANIMATED_IMAGE -> "GIF"
        MediaType.VIDEO -> "VID"
        else -> "FILE"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
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
            text = item.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FolderIconGrid(
    folderView: AlbumFolderViewState,
    imageLoader: ImageLoader,
    okHttpClient: OkHttpClient,
    blurThumbnails: Boolean,
    gridDensity: GridDensity,
    sortMode: SortMode,
    mediaUrlOf: (RemoteEntry) -> String,
    mediaCacheKeyOf: (RemoteEntry) -> String,
    thumbnailCacheLimitBytes: Long,
    folderPreviewPathsOf: ((RemoteEntry) -> List<String>)? = null,
    mediaUrlOfPath: ((String) -> String)? = null,
    mediaCacheKeyOfPath: ((String) -> String)? = null,
    onOpenFolder: (RemoteEntry) -> Unit,
    onOpenMedia: (RemoteEntry) -> Unit,
) {
    val gridState = rememberLazyGridState()
    LaunchedEffect(folderView.currentPath, sortMode) {
        gridState.scrollToItem(0)
    }
    val playThumbnailAnimations = rememberThumbnailAnimationsEnabled(gridState.isScrollInProgress)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = VistoLayoutMetrics.albumGridMinCellWidth(gridDensity)),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(folderView.folders, key = { _, it -> "folder-icon:${it.path}" }) { index, folder ->
            val previewPaths = folderPreviewPathsOf?.invoke(folder).orEmpty()
            FolderMosaicTile(
                folder = folder,
                imageLoader = imageLoader,
                previewPaths = previewPaths,
                previewUrls = previewPaths.mapNotNull { p -> mediaUrlOfPath?.invoke(p) },
                previewCacheKeys = previewPaths.mapNotNull { p -> mediaCacheKeyOfPath?.invoke(p) },
                blurThumbnails = blurThumbnails,
                playThumbnailAnimations = playThumbnailAnimations,
                resumeDelayMs = (index % 12) * 28L,
                onClick = { onOpenFolder(folder) },
            )
        }
        itemsIndexed(folderView.media, key = { _, it -> "media:${it.path}" }) { index, item ->
            AlbumMediaTile(
                item = item,
                imageLoader = imageLoader,
                okHttpClient = okHttpClient,
                mediaUrl = mediaUrlOf(item),
                mediaCacheKey = mediaCacheKeyOf(item),
                thumbnailCacheLimitBytes = thumbnailCacheLimitBytes,
                blurThumbnails = blurThumbnails,
                playThumbnailAnimations = playThumbnailAnimations,
                resumeDelayMs = (index % 12) * 28L,
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
    blurThumbnails: Boolean,
    playThumbnailAnimations: Boolean,
    resumeDelayMs: Long = 0,
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
            val previewCount = minOf(previewUrls.size, previewCacheKeys.size)
            when {
                previewCount >= 4 -> Mosaic2x2(
                    urls = previewUrls.take(4),
                    keys = previewCacheKeys.take(4),
                    imageLoader = imageLoader,
                    blurThumbnails = blurThumbnails,
                    playThumbnailAnimations = playThumbnailAnimations,
                    resumeDelayMs = resumeDelayMs,
                )
                previewCount == 3 -> Mosaic3(
                    urls = previewUrls.take(3),
                    keys = previewCacheKeys.take(3),
                    imageLoader = imageLoader,
                    blurThumbnails = blurThumbnails,
                    playThumbnailAnimations = playThumbnailAnimations,
                    resumeDelayMs = resumeDelayMs,
                )
                previewCount == 2 -> Mosaic2(
                    urls = previewUrls.take(2),
                    keys = previewCacheKeys.take(2),
                    imageLoader = imageLoader,
                    blurThumbnails = blurThumbnails,
                    playThumbnailAnimations = playThumbnailAnimations,
                    resumeDelayMs = resumeDelayMs,
                )
                previewCount == 1 -> MosaicSingle(
                    url = previewUrls[0],
                    key = previewCacheKeys[0],
                    imageLoader = imageLoader,
                    blurThumbnails = blurThumbnails,
                    playThumbnailAnimations = playThumbnailAnimations,
                    resumeDelayMs = resumeDelayMs,
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
private fun Mosaic2x2(urls: List<String>, keys: List<String>, imageLoader: ImageLoader, blurThumbnails: Boolean, playThumbnailAnimations: Boolean, resumeDelayMs: Long = 0) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        urls.chunked(2).forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEachIndexed { columnIndex, url ->
                    val index = rowIndex * 2 + columnIndex
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        MosaicCell(url = url, key = keys.getOrNull(index) ?: url, imageLoader = imageLoader, blurThumbnails = blurThumbnails, playThumbnailAnimations = playThumbnailAnimations, resumeDelayMs = resumeDelayMs)
                    }
                }
            }
        }
    }
}

@Composable
private fun Mosaic3(urls: List<String>, keys: List<String>, imageLoader: ImageLoader, blurThumbnails: Boolean, playThumbnailAnimations: Boolean, resumeDelayMs: Long = 0) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                MosaicCell(url = urls[0], key = keys[0], imageLoader = imageLoader, blurThumbnails = blurThumbnails, playThumbnailAnimations = playThumbnailAnimations, resumeDelayMs = resumeDelayMs)
            }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                MosaicCell(url = urls[1], key = keys[1], imageLoader = imageLoader, blurThumbnails = blurThumbnails, playThumbnailAnimations = playThumbnailAnimations, resumeDelayMs = resumeDelayMs)
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            MosaicCell(url = urls[2], key = keys[2], imageLoader = imageLoader, blurThumbnails = blurThumbnails, playThumbnailAnimations = playThumbnailAnimations, resumeDelayMs = resumeDelayMs)
        }
    }
}

@Composable
private fun Mosaic2(urls: List<String>, keys: List<String>, imageLoader: ImageLoader, blurThumbnails: Boolean, playThumbnailAnimations: Boolean, resumeDelayMs: Long = 0) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        urls.forEachIndexed { i, url ->
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                MosaicCell(url = url, key = keys[i], imageLoader = imageLoader, blurThumbnails = blurThumbnails, playThumbnailAnimations = playThumbnailAnimations, resumeDelayMs = resumeDelayMs)
            }
        }
    }
}

@Composable
private fun MosaicSingle(url: String, key: String, imageLoader: ImageLoader, blurThumbnails: Boolean, playThumbnailAnimations: Boolean, resumeDelayMs: Long = 0) {
    Box(modifier = Modifier.fillMaxSize()) {
        MosaicCell(url = url, key = key, imageLoader = imageLoader, blurThumbnails = blurThumbnails, playThumbnailAnimations = playThumbnailAnimations, resumeDelayMs = resumeDelayMs)
    }
}

@Composable
private fun MosaicCell(url: String, key: String, imageLoader: ImageLoader, blurThumbnails: Boolean, playThumbnailAnimations: Boolean, resumeDelayMs: Long = 0) {
    val request = ImageRequest.Builder(LocalContext.current)
        .data(url)
        .memoryCacheKey(key)
        .diskCacheKey(key)
        .crossfade(false)
        .size(320)
        .precision(Precision.INEXACT)
        .build()
    PausableAsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        playAnimations = playThumbnailAnimations && !blurThumbnails,
        resumeDelayMs = resumeDelayMs,
        modifier = Modifier.fillMaxSize().then(if (blurThumbnails) Modifier.blur(12.dp) else Modifier),
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
        Text(text = Strings.ALBUM_DETAIL_LOADING, style = MaterialTheme.typography.bodyMedium)
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
        Text(text = display, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    okHttpClient: OkHttpClient,
    mediaUrl: String,
    mediaCacheKey: String,
    thumbnailCacheLimitBytes: Long,
    blurThumbnails: Boolean,
    playThumbnailAnimations: Boolean,
    resumeDelayMs: Long = 0,
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
                    style = MaterialTheme.typography.bodyMedium,
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
        } else if (isAnimated) {
            AnimatedThumbnailImage(
                url = mediaUrl,
                cacheKey = mediaCacheKey,
                kind = AnimatedThumbnailCache.Kind.GRID,
                imageLoader = imageLoader,
                okHttpClient = okHttpClient,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                cacheLimitBytes = thumbnailCacheLimitBytes,
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
        } else {
            GeneratedThumbnailImage(
                url = mediaUrl,
                cacheKey = mediaCacheKey,
                kind = GeneratedThumbnailCache.Kind.GRID,
                imageLoader = imageLoader,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                cacheLimitBytes = thumbnailCacheLimitBytes,
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

@Composable
private fun SortMenuAction(sortMode: SortMode, onChange: (SortMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(
            Icons.AutoMirrored.Filled.Sort,
            contentDescription = "排序方式",
        )
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        SortMode.values().forEach { mode ->
            val selected = mode == sortMode
            DropdownMenuItem(
                text = { Text(sortModeLabel(mode)) },
                onClick = {
                    onChange(mode)
                    open = false
                },
                leadingIcon = if (selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else null,
            )
        }
    }
}

private fun sortModeLabel(mode: SortMode): String = when (mode) {
    SortMode.NAME_ASC -> "名称 A→Z"
    SortMode.NAME_DESC -> "名称 Z→A"
    SortMode.MODIFIED_NEWEST_FIRST -> "最近修改在前"
    SortMode.MODIFIED_OLDEST_FIRST -> "最早修改在前"
    SortMode.SIZE_LARGEST_FIRST -> "文件最大在前"
    SortMode.SIZE_SMALLEST_FIRST -> "文件最小在前"
    SortMode.TYPE -> "按类型"
}
