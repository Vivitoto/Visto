package app.visto.ui.browser

import app.visto.core.model.RemoteEntry
import app.visto.core.sort.SortMode

/**
 * Visible state for the directory browser screen.
 *
 * Folders and media are kept separate so the UI can layer "folders first,
 * media grid below" without re-running the partition for each frame.
 */
data class BrowserUiState(
    val currentPath: String = "/",
    val folders: List<RemoteEntry> = emptyList(),
    val media: List<RemoteEntry> = emptyList(),
    val sortMode: SortMode = SortMode.DEFAULT,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = folders.isEmpty() && media.isEmpty()
}
