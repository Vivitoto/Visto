package app.visto.core.model

import app.visto.core.media.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteEntryTest {

    @Test
    fun directoryEntryHoldsBasicFields() {
        val dir = RemoteEntry(
            accountId = 1,
            parentPath = "/Photos",
            path = "/Photos/Travel",
            name = "Travel",
            isDirectory = true,
            mediaType = MediaType.OTHER,
        )
        assertEquals("/Photos/Travel", dir.path)
        assertEquals("Travel", dir.name)
        assertEquals(MediaType.OTHER, dir.mediaType)
        assertNull(dir.mimeType)
        assertNull(dir.sizeBytes)
    }

    @Test
    fun mediaEntryCarriesMimeAndSize() {
        val photo = RemoteEntry(
            accountId = 1,
            parentPath = "/Photos",
            path = "/Photos/a.jpg",
            name = "a.jpg",
            isDirectory = false,
            mediaType = MediaType.IMAGE,
            mimeType = "image/jpeg",
            sizeBytes = 1234,
            etag = "\"deadbeef\"",
            lastModifiedEpochMs = 1_700_000_000_000,
        )
        assertEquals("image/jpeg", photo.mimeType)
        assertEquals(1234L, photo.sizeBytes)
        assertEquals("\"deadbeef\"", photo.etag)
    }

    @Test
    fun pathMustBeAbsolute() {
        assertThrows(IllegalArgumentException::class.java) {
            RemoteEntry(
                accountId = 1,
                parentPath = null,
                path = "relative/path",
                name = "path",
                isDirectory = true,
                mediaType = MediaType.OTHER,
            )
        }
    }

    @Test
    fun parentPathMustBeAbsoluteWhenSet() {
        assertThrows(IllegalArgumentException::class.java) {
            RemoteEntry(
                accountId = 1,
                parentPath = "relative",
                path = "/x",
                name = "x",
                isDirectory = true,
                mediaType = MediaType.OTHER,
            )
        }
    }

    @Test
    fun directoryEntryCannotClaimMediaCategory() {
        assertThrows(IllegalArgumentException::class.java) {
            RemoteEntry(
                accountId = 1,
                parentPath = "/Photos",
                path = "/Photos/Travel",
                name = "Travel",
                isDirectory = true,
                mediaType = MediaType.IMAGE,
            )
        }
    }

    @Test
    fun rootEntryHasNullParent() {
        val root = RemoteEntry(
            accountId = 1,
            parentPath = null,
            path = "/",
            name = "/",
            isDirectory = true,
            mediaType = MediaType.OTHER,
        )
        assertNull(root.parentPath)
    }
}
