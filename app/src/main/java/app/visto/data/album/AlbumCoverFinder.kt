package app.visto.data.album

import app.visto.core.media.MediaType
import app.visto.core.model.DavPath
import app.visto.core.model.RemoteEntry
import app.visto.data.webdav.WebDavClient
import kotlinx.coroutines.CancellationException

/**
 * Finds a representative cover image for an album by doing a shallow
 * directory probe.
 *
 * Strategy:
 *  - List the album root.
 *  - If any image entry exists at the root, return the first one (sorted
 *    by name for determinism).
 *  - Otherwise, list at most [maxSubdirsToProbe] of the root's
 *    subdirectories and return the first image found inside any of them.
 *  - Returns null if no image is found within the probe budget.
 *
 * This intentionally does NOT walk the whole tree: an album with hundreds
 * of nested folders shouldn't cost hundreds of PROPFIND calls just to
 * surface a cover on the home screen.
 */
class AlbumCoverFinder(
    private val client: WebDavClient,
    private val maxSubdirsToProbe: Int = 4,
    private val maxCoverBytes: Long = 8L * 1024 * 1024,
) {

    /**
     * Returns the path of a cover image for [rootPath], or null if the
     * shallow probe finds nothing.
     *
     * Within each probed directory we prefer the smallest image we can find,
     * so that big originals are not pulled over the network just to fill a
     * 56dp thumbnail. If sizes are unknown we fall back to alphabetical
     * order for determinism.
     */
    suspend fun findCoverImage(rootPath: String): RemoteEntry? {
        val normalizedRoot = DavPath.normalize(rootPath)
        val rootEntries = listOrNull(normalizedRoot) ?: return null

        val rootImage = pickBestImage(rootEntries)
        if (rootImage != null) return rootImage

        val subdirs = rootEntries
            .filter { it.isDirectory }
            .sortedBy { it.name.lowercase() }
            .take(maxSubdirsToProbe)

        for (subdir in subdirs) {
            val children = listOrNull(subdir.path) ?: continue
            val childImage = pickBestImage(children)
            if (childImage != null) return childImage
        }
        return null
    }

    private fun pickBestImage(entries: List<RemoteEntry>): RemoteEntry? {
        val images = entries.filter {
            !it.isDirectory &&
                it.mediaType in IMAGE_TYPES &&
                (it.sizeBytes == null || it.sizeBytes <= maxCoverBytes)
        }
        if (images.isEmpty()) return null
        return images.minWithOrNull(
            compareBy(
                { it.sizeBytes ?: Long.MAX_VALUE },
                { it.name.lowercase() },
            )
        )
    }

    private suspend fun listOrNull(path: String): List<RemoteEntry>? = try {
        client.listDirectory(path)
    } catch (ce: CancellationException) {
        throw ce
    } catch (_: Throwable) {
        null
    }

    companion object {
        private val IMAGE_TYPES = setOf(MediaType.IMAGE, MediaType.ANIMATED_IMAGE)
    }
}
