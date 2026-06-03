package app.visto.ui.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserNavigatorTest {

    @Test
    fun newNavigatorStartsAtRootByDefault() {
        val nav = BrowserNavigator()
        assertEquals("/", nav.currentPath)
        assertFalse(nav.canGoBack)
        assertNull(nav.back())
    }

    @Test
    fun openPushesNormalizedPath() {
        val nav = BrowserNavigator()
        nav.open("Photos")
        assertEquals("/Photos", nav.currentPath)
        nav.open("/Photos/Travel/")
        assertEquals("/Photos/Travel", nav.currentPath)
        assertTrue(nav.canGoBack)
    }

    @Test
    fun backPopsAndReturnsPreviousPath() {
        val nav = BrowserNavigator("/Photos")
        nav.open("/Photos/Travel")
        nav.open("/Photos/Travel/Japan")
        assertEquals("/Photos/Travel", nav.back())
        assertEquals("/Photos", nav.back())
        assertNull(nav.back())
    }

    @Test
    fun openIgnoresDuplicatePath() {
        val nav = BrowserNavigator("/Photos")
        nav.open("/Photos")
        assertFalse(nav.canGoBack)
    }
}
