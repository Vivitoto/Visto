package app.visto.ui.albums

import app.visto.core.model.RemoteEntry
import app.visto.core.sort.DirectorySorter
import app.visto.core.sort.SortMode
import app.visto.data.account.AlbumViewMode

/**
 * UI state for the album detail screen.
 *
 * The screen has two view modes over the same current directory:
 *  - FOLDERS: list-style folder rows followed by current-folder media.
 *  - FLAT: icon-grid layout for folders and current-folder media.
 *
 * Both modes use Depth:1 WebDAV listing only. They never recursively flatten
 * child folders into the current view, so the album behaves like a normal
 * file browser.
 */
data class AlbumDetailUiState(
    val title: String,
    val rootPath: String,
    val viewMode: AlbumViewMode = AlbumViewMode.FOLDERS,
    val folderView: AlbumFolderViewState = AlbumFolderViewState(currentPath = rootPath),
    val flatView: AlbumFlatViewState = AlbumFlatViewState(),
    val sortMode: SortMode = SortMode.DEFAULT,
    val errorMessage: String? = null,
) {
    val isLoading: Boolean
        get() = folderView.isLoading

    val isEmpty: Boolean
        get() = !folderView.isLoading &&
            folderView.folders.isEmpty() &&
            folderView.media.isEmpty() &&
            errorMessage == null

    /** Flat list of media currently visible in this folder, ordered for the viewer pager. */
    val visibleMedia: List<RemoteEntry>
        get() = folderView.media
}

/**
 * State for the navigable file-browser mode of an album.
 *
 * [currentPath] is always within (or equal to) the album root; the screen's
 * back action handles going up.
 */
data class AlbumFolderViewState(
    val currentPath: String,
    val folders: List<RemoteEntry> = emptyList(),
    val media: List<RemoteEntry> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * State for the recursive flat-grouped mode of an album.
 */
data class AlbumFlatViewState(
    val sections: List<AlbumDetailSection> = emptyList(),
    val isLoading: Boolean = false,
    val foldersVisited: Int = 0,
    val foldersFailed: Int = 0,
    val warnings: List<String> = emptyList(),
)

data class AlbumDetailSection(
    val title: String,
    val parentPath: String,
    val media: List<RemoteEntry>,
)

object AlbumDetailReducer {

    fun startFolder(
        state: AlbumDetailUiState,
        path: String,
        viewMode: AlbumViewMode = state.viewMode,
    ): AlbumDetailUiState =
        state.copy(
            viewMode = viewMode,
            folderView = AlbumFolderViewState(currentPath = path, isLoading = true),
            errorMessage = null,
        )

    fun applyFolderContents(
        state: AlbumDetailUiState,
        path: String,
        entries: List<RemoteEntry>,
    ): AlbumDetailUiState {
        val sorted = DirectorySorter.sort(
            entries.filter { it.isDirectory || it.mediaType in MEDIA_FOR_GRID },
            state.sortMode,
        )
        val folders = sorted.filter { it.isDirectory }
        val media = sorted.filter { !it.isDirectory }
        return state.copy(
            folderView = AlbumFolderViewState(
                currentPath = path,
                folders = folders,
                media = media,
                isLoading = false,
            ),
            errorMessage = null,
        )
    }

    fun applySort(state: AlbumDetailUiState, mode: SortMode): AlbumDetailUiState {
        val resorted = DirectorySorter.sort(
            state.folderView.folders + state.folderView.media,
            mode,
        )
        val folders = resorted.filter { it.isDirectory }
        val media = resorted.filter { !it.isDirectory }
        return state.copy(
            sortMode = mode,
            folderView = state.folderView.copy(folders = folders, media = media),
        )
    }

    fun applyError(state: AlbumDetailUiState, message: String): AlbumDetailUiState =
        state.copy(
            folderView = state.folderView.copy(isLoading = false),
            errorMessage = message,
        )

    private val MEDIA_FOR_GRID = setOf(
        app.visto.core.media.MediaType.IMAGE,
        app.visto.core.media.MediaType.ANIMATED_IMAGE,
        app.visto.core.media.MediaType.VIDEO,
    )
}
