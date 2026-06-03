package app.visto.data.webdav

import app.visto.core.media.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavListingMapperTest {

    @Test
    fun filtersSelfAndKeepsDirectChildren() {
        val rows = listOf(
            WebDavRow("/dav/Photos/", isDirectory = true, displayName = "Photos"),
            WebDavRow("/dav/Photos/Travel/", isDirectory = true, displayName = "Travel"),
            WebDavRow(
                rawHref = "/dav/Photos/a.jpg",
                isDirectory = false,
                mimeType = "image/jpeg",
                sizeBytes = 100,
                etag = "\"e\"",
                lastModifiedEpochMs = 1000,
            ),
            // Should be filtered: nested deeper than depth 1
            WebDavRow("/dav/Photos/Travel/Japan/b.jpg", isDirectory = false, mimeType = "image/jpeg"),
        )

        val entries = WebDavListingMapper.map(
            accountId = 7,
            baseUrl = "https://dav.example.com/dav",
            requestedPath = "/Photos",
            rows = rows,
        )

        assertEquals(2, entries.size)
        val (folder, file) = entries
        assertEquals("/Photos/Travel", folder.path)
        assertEquals("Travel", folder.name)
        assertEquals(true, folder.isDirectory)
        assertEquals(MediaType.OTHER, folder.mediaType)

        assertEquals("/Photos/a.jpg", file.path)
        assertEquals("a.jpg", file.name)
        assertEquals(MediaType.IMAGE, file.mediaType)
        assertEquals(100L, file.sizeBytes)
        assertEquals(1000L, file.lastModifiedEpochMs)
        assertEquals(7L, file.accountId)
        assertEquals("/Photos", file.parentPath)
    }

    @Test
    fun detectsAnimatedTypesForWebpAndGif() {
        val rows = listOf(
            WebDavRow("/dav/Photos/a.gif", isDirectory = false),
            WebDavRow("/dav/Photos/b.webp", isDirectory = false, mimeType = "image/webp"),
            WebDavRow("/dav/Photos/c.mp4", isDirectory = false, mimeType = "video/mp4"),
        )

        val entries = WebDavListingMapper.map(
            accountId = 1,
            baseUrl = "https://dav.example.com/dav",
            requestedPath = "/Photos",
            rows = rows,
        )

        assertEquals(MediaType.ANIMATED_IMAGE, entries[0].mediaType)
        assertEquals(MediaType.ANIMATED_IMAGE, entries[1].mediaType)
        assertEquals(MediaType.VIDEO, entries[2].mediaType)
        assertTrue(entries.all { !it.isDirectory })
    }
}
