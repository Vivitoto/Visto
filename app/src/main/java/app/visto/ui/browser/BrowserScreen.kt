package app.visto.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Renders the directory browser: folders first, then a media grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    state: BrowserUiState,
    imageLoader: ImageLoader,
    mediaUrlOf: (RemoteEntry) -> String,
    onBack: () -> Unit,
    onOpenFolder: (RemoteEntry) -> Unit,
    onOpenMedia: (RemoteEntry) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.currentPath) },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
                actions = {
                    IconButton(onClick = onRefresh) { Text("⟳") }
                    IconButton(onClick = onOpenSettings) { Text("⚙") }
                },
            )
        },
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.isLoading) {
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
            if (state.folders.isNotEmpty()) {
                Text(
                    text = "Folders",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(state.folders, key = { it.path }) { folder ->
                        FolderRow(folder, onClick = { onOpenFolder(folder) })
                        HorizontalDivider()
                    }
                }
            }
            if (state.media.isNotEmpty()) {
                Text(
                    text = "Media",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.media, key = { it.path }) { item ->
                        MediaTile(
                            media = item,
                            imageLoader = imageLoader,
                            mediaUrl = mediaUrlOf(item),
                            onClick = { onOpenMedia(item) },
                        )
                    }
                }
            }
            if (!state.isLoading && state.isEmpty && state.errorMessage == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "This folder is empty.")
                }
            }
        }
    }
}

@Composable
private fun FolderRow(folder: RemoteEntry, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = "📁  ${folder.name}")
    }
}

@Composable
private fun MediaTile(
    media: RemoteEntry,
    imageLoader: ImageLoader,
    mediaUrl: String,
    onClick: () -> Unit,
) {
    val typeBadge = when (media.mediaType) {
        MediaType.ANIMATED_IMAGE -> "GIF"
        MediaType.VIDEO -> "▶"
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
            if (media.mediaType != MediaType.OTHER && media.mediaType != MediaType.UNKNOWN) {
                val request = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(mediaUrl)
                    .crossfade(true)
                    .build()
                SubcomposeAsyncImage(
                    model = request,
                    imageLoader = imageLoader,
                    contentDescription = media.name,
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
            typeBadge?.let { Text(text = it, modifier = Modifier.padding(4.dp)) }
        }
        Text(
            text = media.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}
