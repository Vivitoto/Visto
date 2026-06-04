package app.visto.ui.albums

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderPickerNavigatorTest {
    @Test
    fun normalizeMakesAbsoluteCleanPath() {
        assertEquals("/", FolderPickerNavigator.normalize(""))
        assertEquals("/", FolderPickerNavigator.normalize("/"))
        assertEquals("/Photos/Family", FolderPickerNavigator.normalize("Photos//Family/"))
        assertEquals("/Photos/Family", FolderPickerNavigator.normalize("/Photos/Family"))
    }

    @Test
    fun parentOfReturnsPreviousFolder() {
        assertEquals("/", FolderPickerNavigator.parentOf("/"))
        assertEquals("/", FolderPickerNavigator.parentOf("/Photos"))
        assertEquals("/Photos", FolderPickerNavigator.parentOf("/Photos/Family"))
        assertEquals("/Photos/Family", FolderPickerNavigator.parentOf("/Photos/Family/2024"))
    }
}
