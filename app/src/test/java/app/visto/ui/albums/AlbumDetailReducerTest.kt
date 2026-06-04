package app.visto.ui.albums

import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import app.visto.data.account.AlbumViewMode
import app.visto.data.album.AlbumContents
import app.visto.data.album.AlbumSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumDetailReducerTest {

    private val baseState = AlbumDetailUiState(
        title = "Picture",
        rootPath = "/Picture",
    )

    @Test
    fun defaultStateStartsInFoldersModeAtRoot() {
        assertEquals(AlbumViewMode.FOLDERS, baseState.viewMode)
        assertEquals("/Picture", baseState.folderView.currentPath)
        assertTrue(baseState.folderView.isLoading)
    }

    @Test
    fun startFolderResetsCurrentPathAndMarksLoading() {
        val next = AlbumDetailReducer.startFolder(baseState, "/Picture/写真")
        assertEquals(AlbumViewMode.FOLDERS, next.viewMode)
        assertEquals("/Picture/写真", next.folderView.currentPath)
        assertTrue(next.folderView.isLoading)
        assertEquals(0, next.folderView.folders.size)
        assertEquals(0, next.folderView.media.size)
    }

    @Test
    fun applyFolderContentsSplitsDirsAndMedia() {
        val entries = listOf(
            folder("/Picture/写真/A"),
            folder("/Picture/写真/B"),
            image("/Picture/写真/cover.jpg"),
            image("/Picture/写真/a.jpg"),
        )
        val started = AlbumDetailReducer.startFolder(baseState, "/Picture/写真")
        val next = AlbumDetailReducer.applyFolderContents(started, "/Picture/写真", entries)
        assertFalse(next.folderView.isLoading)
        assertEquals(listOf("A", "B"), next.folderView.folders.map { it.name })
        assertEquals(listOf("a.jpg", "cover.jpg"), next.folderView.media.map { it.name })
    }

    @Test
    fun startFlatSwitchesViewModeAndMarksFlatLoading() {
        val next = AlbumDetailReducer.startFlat(baseState)
        assertEquals(AlbumViewMode.FLAT, next.viewMode)
        assertTrue(next.flatView.isLoading)
    }

    @Test
    fun applyFlatContentsPopulatesFlatSections() {
        val contents = AlbumContents(
            rootPath = "/Picture",
            sections = listOf(
                AlbumSection(
                    title = "写真",
                    parentPath = "/Picture/写真",
                    media = listOf(image("/Picture/写真/a.jpg")),
                ),
            ),
            totalMedia = 1,
            foldersVisited = 1,
            foldersFailed = 0,
            warnings = emptyList(),
        )
        val started = AlbumDetailReducer.startFlat(baseState)
        val next = AlbumDetailReducer.applyFlatContents(started, contents, stillLoading = false)
        assertEquals(AlbumViewMode.FLAT, next.viewMode)
        assertFalse(next.flatView.isLoading)
        assertEquals(1, next.flatView.sections.size)
        assertEquals("/Picture/写真/a.jpg", next.visibleMedia.first().path)
    }

    @Test
    fun applyErrorStopsLoadingForActiveMode() {
        val started = AlbumDetailReducer.startFolder(baseState, "/Picture")
        val errored = AlbumDetailReducer.applyError(started, "炸了")
        assertEquals("炸了", errored.errorMessage)
        assertFalse(errored.folderView.isLoading)
    }

    private fun folder(path: String): RemoteEntry {
        val name = path.substringAfterLast('/')
        return RemoteEntry(
            accountId = 0L,
            parentPath = path.substringBeforeLast('/').ifEmpty { "/" },
            path = path,
            name = name,
            isDirectory = true,
            mediaType = MediaType.OTHER,
        )
    }

    private fun image(path: String): RemoteEntry {
        val name = path.substringAfterLast('/')
        return RemoteEntry(
            accountId = 0L,
            parentPath = path.substringBeforeLast('/').ifEmpty { "/" },
            path = path,
            name = name,
            isDirectory = false,
            mediaType = MediaType.IMAGE,
            mimeType = "image/jpeg",
            sizeBytes = 1024,
        )
    }
}
