package app.visto.data.thumbnail

import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThumbnailCacheKeyTest {

    private fun entry(
        path: String = "/Photos/a.jpg",
        etag: String? = null,
        size: Long? = null,
        modified: Long? = null,
    ) = RemoteEntry(
        accountId = 1,
        parentPath = "/Photos",
        path = path,
        name = path.substringAfterLast('/'),
        isDirectory = false,
        mediaType = MediaType.IMAGE,
        sizeBytes = size,
        etag = etag,
        lastModifiedEpochMs = modified,
    )

    @Test
    fun samePathAndEtagYieldsSameKey() {
        val a = ThumbnailCacheKey.forEntry(entry(etag = "abc"))
        val b = ThumbnailCacheKey.forEntry(entry(etag = "abc"))
        assertEquals(a, b)
        assertEquals(64, a.length)
    }

    @Test
    fun changingEtagChangesKey() {
        val a = ThumbnailCacheKey.forEntry(entry(etag = "abc"))
        val b = ThumbnailCacheKey.forEntry(entry(etag = "def"))
        assertNotEquals(a, b)
    }

    @Test
    fun fallbackUsesSizeAndModifiedWhenNoEtag() {
        val a = ThumbnailCacheKey.forEntry(entry(size = 100, modified = 1))
        val b = ThumbnailCacheKey.forEntry(entry(size = 100, modified = 1))
        val c = ThumbnailCacheKey.forEntry(entry(size = 200, modified = 1))
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun differentPathsYieldDifferentKeys() {
        val a = ThumbnailCacheKey.forEntry(entry(path = "/Photos/a.jpg"))
        val b = ThumbnailCacheKey.forEntry(entry(path = "/Photos/b.jpg"))
        assertNotEquals(a, b)
    }
}
