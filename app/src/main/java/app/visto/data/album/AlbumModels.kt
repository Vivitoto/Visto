package app.visto.data.album

import app.visto.core.media.MediaType
import app.visto.core.model.DavPath
import app.visto.core.model.RemoteEntry

/**
 * One contiguous group inside an album: all the media found under one
 * original subfolder of the album's root.
 *
 * [title] is the path relative to the album root (e.g. `2024/Trip`), or the
 * empty string for files that live directly in the album root.
 */
data class AlbumSection(
    val title: String,
    val parentPath: String,
    val media: List<RemoteEntry>,
)

data class AlbumContents(
    val rootPath: String,
    val sections: List<AlbumSection>,
    val totalMedia: Int,
    val foldersVisited: Int,
    val foldersFailed: Int,
    val warnings: List<String>,
)

/**
 * Computes the relative title used for a section header.
 *
 * Pure so it is easy to unit-test the path arithmetic without spinning up
 * a WebDAV server.
 */
object AlbumSectionTitle {
    fun forParent(rootPath: String, parentPath: String): String {
        val root = DavPath.normalize(rootPath).trimEnd('/').ifEmpty { "/" }
        val parent = DavPath.normalize(parentPath).trimEnd('/').ifEmpty { "/" }
        if (parent == root) return ""
        if (root == "/") return parent.trimStart('/')
        val prefix = "$root/"
        return if (parent.startsWith(prefix)) parent.removePrefix(prefix) else parent.trimStart('/')
    }
}

/**
 * Pure grouping/ordering routine: given the media files found anywhere under
 * [rootPath], split them into sections by their parent folder and order both
 * sections and intra-section files deterministically.
 */
object AlbumGrouper {
    fun group(rootPath: String, mediaFiles: Collection<RemoteEntry>): List<AlbumSection> {
        val byParent = mediaFiles
            .filter { !it.isDirectory && it.mediaType in VIEWER_MEDIA }
            .groupBy { it.parentPath ?: "/" }
        val sections = byParent.map { (parent, files) ->
            AlbumSection(
                title = AlbumSectionTitle.forParent(rootPath, parent),
                parentPath = parent,
                media = files.sortedBy { it.name.lowercase() },
            )
        }
        return sections.sortedWith(compareBy({ it.title.isEmpty().not() }, { it.title.lowercase() }))
    }

    private val VIEWER_MEDIA = setOf(MediaType.IMAGE, MediaType.ANIMATED_IMAGE, MediaType.VIDEO)
}
