package app.visto.ui.albums

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import app.visto.data.db.AlbumSourceEntity
import app.visto.ui.Strings
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumListScreen(
    state: AlbumListUiState,
    coverImagePathOf: (AlbumSourceEntity) -> String?,
    mediaUrlOf: (String) -> String,
    mediaCacheKeyOf: (String) -> String,
    imageLoader: ImageLoader,
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(state.albums, key = { it.id }) { album ->
                        AlbumRow(
                            album = album,
                            coverImagePath = coverImagePathOf(album),
                            mediaUrlOf = mediaUrlOf,
                            mediaCacheKeyOf = mediaCacheKeyOf,
                            imageLoader = imageLoader,
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
    mediaUrlOf: (String) -> String,
    mediaCacheKeyOf: (String) -> String,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumCover(
                coverImagePath = coverImagePath,
                mediaUrlOf = mediaUrlOf,
                mediaCacheKeyOf = mediaCacheKeyOf,
                imageLoader = imageLoader,
            )
            Column(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = album.displayName,
                    style = MaterialTheme.typography.titleMedium,
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
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AlbumCover(
    coverImagePath: String?,
    mediaUrlOf: (String) -> String,
    mediaCacheKeyOf: (String) -> String,
    imageLoader: ImageLoader,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        if (coverImagePath != null) {
            val request = ImageRequest.Builder(LocalContext.current)
                .data(mediaUrlOf(coverImagePath))
                .memoryCacheKey(mediaCacheKeyOf(coverImagePath))
                .diskCacheKey(mediaCacheKeyOf(coverImagePath))
                .crossfade(true)
                .build()
            SubcomposeAsyncImage(
                model = request,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Icon(
                        Icons.Filled.PhotoAlbum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                error = {
                    Icon(
                        Icons.Filled.PhotoAlbum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        } else {
            Icon(
                Icons.Filled.PhotoAlbum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
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
