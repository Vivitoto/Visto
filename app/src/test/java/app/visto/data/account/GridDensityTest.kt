package app.visto.data.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GridDensityTest {

    @Test
    fun albumToolbarCyclesThroughTwoThreeFiveColumnsThenList() {
        assertEquals(2, GridDensity.COMFORTABLE.folderColumns)
        assertEquals(3, GridDensity.STANDARD.folderColumns)
        assertEquals(5, GridDensity.COMPACT.folderColumns)
        assertEquals(GridDensity.STANDARD, GridDensity.COMFORTABLE.nextAlbumFolderGridDensityOrNull())
        assertEquals(GridDensity.COMPACT, GridDensity.STANDARD.nextAlbumFolderGridDensityOrNull())
        assertNull(GridDensity.COMPACT.nextAlbumFolderGridDensityOrNull())
    }
}
