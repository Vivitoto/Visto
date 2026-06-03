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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.visto.ui.Strings

enum class BrowseMode { ALBUMS, DIRECTORY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onClearCache: () -> Unit,
    browseMode: BrowseMode = BrowseMode.ALBUMS,
    onBrowseModeChange: (BrowseMode) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = Strings.SETTINGS_TITLE) },
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
            Text(text = Strings.SETTINGS_ACCOUNT, style = MaterialTheme.typography.titleMedium)
            Text(text = state.accountDisplayName)
            Text(text = state.accountBaseUrl)
            Text(text = "${Strings.SETTINGS_ROOT_LABEL}${state.accountRoot}")

            HorizontalDivider()

            Text(text = Strings.SETTINGS_BROWSE_MODE, style = MaterialTheme.typography.titleMedium)
            BrowseModeRow(
                label = Strings.SETTINGS_BROWSE_MODE_ALBUMS,
                selected = browseMode == BrowseMode.ALBUMS,
                onClick = { onBrowseModeChange(BrowseMode.ALBUMS) },
            )
            BrowseModeRow(
                label = Strings.SETTINGS_BROWSE_MODE_DIR,
                selected = browseMode == BrowseMode.DIRECTORY,
                onClick = { onBrowseModeChange(BrowseMode.DIRECTORY) },
            )

            HorizontalDivider()

            Text(text = Strings.SETTINGS_LOCAL_CACHE, style = MaterialTheme.typography.titleMedium)
            Text(text = Strings.thumbnailsOnDisk(formatBytes(state.thumbnailCacheBytes)))
            Button(
                onClick = onClearCache,
                enabled = !state.isClearingCache,
            ) {
                Text(text = if (state.isClearingCache) Strings.SETTINGS_CLEARING else Strings.SETTINGS_CLEAR_THUMBNAILS)
            }
            state.message?.let { Text(text = it) }
        }
    }
}

@Composable
private fun BrowseModeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
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
