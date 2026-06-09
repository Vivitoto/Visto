package app.visto.core.sort

import org.junit.Assert.assertEquals
import org.junit.Test

class SortModeTest {

    @Test
    fun fromStorageRestoresStoredKey() {
        assertEquals(SortMode.NAME_ASC, SortMode.fromStorage("name_asc"))
        assertEquals(SortMode.SIZE_SMALLEST_FIRST, SortMode.fromStorage("size_smallest_first"))
    }

    @Test
    fun fromStorageAcceptsLegacyEnumNameAndFallsBackToDefault() {
        assertEquals(SortMode.TYPE, SortMode.fromStorage("TYPE"))
        assertEquals(SortMode.DEFAULT, SortMode.fromStorage(null))
        assertEquals(SortMode.DEFAULT, SortMode.fromStorage("unknown"))
    }
}
