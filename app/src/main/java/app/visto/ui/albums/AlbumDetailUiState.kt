package app.visto.ui.albums

import app.visto.core.model.RemoteEntry
import app.visto.data.album.AlbumContents

data class AlbumDetailUiState(
    val title: String,
    val rootPath: String,
    val sections: List<AlbumDetailSection> = emptyList(),
    val isLoading: Boolean = true,
    val foldersVisited: Int = 0,
    val foldersFailed: Int = 0,
    val warnings: List<String> = emptyList(),
    val errorMessage: String? = null,
) {
    val totalMedia: Int get() = sections.sumOf { it.media.size }
    val flatMedia: List<RemoteEntry> get() = sections.flatMap { it.media }
    val isEmpty: Boolean get() = !isLoading && sections.isEmpty() && errorMessage == null
}

data class AlbumDetailSection(
    val title: String,
    val parentPath: String,
    val media: List<RemoteEntry>,
)

object AlbumDetailReducer {

    fun fromContents(title: String, contents: AlbumContents, loading: Boolean): AlbumDetailUiState {
        val sections = contents.sections.map {
            AlbumDetailSection(title = it.title, parentPath = it.parentPath, media = it.media)
        }
        return AlbumDetailUiState(
            title = title,
            rootPath = contents.rootPath,
            sections = sections,
            isLoading = loading,
            foldersVisited = contents.foldersVisited,
            foldersFailed = contents.foldersFailed,
            warnings = contents.warnings,
        )
    }
}
