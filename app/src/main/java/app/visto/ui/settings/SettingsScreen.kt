package app.visto.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.visto.AppInfo
import app.visto.data.account.GridDensity
import app.visto.data.account.ThumbnailCacheLimit
import app.visto.data.webdav.WebDavDiagnosticResult
import app.visto.data.webdav.WebDavDiagnosticStatus
import app.visto.ui.Strings
import app.visto.ui.theme.ThemeMode
import kotlin.math.roundToInt

private val SettingsPageHorizontalPadding = 16.dp
private val SettingsPageTopPadding = 14.dp
private val SettingsPageBottomPadding = 12.dp
private val SettingsSectionGap = 12.dp
private val SettingsTitleGap = 4.dp
private val SettingsCardPadding = 10.dp
private val SettingsItemGap = 8.dp
private val SettingsDetailGap = 6.dp
private val SettingsRadioRowVerticalPadding = 2.dp
private val SettingsSliderThumbSize = 22.dp
private val SettingsSliderThumbRadius = 11.dp

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onClearCache: () -> Unit,
    onClearBookCache: () -> Unit,
    onCacheLimitChange: (ThumbnailCacheLimit) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAutoLoadOriginalImagesChange: (Boolean) -> Unit,
    onBlurThumbnailsChange: (Boolean) -> Unit,
    onGridDensityChange: (GridDensity) -> Unit,
    onAddServer: () -> Unit,
    onSwitchAccount: (Long) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onTestActiveConnection: () -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenReleasePage: () -> Unit,
    onDismissUpdateMessage: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    Scaffold(
        bottomBar = bottomBar,
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(
                    start = SettingsPageHorizontalPadding,
                    top = SettingsPageTopPadding,
                    end = SettingsPageHorizontalPadding,
                    bottom = SettingsPageBottomPadding,
                )
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SettingsSectionGap),
        ) {
            SettingsSection(Strings.SETTINGS_ACCOUNT) {
                AccountsSection(
                    state = state,
                    onAddServer = onAddServer,
                    onSwitchAccount = onSwitchAccount,
                    onDeleteAccount = onDeleteAccount,
                    onTestActiveConnection = onTestActiveConnection,
                )
            }

            SettingsSection(Strings.SETTINGS_APPEARANCE) {
                ThemeSection(state.themeMode, onThemeModeChange)
            }

            SettingsSection(Strings.SETTINGS_VIEWER) {
                ViewerSection(
                    autoLoadOriginalImages = state.autoLoadOriginalImages,
                    blurThumbnails = state.blurThumbnails,
                    gridDensity = state.gridDensity,
                    onAutoLoadOriginalImagesChange = onAutoLoadOriginalImagesChange,
                    onBlurThumbnailsChange = onBlurThumbnailsChange,
                    onGridDensityChange = onGridDensityChange,
                )
            }

            SettingsSection(Strings.SETTINGS_LOCAL_CACHE) {
                CacheSection(state, onClearCache, onClearBookCache, onCacheLimitChange)
            }

            SettingsSection(Strings.SETTINGS_ABOUT_TITLE) {
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
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SettingsTitleGap)) {
        SectionLabel(title)
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = SettingsTitleGap),
    )
}

@Composable
private fun AccountsSection(
    state: SettingsUiState,
    onAddServer: () -> Unit,
    onSwitchAccount: (Long) -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onTestActiveConnection: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(SettingsCardPadding).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsItemGap),
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
                        horizontalArrangement = Arrangement.spacedBy(SettingsItemGap),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (active) "${account.displayName} · ${Strings.SETTINGS_ACTIVE_SERVER}" else account.displayName,
                                style = MaterialTheme.typography.bodyMedium,
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
            if (state.activeAccountId != null) {
                OutlinedButton(
                    onClick = onTestActiveConnection,
                    enabled = !state.isTestingConnection,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isTestingConnection) Strings.ACCOUNT_TESTING else Strings.ACCOUNT_TEST_CONNECTION)
                }
            }
            state.diagnostic?.let { DiagnosticResultCard(it) }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(SettingsCardPadding)) {
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
private fun DiagnosticResultCard(result: WebDavDiagnosticResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(SettingsCardPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsDetailGap),
        ) {
            Text(
                text = result.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = if (result.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            result.steps.forEach { step ->
                val marker = if (step.status == WebDavDiagnosticStatus.PASS) "✓" else "!"
                val color = if (step.status == WebDavDiagnosticStatus.PASS) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SettingsItemGap),
                ) {
                    Text(text = marker, color = color)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = step.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = step.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RailsRadio(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = SettingsRadioRowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = SettingsItemGap))
    }
}

@Composable
private fun ViewerSection(
    autoLoadOriginalImages: Boolean,
    blurThumbnails: Boolean,
    gridDensity: GridDensity,
    onAutoLoadOriginalImagesChange: (Boolean) -> Unit,
    onBlurThumbnailsChange: (Boolean) -> Unit,
    onGridDensityChange: (GridDensity) -> Unit,
) {
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
            GridDensityRow(
                current = gridDensity,
                onChange = onGridDensityChange,
            )
        }
    }
}

