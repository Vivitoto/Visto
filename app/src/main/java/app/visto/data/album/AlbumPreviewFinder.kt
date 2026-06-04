package app.visto.data.album

import app.visto.core.media.MediaType
import app.visto.core.model.DavPath
import app.visto.core.model.RemoteEntry
import app.visto.data.webdav.WebDavClient
import kotlinx.coroutines.CancellationException

/**
 * Collects a short list of representative image paths for an album to use
 * as a mosaic preview on the album card.
 *
 * Strategy:
 *  - PROPFIND the album root.
 *  - Take direct image children first (sorted by name for stability).
 *  - If we still need more, fan out into at most [maxSubdirsToProbe]
 *    subdirectories (alphabetical) and pull images from each.
 *  - Stop as soon as we collect [targetCount] paths or run out of probes.
 *
 * The probe is intentionally shallow: an album with hundreds of nested
 * folders should not cost hundreds of PROPFIND calls just to fill four
 * mosaic tiles on the home screen.
 */
class AlbumPreviewFinder(
    private val client: WebDavClient,
    private val maxSubdirsToProbe: Int = 6,
) {

    suspend fun findPreviewImages(rootPath: String, targetCount: Int): List<RemoteEntry> {
        if (targetCount <= 0) return emptyList()
        val normalizedRoot = DavPath.normalize(rootPath)
        val rootEntries = listOrNull(normalizedRoot) ?: return emptyList()
        val picks = mutableListOf<RemoteEntry>()
        val seenPaths = HashSet<String>()

        fun collectFrom(entries: List<RemoteEntry>) {
            entries
                .asSequence()
                .filter { !it.isDirectory && it.mediaType in IMAGE_TYPES }
                .sortedWith(
                    compareBy(
                        { it.sizeBytes ?: Long.MAX_VALUE },
                        { it.name.lowercase() },
                    )
                )
                .forEach { entry ->
                    if (picks.size >= targetCount) return
                    if (seenPaths.add(entry.path)) picks += entry
                }
        }

        collectFrom(rootEntries)
        if (picks.size >= targetCount) return picks

        val subdirs = rootEntries
            .filter { it.isDirectory }
            .sortedBy { it.name.lowercase() }
            .take(maxSubdirsToProbe)

        for (subdir in subdirs) {
            if (picks.size >= targetCount) break
            val children = listOrNull(subdir.path) ?: continue
            collectFrom(children)
        }
        return picks
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
