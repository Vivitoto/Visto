package app.visto

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectSmokeTest {
    @Test
    fun appInfoMatchesProjectIdentity() {
        assertEquals("Visto", AppInfo.APP_NAME)
        assertEquals("app.visto", AppInfo.PACKAGE_NAME)
        assertEquals("1.1.12", AppInfo.VERSION_NAME)
        assertEquals(33, AppInfo.VERSION_CODE)
    }
}
