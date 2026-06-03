package app.visto.data.thumbnail

import app.visto.data.db.ThumbnailCacheEntity

/**
 * Decides which thumbnails to evict when the cache exceeds [maxBytes].
 *
 * The policy is least-recently-accessed first, irrespective of the source
 * file size. Tests live alongside this object; actual file deletion is the
 * caller's responsibility so unit tests stay JVM-only.
 */
object ThumbnailLruPolicy {

    /**
     * Choose entries to delete so that the remaining ready cache is at or
     * below [maxBytes]. Items are evicted in [readyByOldestAccess] order.
     *
     * @param readyByOldestAccess thumbnails ordered from least to most
     *  recently accessed; only `status = "ready"` rows belong here.
     */
    fun selectForEviction(
        readyByOldestAccess: List<ThumbnailCacheEntity>,
        maxBytes: Long,
    ): List<ThumbnailCacheEntity> {
        if (maxBytes < 0) return emptyList()
        var total = readyByOldestAccess.sumOf { it.bytesOnDisk }
        if (total <= maxBytes) return emptyList()
        val evict = mutableListOf<ThumbnailCacheEntity>()
        for (item in readyByOldestAccess) {
            if (total <= maxBytes) break
            evict += item
            total -= item.bytesOnDisk
        }
        return evict
    }
}
