package app.visto

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectSmokeTest {
    @Test
    fun appInfoMatchesProjectIdentity() {
        assertEquals("Visto", AppInfo.APP_NAME)
        assertEquals("app.visto", AppInfo.PACKAGE_NAME)
        assertEquals("0.1.10", AppInfo.VERSION_NAME)
    }
}
