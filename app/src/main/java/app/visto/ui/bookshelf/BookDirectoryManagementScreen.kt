package app.visto.ui.bookshelf

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.visto.data.db.BookSourceEntity
import app.visto.ui.Strings
import app.visto.ui.layout.VistoLayoutMetrics
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class BookDirectoryManagementUiState(
    val sources: List<BookSourceEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val scanningSourceId: Long? = null,
    val message: String? = null,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDirectoryManagementScreen(
    state: BookDirectoryManagementUiState,
    onBack: () -> Unit,
    onAddSource: () -> Unit,
    onRescanSource: (BookSourceEntity) -> Unit,
    onDeleteSource: (BookSourceEntity) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<BookSourceEntity?>(null) }
    var statusOverlayHeight by remember { mutableStateOf(0.dp) }
    var fabHeight by remember { mutableStateOf(VistoLayoutMetrics.DefaultFloatingActionButtonHeight) }
    val density = LocalDensity.current

    LaunchedEffect(state.errorMessage, state.message) {
        if (state.errorMessage == null && state.message == null) {
            statusOverlayHeight = 0.dp
        }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.BOOK_SOURCE_TITLE) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = Strings.BACK)
                    }
                },
            )
        },
        floatingActionButton = {
            if (!state.isLoading && !state.isScanning) {
                FloatingActionButton(
                    onClick = onAddSource,
                    modifier = Modifier.onSizeChanged { size ->
                        with(density) { fabHeight = size.height.toDp() }
                    },
                    content = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = Strings.BOOK_SOURCE_ADD,
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        val bottomContentPadding = VistoLayoutMetrics.scrollEndPadding(
            bottomOverlayHeight = statusOverlayHeight,
            floatingActionButtonHeight = fabHeight,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.sources.isEmpty() -> BookSourceEmptyState(
                    onAddSource = onAddSource,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, top = 12.dp, end = 14.dp, bottom = bottomContentPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.sources, key = { it.id }) { source ->
                        BookSourceCard(
                            source = source,
                            isScanning = state.isScanning && state.scanningSourceId == source.id,
                            actionsEnabled = !state.isScanning,
                            onRescan = { onRescanSource(source) },
                            onDelete = { pendingDelete = source },
                        )
                    }
                }
            }

            BookSourceStatusMessages(
                errorMessage = state.errorMessage,
                message = state.message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .onSizeChanged { size ->
                        with(density) { statusOverlayHeight = size.height.toDp() }
                    },
            )
        }
    }

    val deleteSource = pendingDelete
    if (deleteSource != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(Strings.BOOK_SOURCE_DELETE_TITLE) },
            text = { Text(Strings.BOOK_SOURCE_DELETE_MESSAGE) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDeleteSource(deleteSource)
                    },
                ) { Text(Strings.BOOK_SOURCE_DELETE_CONFIRM, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(Strings.ALBUMS_CANCEL) }
            },
        )
    }
}

@Composable
private fun BookSourceEmptyState(
    onAddSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(46.dp),
        )
        Text(
            text = Strings.BOOK_SOURCE_EMPTY_TITLE,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = Strings.BOOK_SOURCE_EMPTY_SUBTITLE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onAddSource,
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(Strings.BOOK_SOURCE_ADD)
        }
    }
}

@Composable
private fun BookSourceCard(
    source: BookSourceEntity,
    isScanning: Boolean,
    actionsEnabled: Boolean,
    onRescan: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = source.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = source.rootPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = if (isScanning) Strings.BOOK_SOURCE_SCANNING else bookSourceScanSummary(source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onRescan,
                    enabled = actionsEnabled,
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(Strings.BOOK_SOURCE_RESCAN, modifier = Modifier.padding(start = 6.dp))
                }
                TextButton(
                    onClick = onDelete,
                    enabled = actionsEnabled,
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = Strings.BOOK_SOURCE_DELETE,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BookSourceStatusMessages(
    errorMessage: String?,
    message: String?,
    modifier: Modifier = Modifier,
) {
    if (errorMessage == null && message == null) return
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun bookSourceScanSummary(source: BookSourceEntity): String {
    val scannedAt = source.lastScannedAt ?: return Strings.BOOK_SOURCE_LAST_SCAN_NEVER
    val formattedTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(scannedAt))
    return Strings.bookSourceLastScan(
        formattedTime = formattedTime,
        imported = source.lastImportedCount,
        updated = source.lastUpdatedCount,
        foldersVisited = source.lastFoldersVisited,
        foldersFailed = source.lastFoldersFailed,
    )
}
