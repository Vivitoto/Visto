package app.visto.ui.browser

import app.visto.core.model.RemoteEntry
import app.visto.core.sort.DirectorySorter
import app.visto.core.sort.SortMode

/**
 * Pure helper that turns a flat list of directory entries into a partitioned
 * + sorted [BrowserUiState] update.
 */
object BrowserStateBuilder {

    fun apply(
        currentPath: String,
        entries: List<RemoteEntry>,
        sortMode: SortMode,
        isLoading: Boolean = false,
        isRefreshing: Boolean = false,
        errorMessage: String? = null,
    ): BrowserUiState {
        val sorted = DirectorySorter.sort(entries, sortMode)
        val folders = sorted.filter { it.isDirectory }
        val media = sorted.filter { !it.isDirectory }
        return BrowserUiState(
            currentPath = currentPath,
            folders = folders,
            media = media,
            sortMode = sortMode,
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            errorMessage = errorMessage,
        )
    }
}
