package app.visto.ui.viewer

import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerSessionTest {

    private fun item(name: String, type: MediaType, dir: Boolean = false) = RemoteEntry(
        accountId = 1,
        parentPath = "/Photos",
        path = "/Photos/$name",
        name = name,
        isDirectory = dir,
        mediaType = if (dir) MediaType.OTHER else type,
    )

    @Test
    fun buildSkipsFoldersAndUnsupportedFiles() {
        val entries = listOf(
            item("Travel", MediaType.OTHER, dir = true),
            item("a.jpg", MediaType.IMAGE),
            item("notes.txt", MediaType.OTHER),
            item("v.mp4", MediaType.VIDEO),
            item("g.gif", MediaType.ANIMATED_IMAGE),
        )
        val session = ViewerSession.build(entries, openedPath = "/Photos/v.mp4")
        assertEquals(listOf("a.jpg", "v.mp4", "g.gif"), session.items.map { it.name })
        assertEquals(1, session.initialIndex)
    }

    @Test
    fun missingOpenedPathFallsBackToFirst() {
        val entries = listOf(item("a.jpg", MediaType.IMAGE))
        val session = ViewerSession.build(entries, openedPath = "/Photos/nope.jpg")
        assertEquals(0, session.initialIndex)
    }
}
