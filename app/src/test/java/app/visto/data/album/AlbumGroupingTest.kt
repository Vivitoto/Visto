package app.visto.data.album

import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumGroupingTest {

    private fun image(parent: String, name: String) = RemoteEntry(
        accountId = 1,
        parentPath = parent,
        path = "$parent/$name".replace("//", "/"),
        name = name,
        isDirectory = false,
        mediaType = MediaType.IMAGE,
    )

    private fun dir(parent: String, name: String) = RemoteEntry(
        accountId = 1,
        parentPath = parent,
        path = "$parent/$name".replace("//", "/"),
        name = name,
        isDirectory = true,
        mediaType = MediaType.OTHER,
    )

    @Test
    fun sectionTitleHandlesRootAndNestedParents() {
        assertEquals("", AlbumSectionTitle.forParent("/Photos", "/Photos"))
        assertEquals("2024", AlbumSectionTitle.forParent("/Photos", "/Photos/2024"))
        assertEquals("2024/Trip", AlbumSectionTitle.forParent("/Photos", "/Photos/2024/Trip"))
        assertEquals("Family", AlbumSectionTitle.forParent("/", "/Family"))
    }

    @Test
    fun groupSplitsByParentAndOrders() {
        val media = listOf(
            image("/Photos", "b.jpg"),
            image("/Photos", "a.jpg"),
            image("/Photos/2024/Trip", "z.jpg"),
            image("/Photos/2024", "m.jpg"),
            dir("/Photos", "ignored"),
        )
        val sections = AlbumGrouper.group("/Photos", media)
        assertEquals(listOf("", "2024", "2024/Trip"), sections.map { it.title })
        assertEquals(listOf("a.jpg", "b.jpg"), sections[0].media.map { it.name })
        assertEquals(listOf("m.jpg"), sections[1].media.map { it.name })
        assertEquals(listOf("z.jpg"), sections[2].media.map { it.name })
    }
}
