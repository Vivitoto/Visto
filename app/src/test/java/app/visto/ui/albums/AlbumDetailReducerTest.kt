package app.visto.ui.albums

import app.visto.core.media.MediaType
import app.visto.core.model.RemoteEntry
import app.visto.data.account.AlbumViewMode
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
    fun startFolderCanPreserveIconGridViewMode() {
        val iconState = baseState.copy(viewMode = AlbumViewMode.FLAT)
        val next = AlbumDetailReducer.startFolder(iconState, "/Picture/写真", AlbumViewMode.FLAT)
        assertEquals(AlbumViewMode.FLAT, next.viewMode)
        assertEquals("/Picture/写真", next.folderView.currentPath)
        assertTrue(next.folderView.isLoading)
    }

    @Test
    fun iconGridOnlyExposesCurrentFolderMediaForViewer() {
        val entries = listOf(
            folder("/Picture/写真/A"),
            image("/Picture/写真/a.jpg"),
        )
        val started = AlbumDetailReducer.startFolder(
            baseState.copy(viewMode = AlbumViewMode.FLAT),
            "/Picture/写真",
            AlbumViewMode.FLAT,
        )
        val next = AlbumDetailReducer.applyFolderContents(started, "/Picture/写真", entries)
        assertEquals(AlbumViewMode.FLAT, next.viewMode)
        assertEquals(listOf("a.jpg"), next.visibleMedia.map { it.name })
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
