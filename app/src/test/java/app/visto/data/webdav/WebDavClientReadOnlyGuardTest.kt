package app.visto.data.webdav

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v0.1 of Visto is strictly read-only on WebDAV. This guard test fails if
 * anyone accidentally adds DELETE / PUT / MOVE / COPY / MKCOL surface to the
 * client without an explicit decision.
 */
class WebDavClientReadOnlyGuardTest {

    @Test
    fun publicSurfaceIsReadOnly() {
        val methods = WebDavClient::class.java.methods
            .filter { it.declaringClass == WebDavClient::class.java }
            .map { it.name }
            .toSet()

        // Allowed surface.
        assertTrue("listDirectory must exist", methods.contains("listDirectory"))
        assertTrue("buildMediaRequest must exist", methods.contains("buildMediaRequest"))

        val forbidden = listOf(
            "delete", "deleteFile", "deleteDirectory",
            "put", "upload", "uploadFile",
            "move", "rename", "copy", "mkcol", "createDirectory",
        )
        for (name in forbidden) {
            assertFalse(
                "WebDavClient must not expose a mutation method: $name",
                methods.any { it.equals(name, ignoreCase = true) }
            )
        }
    }
}
