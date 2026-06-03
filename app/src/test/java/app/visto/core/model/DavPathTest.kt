package app.visto.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DavPathTest {

    @Test
    fun normalizeHandlesNullAndBlank() {
        assertEquals("/", DavPath.normalize(null))
        assertEquals("/", DavPath.normalize(""))
        assertEquals("/", DavPath.normalize("   "))
        assertEquals("/", DavPath.normalize("/"))
        assertEquals("/", DavPath.normalize("///"))
    }

    @Test
    fun normalizeAddsLeadingSlashAndTrimsTrailing() {
        assertEquals("/Photos", DavPath.normalize("Photos"))
        assertEquals("/Photos", DavPath.normalize("Photos/"))
        assertEquals("/Photos/Travel", DavPath.normalize("/Photos/Travel/"))
    }

    @Test
    fun normalizeCollapsesRepeatedSlashes() {
        assertEquals("/Photos/Travel", DavPath.normalize("/Photos//Travel"))
        assertEquals("/Photos/Travel", DavPath.normalize("///Photos///Travel///"))
    }

    @Test
    fun joinAppendsSegments() {
        assertEquals("/Photos/Travel", DavPath.join("/Photos", "Travel"))
        assertEquals("/Photos/Travel", DavPath.join("/Photos", "/Travel/"))
        assertEquals("/Travel", DavPath.join("/", "Travel"))
        assertEquals("/Photos", DavPath.join("/Photos", ""))
        assertEquals("/Photos/Travel/Japan", DavPath.join("/Photos", "Travel/Japan"))
    }

    @Test
    fun parentOfRootIsNull() {
        assertNull(DavPath.parent("/"))
        assertNull(DavPath.parent(""))
    }

    @Test
    fun parentOfTopLevelIsRoot() {
        assertEquals("/", DavPath.parent("/Photos"))
        assertEquals("/", DavPath.parent("Photos"))
    }

    @Test
    fun parentOfNestedReturnsContainer() {
        assertEquals("/Photos", DavPath.parent("/Photos/Travel"))
        assertEquals("/Photos/Travel", DavPath.parent("/Photos/Travel/Japan/"))
    }

    @Test
    fun displayNameUsesLastSegment() {
        assertEquals("/", DavPath.displayName("/"))
        assertEquals("Photos", DavPath.displayName("/Photos"))
        assertEquals("Japan", DavPath.displayName("/Photos/Travel/Japan/"))
    }

    @Test
    fun isDirectChildDetectsSingleLevel() {
        assertTrue(DavPath.isDirectChild("/Photos", "/Photos/a.jpg"))
        assertTrue(DavPath.isDirectChild("/", "/Photos"))
        assertFalse(DavPath.isDirectChild("/Photos", "/Photos/Travel/a.jpg"))
        assertFalse(DavPath.isDirectChild("/Photos", "/"))
    }
}
