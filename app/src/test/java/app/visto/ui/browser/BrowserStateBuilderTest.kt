package app.visto.ui.browser

import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import app.visto.core.sort.SortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserStateBuilderTest {

    private fun folder(name: String, modified: Long? = null) = RemoteEntry(
        accountId = 1,
        parentPath = "/Photos",
        path = "/Photos/$name",
        name = name,
        isDirectory = true,
        mediaType = MediaType.OTHER,
        lastModifiedEpochMs = modified,
    )

    private fun image(name: String, modified: Long? = null) = RemoteEntry(
        accountId = 1,
        parentPath = "/Photos",
        path = "/Photos/$name",
        name = name,
        isDirectory = false,
        mediaType = MediaType.IMAGE,
        lastModifiedEpochMs = modified,
    )

    @Test
    fun applyPartitionsFoldersAndMedia() {
        val state = BrowserStateBuilder.apply(
            currentPath = "/Photos",
            entries = listOf(image("b.jpg", 100), folder("Travel", 50), image("a.mp4", 200)),
            sortMode = SortMode.NAME_ASC,
        )
        assertEquals(listOf("Travel"), state.folders.map { it.name })
        assertEquals(listOf("a.mp4", "b.jpg"), state.media.map { it.name })
        assertEquals(SortMode.NAME_ASC, state.sortMode)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun emptyStateExposesEmptyFlag() {
        val state = BrowserStateBuilder.apply(
            currentPath = "/Photos",
            entries = emptyList(),
            sortMode = SortMode.DEFAULT,
        )
        assertTrue(state.isEmpty)
    }
}
