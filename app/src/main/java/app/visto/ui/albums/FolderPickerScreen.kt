package app.visto.ui.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.visto.core.model.RemoteEntry
import app.visto.ui.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerScreen(
    state: FolderPickerState,
    onBack: () -> Unit,
    onGoUp: () -> Unit,
    onOpenFolder: (RemoteEntry) -> Unit,
    onSelectCurrent: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.FOLDER_PICKER_TITLE) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "返回") } },
            )
        },
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Text(
                text = state.currentPath,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Button(
                onClick = onSelectCurrent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(Strings.FOLDER_PICKER_SELECT_CURRENT)
            }
            if (state.canGoUp) {
                OutlinedButton(
                    onClick = onGoUp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(Strings.FOLDER_PICKER_UP)
                }
            }
            state.errorMessage?.let {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = it)
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                        Text(Strings.RETRY)
                    }
                }
            }
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            } else if (state.folders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(Strings.FOLDER_PICKER_EMPTY) }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.folders, key = { it.path }) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenFolder(folder) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = folder.name,
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                            )
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