@Composable
private fun GridDensityRow(current: GridDensity, onChange: (GridDensity) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsCardPadding, vertical = SettingsItemGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = Strings.SETTINGS_GRID_DENSITY, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = Strings.SETTINGS_GRID_DENSITY_DESC,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = current.displayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(
            modifier = Modifier.padding(top = SettingsItemGap),
            horizontalArrangement = Arrangement.spacedBy(SettingsItemGap),
        ) {
            GridDensity.entries.forEach { density ->
                if (density == current) {
                    Button(
                        onClick = { onChange(density) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(density.displayLabel)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onChange(density) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(density.displayLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = SettingsCardPadding, vertical = SettingsItemGap),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SettingsItemGap),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CacheSection(
    state: SettingsUiState,
    onClearCache: () -> Unit,
    onClearBookCache: () -> Unit,
    onCacheLimitChange: (ThumbnailCacheLimit) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(SettingsCardPadding)) {
            Text(text = Strings.thumbnailsOnDisk(formatBytes(state.thumbnailCacheBytes)), style = MaterialTheme.typography.bodyMedium)

            val limits = ThumbnailCacheLimit.entries
            Row(
                modifier = Modifier.padding(top = SettingsItemGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Strings.SETTINGS_CACHE_LIMIT_LABEL,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = state.thumbnailCacheLimit.displayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier.padding(top = SettingsDetailGap),
                verticalArrangement = Arrangement.spacedBy(SettingsDetailGap),
            ) {
                val selectedIndex = limits.indexOf(state.thumbnailCacheLimit).coerceAtLeast(0)
                Slider(
                    value = selectedIndex.toFloat(),
                    onValueChange = { value ->
                        val index = value.roundToInt().coerceIn(0, limits.lastIndex)
                        onCacheLimitChange(limits[index])
                    },
                    valueRange = 0f..limits.lastIndex.toFloat(),
                    steps = (limits.size - 2).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(SettingsSliderThumbSize)
                                .clip(RoundedCornerShape(SettingsSliderThumbRadius))
                                .background(MaterialTheme.colorScheme.primary)
                                .border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(SettingsSliderThumbRadius),
                                ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    limits.forEachIndexed { index, limit ->
                        Text(
                            text = limit.displayLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (index == selectedIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
            state.message?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onClearCache,
                enabled = !state.isClearingCache,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SettingsItemGap),
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.padding(end = SettingsItemGap))
                Text(text = if (state.isClearingCache) Strings.SETTINGS_CLEARING else Strings.SETTINGS_CLEAR_THUMBNAILS)
            }
            OutlinedButton(
                onClick = onClearBookCache,
                enabled = !state.isClearingCache,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.padding(end = SettingsItemGap))
                Text(text = if (state.isClearingCache) Strings.SETTINGS_CLEARING else "清理书籍缓存")
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
            modifier = Modifier.padding(SettingsCardPadding).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsItemGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SettingsItemGap),
            ) {
                Text(
                    text = "v${AppInfo.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
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
                Column(verticalArrangement = Arrangement.spacedBy(SettingsTitleGap)) {
                    Text(
                        text = "${Strings.SETTINGS_NEW_VERSION}：v${info.latestVersion}",
                        style = MaterialTheme.typography.bodyMedium,
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
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = SettingsTitleGap),
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
                    horizontalArrangement = Arrangement.spacedBy(SettingsItemGap),
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
