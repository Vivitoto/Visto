package app.visto.data.webdav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SecretRedactorTest {

    @Test
    fun redactsInlineUrlCredentials() {
        val out = SecretRedactor.redactUrl("https://alice:secret@dav.example.com/dav")
        assertEquals("https://<redacted>@dav.example.com/dav", out)
    }

    @Test
    fun redactsTokenQueryParameters() {
        val out = SecretRedactor.redactUrl("https://example.com/file?token=abc123&id=1")
        assertEquals("https://example.com/file?token=<redacted>&id=1", out)
    }

    @Test
    fun redactsBearerAndBasicAuthHeaders() {
        val out = SecretRedactor.redactLogLine("Authorization: Bearer top-secret-token\nAuthorization: Basic YWxpY2U6c2VjcmV0")
        assertFalse("token must be gone", out.contains("top-secret-token"))
        assertFalse("basic creds must be gone", out.contains("YWxpY2U6c2VjcmV0"))
    }
}
