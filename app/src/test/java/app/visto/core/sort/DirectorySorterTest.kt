package app.visto.core.sort

import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class DirectorySorterTest {

    private fun folder(name: String, modified: Long? = null): RemoteEntry = RemoteEntry(
        accountId = 1,
        parentPath = "/Photos",
        path = "/Photos/$name",
        name = name,
        isDirectory = true,
        mediaType = MediaType.OTHER,
        lastModifiedEpochMs = modified,
    )

    private fun image(name: String, modified: Long? = null, size: Long? = null, type: MediaType = MediaType.IMAGE): RemoteEntry = RemoteEntry(
        accountId = 1,
        parentPath = "/Photos",
        path = "/Photos/$name",
        name = name,
        isDirectory = false,
        mediaType = type,
        sizeBytes = size,
        lastModifiedEpochMs = modified,
    )

    private val mixed = listOf(
        image("z.jpg", modified = 100, size = 500),
        folder("Travel", modified = 50),
        image("a.mp4", modified = 200, size = 8000, type = MediaType.VIDEO),
        folder("Camera", modified = 80),
        image("b.gif", modified = 150, size = 1000, type = MediaType.ANIMATED_IMAGE),
    )

    @Test
    fun foldersAlwaysComeFirst() {
        val sorted = DirectorySorter.sort(mixed, SortMode.NAME_ASC)
        val firstTwo = sorted.take(2).map { it.isDirectory }
        assertEquals(listOf(true, true), firstTwo)
        assertEquals(listOf(false, false, false), sorted.drop(2).map { it.isDirectory })
    }

    @Test
    fun nameAscOrdersAlphabetically() {
        val sorted = DirectorySorter.sort(mixed, SortMode.NAME_ASC).map { it.name }
        assertEquals(listOf("Camera", "Travel", "a.mp4", "b.gif", "z.jpg"), sorted)
    }

    @Test
    fun nameDescReversesWithinGroups() {
        val sorted = DirectorySorter.sort(mixed, SortMode.NAME_DESC).map { it.name }
        assertEquals(listOf("Travel", "Camera", "z.jpg", "b.gif", "a.mp4"), sorted)
    }

    @Test
    fun modifiedNewestFirstIsDefault() {
        val sorted = DirectorySorter.sort(mixed, SortMode.DEFAULT).map { it.name }
        assertEquals(listOf("Camera", "Travel", "a.mp4", "b.gif", "z.jpg"), sorted)
    }

    @Test
    fun modifiedOldestFirstOrdersAscending() {
        val sorted = DirectorySorter.sort(mixed, SortMode.MODIFIED_OLDEST_FIRST).map { it.name }
        assertEquals(listOf("Travel", "Camera", "z.jpg", "b.gif", "a.mp4"), sorted)
    }

    @Test
    fun sizeSortIgnoresFoldersForSizeFieldButKeepsFolderFirst() {
        val sorted = DirectorySorter.sort(mixed, SortMode.SIZE_LARGEST_FIRST).map { it.name }
        assertEquals("Camera", sorted.first())
        assertEquals("Travel", sorted[1])
        assertEquals(listOf("a.mp4", "b.gif", "z.jpg"), sorted.drop(2))
    }

    @Test
    fun typeSortGroupsByMediaType() {
        val sorted = DirectorySorter.sort(mixed, SortMode.TYPE).map { it.name }
        // folders first, then IMAGE -> ANIMATED_IMAGE -> VIDEO
        assertEquals(listOf("Camera", "Travel", "z.jpg", "b.gif", "a.mp4"), sorted)
    }

    @Test
    fun sortIsStableForEqualKeys() {
        val tied = listOf(
            image("a.jpg", modified = 100),
            image("b.jpg", modified = 100),
            image("c.jpg", modified = 100),
        )
        val sorted = DirectorySorter.sort(tied, SortMode.MODIFIED_NEWEST_FIRST).map { it.name }
        assertEquals(listOf("a.jpg", "b.jpg", "c.jpg"), sorted)
    }
}
