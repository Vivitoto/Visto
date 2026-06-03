package app.visto.ui.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import app.visto.ui.Strings
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Renders an album as a vertical stack of folder sections.
 *
 * The same Coil + Media3 + auth interceptor pipeline used by the legacy
 * browser screen powers the thumbnails here. Folders aren't navigable from
 * this screen — the album already collapsed the directory tree into one
 * scrolling timeline grouped by subfolder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    state: AlbumDetailUiState,
    imageLoader: ImageLoader,
    mediaUrlOf: (RemoteEntry) -> String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenMedia: (RemoteEntry) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.title) },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
                actions = { IconButton(onClick = onRefresh) { Text("⟳") } },
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
                state.isEmpty -> EmptyAlbum()
                state.errorMessage != null -> Text(
                    text = state.errorMessage,
                    modifier = Modifier.padding(16.dp),
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    state.sections.forEachIndexed { index, section ->
                        item(key = "header-${section.parentPath}") {
                            SectionHeader(section)
                        }
                        item(key = "grid-${section.parentPath}") {
                            SectionGrid(
                                section = section,
                                imageLoader = imageLoader,
                                mediaUrlOf = mediaUrlOf,
                                onOpenMedia = onOpenMedia,
                            )
                            if (index != state.sections.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
            if (state.warnings.isNotEmpty()) {
                Text(
                    text = state.warnings.first(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(state: AlbumDetailUiState) {
    if (!state.isLoading) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = Strings.ALBUM_DETAIL_LOADING, style = MaterialTheme.typography.bodySmall)
        Text(
            text = Strings.albumDetailProgress(state.foldersVisited, state.foldersFailed),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun EmptyAlbum() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = Strings.ALBUM_DETAIL_NO_MEDIA)
    }
}

@Composable
private fun SectionHeader(section: AlbumDetailSection) {
    val display = if (section.title.isEmpty()) Strings.ALBUM_DETAIL_ROOT_SECTION else section.title
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = display, style = MaterialTheme.typography.titleSmall)
        Text(
            text = Strings.albumDetailSectionCount(section.media.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionGrid(
    section: AlbumDetailSection,
    imageLoader: ImageLoader,
    mediaUrlOf: (RemoteEntry) -> String,
    onOpenMedia: (RemoteEntry) -> Unit,
) {
    val rowCount = ((section.media.size + 2) / 3).coerceAtLeast(1)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height((rowCount * 120).dp.coerceAtMost(900.dp)),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        userScrollEnabled = false,
    ) {
        items(section.media, key = { it.path }) { item ->
            AlbumMediaTile(
                item = item,
                imageLoader = imageLoader,
                mediaUrl = mediaUrlOf(item),
                onClick = { onOpenMedia(item) },
            )
        }
    }
}

@Composable
private fun AlbumMediaTile(
    item: RemoteEntry,
    imageLoader: ImageLoader,
    mediaUrl: String,
    onClick: () -> Unit,
) {
    val badge = when (item.mediaType) {
        MediaType.ANIMATED_IMAGE -> "GIF"
        MediaType.VIDEO -> "▶"
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
        val request = ImageRequest.Builder(LocalContext.current)
            .data(mediaUrl)
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
        badge?.let { Text(text = it, modifier = Modifier.padding(4.dp)) }
    }
}
