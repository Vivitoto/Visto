package app.visto.data.thumbnail

import app.visto.data.db.ThumbnailCacheEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ThumbnailLruPolicyTest {

    private fun row(id: Long, bytes: Long, accessed: Long) = ThumbnailCacheEntity(
        id = id,
        remoteEntryId = id,
        cacheKey = "k$id",
        thumbPath = "/tmp/$id",
        width = 240,
        height = 240,
        sourceEtag = null,
        sourceSizeBytes = bytes,
        createdAt = 0,
        lastAccessedAt = accessed,
        bytesOnDisk = bytes,
        status = "ready",
    )

    @Test
    fun underLimitEvictsNothing() {
        val rows = listOf(row(1, 100, 1), row(2, 200, 2))
        assertEquals(emptyList<ThumbnailCacheEntity>(), ThumbnailLruPolicy.selectForEviction(rows, 500))
    }

    @Test
    fun atLimitEvictsNothing() {
        val rows = listOf(row(1, 300, 1), row(2, 200, 2))
        assertEquals(emptyList<ThumbnailCacheEntity>(), ThumbnailLruPolicy.selectForEviction(rows, 500))
    }

    @Test
    fun overLimitEvictsOldestFirst() {
        val rows = listOf(
            row(1, 300, 1), // oldest
            row(2, 200, 2),
            row(3, 400, 3),
        )
        val evicted = ThumbnailLruPolicy.selectForEviction(rows, 500)
        assertEquals(listOf(1L, 2L), evicted.map { it.id })
    }

    @Test
    fun zeroLimitEvictsAll() {
        val rows = listOf(row(1, 1, 1), row(2, 2, 2))
        val evicted = ThumbnailLruPolicy.selectForEviction(rows, 0)
        assertEquals(listOf(1L, 2L), evicted.map { it.id })
    }
}
