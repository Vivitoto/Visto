package app.visto.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.visto.ui.Strings
import app.visto.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onClearCache: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = Strings.SETTINGS_TITLE) })
        },
        bottomBar = bottomBar,
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SectionLabel(Strings.SETTINGS_ACCOUNT)
            AccountCard(state)

            ThemeSection(state.themeMode, onThemeModeChange)

            CacheSection(state, onClearCache)

            SectionLabel("关于")
            AboutCard()
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun AccountCard(state: SettingsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 14.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = state.accountDisplayName, style = MaterialTheme.typography.titleMedium)
                Text(text = state.accountBaseUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "${Strings.SETTINGS_ROOT_LABEL}${state.accountRoot}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ThemeSection(current: ThemeMode, onChange: (ThemeMode) -> Unit) {
    SectionLabel(Strings.SETTINGS_TITLE)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            RailsRadio(
                label = "跟随系统",
                selected = current == ThemeMode.SYSTEM,
                onClick = { onChange(ThemeMode.SYSTEM) },
            )
            RailsRadio(
                label = "浅色模式",
                selected = current == ThemeMode.LIGHT,
                onClick = { onChange(ThemeMode.LIGHT) },
            )
            RailsRadio(
                label = "深色模式",
                selected = current == ThemeMode.DARK,
                onClick = { onChange(ThemeMode.DARK) },
            )
        }
    }
}

@Composable
private fun RailsRadio(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun CacheSection(state: SettingsUiState, onClearCache: () -> Unit) {
    SectionLabel(Strings.SETTINGS_LOCAL_CACHE)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = Strings.thumbnailsOnDisk(formatBytes(state.thumbnailCacheBytes)))
            state.message?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onClearCache,
                enabled = !state.isClearingCache,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(text = if (state.isClearingCache) Strings.SETTINGS_CLEARING else Strings.SETTINGS_CLEAR_THUMBNAILS)
            }
        }
    }
}

@Composable
private fun AboutCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Visto · 调试构建", modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
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