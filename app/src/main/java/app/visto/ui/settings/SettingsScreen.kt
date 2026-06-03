package app.visto.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onClearCache: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
            )
        },
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Account", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(text = state.accountDisplayName)
            Text(text = state.accountBaseUrl)
            Text(text = "Root: ${state.accountRoot}")

            HorizontalDivider()

            Text(text = "Local cache", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(text = "Thumbnails on disk: ${formatBytes(state.thumbnailCacheBytes)}")
            Button(
                onClick = onClearCache,
                enabled = !state.isClearingCache,
            ) {
                Text(text = if (state.isClearingCache) "Clearing…" else "Clear local thumbnails")
            }
            state.message?.let { Text(text = it) }
        }
    }
}

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
