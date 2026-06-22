package app.visto.core.sort

import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry

/**
 * Sorts a directory listing into the visual order Visto uses on the browser screen.
 *
 * Rules:
 *  - Folders always appear before files.
 *  - Inside each group the selected [SortMode] decides the order.
 *  - Sorting is stable so equal items keep their input order.
 */
object DirectorySorter {

    fun sort(entries: List<RemoteEntry>, mode: SortMode): List<RemoteEntry> {
        val folders = entries.filter { it.isDirectory }
        val files = entries.filter { !it.isDirectory }
        val comparator = comparatorFor(mode)
        return folders.sortedWith(comparator) + files.sortedWith(comparator)
    }

    private fun comparatorFor(mode: SortMode): Comparator<RemoteEntry> {
        val nameAsc = Comparator<RemoteEntry> { a, b ->
            a.name.compareTo(b.name, ignoreCase = true)
        }
        return when (mode) {
            SortMode.NAME_ASC -> nameAsc
            SortMode.NAME_DESC -> nameAsc.reversed()
            SortMode.MODIFIED_NEWEST_FIRST -> compareBy<RemoteEntry> { -(it.lastModifiedEpochMs ?: Long.MIN_VALUE) }
                .then(nameAsc)
            SortMode.MODIFIED_OLDEST_FIRST -> compareBy<RemoteEntry> { it.lastModifiedEpochMs ?: Long.MAX_VALUE }
                .then(nameAsc)
            SortMode.SIZE_LARGEST_FIRST -> compareBy<RemoteEntry> { -(it.sizeBytes ?: -1L) }
                .then(nameAsc)
            SortMode.SIZE_SMALLEST_FIRST -> compareBy<RemoteEntry> { it.sizeBytes ?: Long.MAX_VALUE }
                .then(nameAsc)
            SortMode.TYPE -> compareBy<RemoteEntry> { typeOrder(it.mediaType) }
                .then(nameAsc)
        }
    }

    private fun typeOrder(mediaType: MediaType): Int = when (mediaType) {
        MediaType.IMAGE -> 0
        MediaType.ANIMATED_IMAGE -> 1
        MediaType.VIDEO -> 2
        MediaType.TEXT_BOOK -> 3
        MediaType.EPUB_BOOK -> 4
        MediaType.OTHER -> 5
        MediaType.UNKNOWN -> 6
    }
}
