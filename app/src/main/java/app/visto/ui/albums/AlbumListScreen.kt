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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import app.visto.data.db.AlbumSourceEntity
import app.visto.ui.Strings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumListScreen(
    state: AlbumListUiState,
    onOpenAlbum: (AlbumSourceEntity) -> Unit,
    onAddRequested: () -> Unit,
    onAddDismissed: () -> Unit,
    onAddFormChange: (AlbumAddFormState) -> Unit,
    onAddSubmit: () -> Unit,
    onBrowsePathRequested: () -> Unit,
    onDeleteRequested: (AlbumSourceEntity) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.ALBUMS_TITLE) },
                actions = {
                    IconButton(onClick = onOpenSettings) { Text("⚙") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRequested) { Text("+") }
        },
    ) { innerPadding: PaddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.albums.isEmpty() -> EmptyState(onAddRequested)
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.albums, key = { it.id }) { album ->
                        AlbumRow(
                            album = album,
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
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = album.displayName, style = MaterialTheme.typography.titleMedium)
        Text(
            text = album.rootPath,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        Button(onClick = onAddRequested) { Text(text = "+ ${Strings.ALBUMS_ADD}") }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.path,
                        onValueChange = { onChange(AlbumAddFormReducer.updatePath(form, it)) },
                        label = { Text(Strings.ALBUMS_PATH_LABEL) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onBrowsePathRequested) { Text(Strings.ALBUMS_BROWSE) }
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
