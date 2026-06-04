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

    @Test
    fun parentOfClampsToConfiguredRoot() {
        assertEquals("/Photos", FolderPickerNavigator.parentOf("/Photos", "/Photos"))
        assertEquals("/Photos", FolderPickerNavigator.parentOf("/Photos/Family", "/Photos"))
        assertEquals("/Photos", FolderPickerNavigator.parentOf("/Other", "/Photos"))
    }

    @Test
    fun clampToRootUsesPathBoundary() {
        assertEquals("/Photos", FolderPickerNavigator.clampToRoot("/Photos2", "/Photos"))
        assertEquals("/Photos/Family", FolderPickerNavigator.clampToRoot("/Photos/Family", "/Photos"))
    }
}
