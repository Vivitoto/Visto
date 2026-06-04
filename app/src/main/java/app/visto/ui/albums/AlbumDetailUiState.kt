package app.visto.ui.albums

import app.visto.core.model.RemoteEntry
import app.visto.data.account.AlbumViewMode
import app.visto.data.album.AlbumContents

/**
 * UI state for the album detail screen.
 *
 * The screen has two view modes:
 *  - FOLDERS: shows the album path as a navigable file browser; current
 *    directory's subdirectories and direct media files are rendered side
 *    by side. Backed by per-folder PROPFIND calls (cheap).
 *  - FLAT: recursively walks the album and groups every media file by its
 *    parent folder. Backed by the streaming AlbumLoader (more expensive).
 *
 * Both modes share the same screen container so users can switch between
 * them without losing the album context.
 */
data class AlbumDetailUiState(
    val title: String,
    val rootPath: String,
    val viewMode: AlbumViewMode = AlbumViewMode.FOLDERS,
    val folderView: AlbumFolderViewState = AlbumFolderViewState(currentPath = rootPath),
    val flatView: AlbumFlatViewState = AlbumFlatViewState(),
    val errorMessage: String? = null,
) {
    val isLoading: Boolean
        get() = when (viewMode) {
            AlbumViewMode.FOLDERS -> folderView.isLoading
            AlbumViewMode.FLAT -> flatView.isLoading
        }

    val isEmpty: Boolean
        get() = when (viewMode) {
            AlbumViewMode.FOLDERS -> !folderView.isLoading &&
                folderView.folders.isEmpty() &&
                folderView.media.isEmpty() &&
                errorMessage == null
            AlbumViewMode.FLAT -> !flatView.isLoading &&
                flatView.sections.isEmpty() &&
                errorMessage == null
        }

    /** Flat list of all media currently visible, ordered for the viewer pager. */
    val visibleMedia: List<RemoteEntry>
        get() = when (viewMode) {
            AlbumViewMode.FOLDERS -> folderView.media
            AlbumViewMode.FLAT -> flatView.sections.flatMap { it.media }
        }
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

    fun startFlat(state: AlbumDetailUiState): AlbumDetailUiState =
        state.copy(
            viewMode = AlbumViewMode.FLAT,
            flatView = state.flatView.copy(isLoading = true),
            errorMessage = null,
        )

    fun applyFlatContents(
        state: AlbumDetailUiState,
        contents: AlbumContents,
        stillLoading: Boolean,
    ): AlbumDetailUiState {
        val sections = contents.sections.map {
            AlbumDetailSection(title = it.title, parentPath = it.parentPath, media = it.media)
        }
        return state.copy(
            viewMode = AlbumViewMode.FLAT,
            flatView = AlbumFlatViewState(
                sections = sections,
                isLoading = stillLoading,
                foldersVisited = contents.foldersVisited,
                foldersFailed = contents.foldersFailed,
                warnings = contents.warnings,
            ),
            errorMessage = null,
        )
    }

    fun startFolder(state: AlbumDetailUiState, path: String): AlbumDetailUiState =
        state.copy(
            viewMode = AlbumViewMode.FOLDERS,
            folderView = AlbumFolderViewState(currentPath = path, isLoading = true),
            errorMessage = null,
        )

    fun applyFolderContents(
        state: AlbumDetailUiState,
        path: String,
        entries: List<RemoteEntry>,
    ): AlbumDetailUiState {
        val folders = entries.filter { it.isDirectory }.sortedBy { it.name.lowercase() }
        val media = entries
            .filter { !it.isDirectory && it.mediaType in MEDIA_FOR_GRID }
            .sortedBy { it.name.lowercase() }
        return state.copy(
            viewMode = AlbumViewMode.FOLDERS,
            folderView = AlbumFolderViewState(
                currentPath = path,
                folders = folders,
                media = media,
                isLoading = false,
            ),
            errorMessage = null,
        )
    }

    fun applyError(state: AlbumDetailUiState, message: String): AlbumDetailUiState =
        when (state.viewMode) {
            AlbumViewMode.FOLDERS -> state.copy(
                folderView = state.folderView.copy(isLoading = false),
                errorMessage = message,
            )
            AlbumViewMode.FLAT -> state.copy(
                flatView = state.flatView.copy(isLoading = false),
                errorMessage = message,
            )
        }

    private val MEDIA_FOR_GRID = setOf(
        app.visto.core.media.MediaType.IMAGE,
        app.visto.core.media.MediaType.ANIMATED_IMAGE,
        app.visto.core.media.MediaType.VIDEO,
    )
}
