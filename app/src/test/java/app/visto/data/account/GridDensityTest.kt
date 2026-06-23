package app.visto.data.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GridDensityTest {

    @Test
    fun albumToolbarCyclesThroughDensityModesThenList() {
        assertEquals(GridDensity.STANDARD, GridDensity.COMFORTABLE.nextAlbumFolderGridDensityOrNull())
        assertEquals(GridDensity.COMPACT, GridDensity.STANDARD.nextAlbumFolderGridDensityOrNull())
        assertNull(GridDensity.COMPACT.nextAlbumFolderGridDensityOrNull())
    }
}
