package app.visto.ui.viewer

import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry

/**
 * Viewer state. The viewer only walks media entries in the current
 * directory; folders and unsupported files are excluded.
 */
data class ViewerSession(
    val items: List<RemoteEntry>,
    val initialIndex: Int,
) {
    init {
        if (items.isNotEmpty()) {
            require(initialIndex in items.indices) {
                "initialIndex out of bounds: $initialIndex / ${items.size}"
            }
        }
    }

    companion object {
        fun build(allEntries: List<RemoteEntry>, openedPath: String): ViewerSession {
            val media = allEntries.filter {
                !it.isDirectory && it.mediaType in VIEWER_TYPES
            }
            val idx = media.indexOfFirst { it.path == openedPath }.coerceAtLeast(0)
            return ViewerSession(items = media, initialIndex = idx)
        }

        val VIEWER_TYPES = setOf(MediaType.IMAGE, MediaType.ANIMATED_IMAGE, MediaType.VIDEO)
    }
}
