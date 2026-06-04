package app.visto.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.visto.AppInfo
import app.visto.data.account.ThumbnailCacheLimit
import app.visto.ui.Strings
import app.visto.ui.theme.ThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onClearCache: () -> Unit,
    onCacheLimitChange: (ThumbnailCacheLimit) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAutoLoadOriginalImagesChange: (Boolean) -> Unit,
    onBlurThumbnailsChange: (Boolean) -> Unit,
    onAddServer: () -> Unit,
    onSwitchAccount: (Long) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onDismissUpdateMessage: () -> Unit,
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
            AccountsSection(
                state = state,
                onAddServer = onAddServer,
                onSwitchAccount = onSwitchAccount,
                onDeleteAccount = onDeleteAccount,
            )

            ThemeSection(state.themeMode, onThemeModeChange)

            ViewerSection(
                autoLoadOriginalImages = state.autoLoadOriginalImages,
                blurThumbnails = state.blurThumbnails,
                onAutoLoadOriginalImagesChange = onAutoLoadOriginalImagesChange,
                onBlurThumbnailsChange = onBlurThumbnailsChange,
            )

            CacheSection(state, onClearCache, onCacheLimitChange)

            SectionLabel(Strings.SETTINGS_ABOUT_TITLE)
            AboutCard(
                update = state.update,
                onCheckUpdate = onCheckUpdate,
                onDownloadUpdate = onDownloadUpdate,
                onInstallUpdate = onInstallUpdate,
                onOpenReleasePage = onOpenReleasePage,
                onDismissUpdateMessage = onDismissUpdateMessage,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun AccountsSection(
    state: SettingsUiState,
    onAddServer: () -> Unit,
    onSwitchAccount: (Long) -> Unit,
    onDeleteAccount: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.accounts.isEmpty()) {
                Text(
                    text = Strings.SETTINGS_NO_SERVER,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.accounts.forEach { account ->
                    val active = account.id == state.activeAccountId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (active) "${account.displayName} · ${Strings.SETTINGS_ACTIVE_SERVER}" else account.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                            )
                            Text(
                                text = account.baseUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Text(
                                text = "${Strings.SETTINGS_ROOT_LABEL}${account.rootPath}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        if (!active) {
                            Button(onClick = { onSwitchAccount(account.id) }) {
                                Text(Strings.SETTINGS_SET_ACTIVE_SERVER)
                            }
                        }
                        Button(onClick = { onDeleteAccount(account.id) }) {
                            Text(Strings.ALBUMS_DELETE)
                        }
                    }
                }
            }
            Button(
                onClick = onAddServer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(Strings.SETTINGS_ADD_SERVER)
            }
        }
    }
}

@Composable
private fun ThemeSection(current: ThemeMode, onChange: (ThemeMode) -> Unit) {
    SectionLabel(Strings.SETTINGS_APPEARANCE)
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
private fun ViewerSection(
    autoLoadOriginalImages: Boolean,
    blurThumbnails: Boolean,
    onAutoLoadOriginalImagesChange: (Boolean) -> Unit,
    onBlurThumbnailsChange: (Boolean) -> Unit,
) {
    SectionLabel(Strings.SETTINGS_VIEWER)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SwitchRow(
                title = Strings.SETTINGS_AUTO_LOAD_ORIGINAL,
                description = Strings.SETTINGS_AUTO_LOAD_ORIGINAL_DESC,
                checked = autoLoadOriginalImages,
                onChange = onAutoLoadOriginalImagesChange,
            )
            SwitchRow(
                title = Strings.SETTINGS_BLUR_THUMBNAILS,
                description = Strings.SETTINGS_BLUR_THUMBNAILS_DESC,
                checked = blurThumbnails,
                onChange = onBlurThumbnailsChange,
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
        )
    }
}

@Composable
private fun CacheSection(state: SettingsUiState, onClearCache: () -> Unit, onCacheLimitChange: (ThumbnailCacheLimit) -> Unit) {
    SectionLabel(Strings.SETTINGS_LOCAL_CACHE)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = Strings.thumbnailsOnDisk(formatBytes(state.thumbnailCacheBytes)))

            val limits = ThumbnailCacheLimit.entries
            val selectedIndex = limits.indexOf(state.thumbnailCacheLimit).coerceAtLeast(0)
            Row(
                modifier = Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Strings.SETTINGS_CACHE_LIMIT_LABEL,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = state.thumbnailCacheLimit.displayLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = selectedIndex.toFloat(),
                onValueChange = { value ->
                    onCacheLimitChange(limits[value.roundToInt().coerceIn(0, limits.lastIndex)])
                },
                valueRange = 0f..limits.lastIndex.toFloat(),
                steps = (limits.size - 2).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth(),
            )
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
private fun AboutCard(
    update: UpdateUiState,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onDismissUpdateMessage: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "v${AppInfo.VERSION_NAME}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onCheckUpdate,
                    enabled = !update.isChecking && !update.isDownloading,
                ) {
                    Text(text = if (update.isChecking) Strings.SETTINGS_CHECKING_UPDATE else Strings.SETTINGS_CHECK_UPDATE)
                }
            }

            val info = update.info
            if (info != null && info.hasUpdate) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${Strings.SETTINGS_NEW_VERSION}：v${info.latestVersion}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    val sizeText = info.apkSize?.let { " · ${formatBytes(it)}" } ?: ""
                    Text(
                        text = "${info.apkName}$sizeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (info.releaseNotes.isNotBlank()) {
                        Text(
                            text = Strings.SETTINGS_RELEASE_NOTES,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 10,
                        )
                    }
                }
            } else if (info != null && !info.hasUpdate) {
                Text(
                    text = Strings.SETTINGS_LATEST_VERSION_ALREADY,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (update.isDownloading) {
                val total = update.downloadTotalBytes
                val received = update.downloadedBytes
                if (total != null && total > 0) {
                    LinearProgressIndicator(
                        progress = { (received.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${formatBytes(received)} / ${formatBytes(total)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = formatBytes(received),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            update.errorMessage?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismissUpdateMessage) {
                        Text(text = "知道了")
                    }
                }
            }
            update.infoMessage?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismissUpdateMessage) {
                        Text(text = "知道了")
                    }
                }
            }

            if (info != null && info.hasUpdate) {
                val downloaded = update.downloaded
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (downloaded != null) {
                        Button(
                            onClick = onInstallUpdate,
                            modifier = Modifier.weight(1f),
                        ) { Text(text = Strings.SETTINGS_INSTALL_NOW) }
                    } else {
                        Button(
                            onClick = onDownloadUpdate,
                            enabled = !update.isDownloading,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = if (update.isDownloading) Strings.SETTINGS_DOWNLOADING else Strings.SETTINGS_DOWNLOAD_AND_INSTALL)
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenReleasePage,
                        modifier = Modifier.weight(1f),
                    ) { Text(text = Strings.SETTINGS_OPEN_RELEASE_PAGE) }
                }
            }
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