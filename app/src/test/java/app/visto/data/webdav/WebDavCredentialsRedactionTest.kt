package app.visto.data.webdav

import org.junit.Assert.assertFalse
import org.junit.Test

class WebDavCredentialsRedactionTest {
    @Test
    fun toStringDoesNotLeakSecrets() {
        val creds = WebDavCredentials(
            baseUrl = "https://dav.example.com/dav",
            username = "alice",
            password = "very-secret",
        )
        val text = creds.toString()
        assertFalse("URL should be redacted", text.contains("dav.example.com"))
        assertFalse("username should be redacted", text.contains("alice"))
        assertFalse("password must be redacted", text.contains("very-secret"))
    }
}
