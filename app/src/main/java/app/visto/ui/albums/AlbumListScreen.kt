package app.visto.ui.albums

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.visto.data.db.AlbumSourceEntity
import app.visto.ui.Strings
import app.visto.ui.components.PausableAsyncImage
import app.visto.ui.components.rememberThumbnailAnimationsEnabled
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Precision

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumListScreen(
    state: AlbumListUiState,
    coverImagePathOf: (AlbumSourceEntity) -> String?,
    coverPreviewsOf: (AlbumSourceEntity) -> List<String>,
    mediaUrlOf: (String) -> String,
    mediaCacheKeyOf: (String) -> String,
    imageLoader: ImageLoader,
    blurThumbnails: Boolean,
    onOpenAlbum: (AlbumSourceEntity) -> Unit,
    onAddRequested: () -> Unit,
    onAddDismissed: () -> Unit,
    onAddFormChange: (AlbumAddFormState) -> Unit,
    onAddSubmit: () -> Unit,
    onBrowsePathRequested: () -> Unit,
    onDeleteRequested: (AlbumSourceEntity) -> Unit,
    onOpenSettings: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val playThumbnailAnimations = rememberThumbnailAnimationsEnabled(listState.isScrollInProgress)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.ALBUMS_TITLE) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = Strings.SETTINGS_TITLE)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRequested) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = Strings.ALBUMS_ADD,
                )
            }
        },
        bottomBar = bottomBar,
    ) { innerPadding: PaddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.albums.isEmpty() -> EmptyState(onAddRequested)
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    itemsIndexed(state.albums, key = { _, it -> it.id }) { index, album ->
                        AlbumRow(
                            album = album,
                            resumeDelayMs = (index % 12) * 28L,
                            coverImagePath = coverImagePathOf(album),
                            coverPreviews = coverPreviewsOf(album),
                            mediaUrlOf = mediaUrlOf,
                            mediaCacheKeyOf = mediaCacheKeyOf,
                            imageLoader = imageLoader,
                            blurThumbnails = blurThumbnails,
                            playThumbnailAnimations = playThumbnailAnimations,
                            onClick = { onOpenAlbum(album) },
                            onLongClick = { onDeleteRequested(album) },
                        )
                    }
                }
            }
            state.errorMessage?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
    }

    if (state.showAddDialog) {
        AlbumAddDialog(
            form = state.addDialog,
            onDismiss = onAddDismissed,
            onChange = onAddFormChange,
            onSubmit = onAddSubmit,
            onBrowsePathRequested = onBrowsePathRequested,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumRow(
    album: AlbumSourceEntity,
    coverImagePath: String?,
    coverPreviews: List<String>,
    mediaUrlOf: (String) -> String,
    mediaCacheKeyOf: (String) -> String,
    imageLoader: ImageLoader,
    blurThumbnails: Boolean,
    playThumbnailAnimations: Boolean,
    resumeDelayMs: Long = 0,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    // Compact album row: small mosaic/cover thumbnail on the left + name/path
    // + actions on the right. Matches the sub-folder row density inside the
    // album so the home list does not feel oversized.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumThumbnail(
                coverImagePath = coverImagePath,
                coverPreviews = coverPreviews,
                mediaUrlOf = mediaUrlOf,
                mediaCacheKeyOf = mediaCacheKeyOf,
                imageLoader = imageLoader,
                blurThumbnails = blurThumbnails,
                playThumbnailAnimations = playThumbnailAnimations,
                resumeDelayMs = resumeDelayMs,
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = album.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = album.rootPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onLongClick) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = Strings.ALBUMS_DELETE,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AlbumThumbnail(
    coverImagePath: String?,
    coverPreviews: List<String>,
    mediaUrlOf: (String) -> String,
    mediaCacheKeyOf: (String) -> String,
    imageLoader: ImageLoader,
    blurThumbnails: Boolean,
    playThumbnailAnimations: Boolean,
    resumeDelayMs: Long = 0,
) {
    val THUMB_SIZE = 76.dp
    Box(
        modifier = Modifier
            .size(THUMB_SIZE)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            coverPreviews.size >= 2 -> CompactMosaic(
                previews = coverPreviews.take(4),
                mediaUrlOf = mediaUrlOf,
                mediaCacheKeyOf = mediaCacheKeyOf,
                imageLoader = imageLoader,
                blurThumbnails = blurThumbnails,
                playThumbnailAnimations = playThumbnailAnimations,
                resumeDelayMs = resumeDelayMs,
            )
            coverImagePath != null -> SinglePreview(
                path = coverImagePath,
                mediaUrlOf = mediaUrlOf,
                mediaCacheKeyOf = mediaCacheKeyOf,
                imageLoader = imageLoader,
                blurThumbnails = blurThumbnails,
                playThumbnailAnimations = playThumbnailAnimations,
                resumeDelayMs = resumeDelayMs,
            )
            coverPreviews.size == 1 -> SinglePreview(
                path = coverPreviews[0],
                mediaUrlOf = mediaUrlOf,
                mediaCacheKeyOf = mediaCacheKeyOf,
                imageLoader = imageLoader,
                blurThumbnails = blurThumbnails,
                playThumbnailAnimations = playThumbnailAnimations,
                resumeDelayMs = resumeDelayMs,
            )
            else -> Icon(
                imageVector = Icons.Filled.PhotoAlbum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CompactMosaic(
    previews: List<String>,
    mediaUrlOf: (String) -> String,
    mediaCacheKeyOf: (String) -> String,
    imageLoader: ImageLoader,
    blurThumbnails: Boolean,
    playThumbnailAnimations: Boolean,
    resumeDelayMs: Long = 0,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        previews.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                row.forEach { path ->
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        SinglePreview(
                            path = path,
                            mediaUrlOf = mediaUrlOf,
                            mediaCacheKeyOf = mediaCacheKeyOf,
                            imageLoader = imageLoader,
                            blurThumbnails = blurThumbnails,
                            playThumbnailAnimations = playThumbnailAnimations,
                            resumeDelayMs = resumeDelayMs,
                        )
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SinglePreview(
    path: String,
    mediaUrlOf: (String) -> String,
    mediaCacheKeyOf: (String) -> String,
    imageLoader: ImageLoader,
    blurThumbnails: Boolean,
    playThumbnailAnimations: Boolean,
    resumeDelayMs: Long = 0,
) {
    val request = ImageRequest.Builder(LocalContext.current)
        .data(mediaUrlOf(path))
        .memoryCacheKey(mediaCacheKeyOf(path))
        .diskCacheKey(mediaCacheKeyOf(path))
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
    )
}

@Composable
private fun EmptyState(onAddRequested: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = Strings.ALBUMS_EMPTY_TITLE, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = Strings.ALBUMS_EMPTY_SUBTITLE)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddRequested) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(text = Strings.ALBUMS_ADD)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumAddDialog(
    form: AlbumAddFormState,
    onDismiss: () -> Unit,
    onChange: (AlbumAddFormState) -> Unit,
    onSubmit: () -> Unit,
    onBrowsePathRequested: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.ALBUMS_ADD_DIALOG_TITLE) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.path,
                    onValueChange = { onChange(AlbumAddFormReducer.updatePath(form, it)) },
                    label = { Text(Strings.ALBUMS_PATH_LABEL) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = onBrowsePathRequested,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(Strings.ALBUMS_BROWSE)
                }
                Text(
                    text = Strings.ALBUMS_PATH_HINT,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { onChange(AlbumAddFormReducer.updateName(form, it)) },
                    label = { Text(Strings.ALBUMS_NAME_LABEL) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                form.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = onSubmit, enabled = !form.isSaving) {
                Text(if (form.isSaving) Strings.ACCOUNT_SAVING else Strings.ALBUMS_SAVE)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.ALBUMS_CANCEL) }
        },
    )
}
