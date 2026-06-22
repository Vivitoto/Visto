package app.visto.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class HomeTab { ALBUMS, BOOKSHELF, BROWSER, SETTINGS }

@Composable
fun VistoBottomBar(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        NavigationBarItem(
            selected = selected == HomeTab.ALBUMS,
            onClick = { onSelect(HomeTab.ALBUMS) },
            icon = { Icon(Icons.Filled.PhotoAlbum, contentDescription = Strings.ALBUMS_TITLE) },
            label = { Text(Strings.ALBUMS_TITLE) },
            colors = itemColors(),
        )
        NavigationBarItem(
            selected = selected == HomeTab.BOOKSHELF,
            onClick = { onSelect(HomeTab.BOOKSHELF) },
            icon = { Icon(Icons.Filled.Book, contentDescription = Strings.BOOKSHELF_TITLE) },
            label = { Text(Strings.BOOKSHELF_TITLE) },
            colors = itemColors(),
        )
        NavigationBarItem(
            selected = selected == HomeTab.BROWSER,
            onClick = { onSelect(HomeTab.BROWSER) },
            icon = { Icon(Icons.Filled.Folder, contentDescription = Strings.SETTINGS_BROWSE_MODE_DIR) },
            label = { Text(Strings.BROWSER_TITLE) },
            colors = itemColors(),
        )
        NavigationBarItem(
            selected = selected == HomeTab.SETTINGS,
            onClick = { onSelect(HomeTab.SETTINGS) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = Strings.SETTINGS_TITLE) },
            label = { Text(Strings.SETTINGS_TITLE) },
            colors = itemColors(),
        )
    }
}

@Composable
private fun itemColors(): NavigationBarItemColors {
    val cs = MaterialTheme.colorScheme
    return NavigationBarItemDefaults.colors(
        selectedIconColor = cs.onPrimary,
        selectedTextColor = cs.primary,
        indicatorColor = cs.primary,
        unselectedIconColor = cs.onSurfaceVariant,
        unselectedTextColor = cs.onSurfaceVariant,
    )
}
