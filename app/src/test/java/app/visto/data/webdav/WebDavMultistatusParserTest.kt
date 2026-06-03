package app.visto.data.webdav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavMultistatusParserTest {

    @Test
    fun parsesDirectoryAndJpegFile() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/dav/Photos/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype><d:collection/></d:resourcetype>
                    <d:displayname>Photos</d:displayname>
                    <d:getlastmodified>Tue, 03 Jun 2025 10:00:00 GMT</d:getlastmodified>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/dav/Photos/Travel/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype><d:collection/></d:resourcetype>
                    <d:displayname>Travel</d:displayname>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/dav/Photos/a.jpg</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype/>
                    <d:getcontentlength>4242</d:getcontentlength>
                    <d:getcontenttype>image/jpeg</d:getcontenttype>
                    <d:getetag>"abc123"</d:getetag>
                    <d:getlastmodified>Mon, 02 Jun 2025 08:30:00 GMT</d:getlastmodified>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val rows = WebDavMultistatusParser.parse(xml)
        assertEquals(3, rows.size)

        val self = rows[0]
        assertEquals("/dav/Photos/", self.rawHref)
        assertTrue(self.isDirectory)
        assertEquals("Photos", self.displayName)

        val travel = rows[1]
        assertTrue(travel.isDirectory)
        assertEquals("Travel", travel.displayName)

        val file = rows[2]
        assertEquals(false, file.isDirectory)
        assertEquals(4242L, file.sizeBytes)
        assertEquals("image/jpeg", file.mimeType)
        assertEquals("\"abc123\"", file.etag)
        assertNull(file.displayName)
        // 2025-06-02T08:30:00 GMT
        assertEquals(1748853000000L, file.lastModifiedEpochMs)
    }

    @Test
    fun ignoresNonSuccessfulPropstatBlocks() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/dav/Photos/secret.jpg</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype/>
                  </d:prop>
                  <d:status>HTTP/1.1 404 Not Found</d:status>
                </d:propstat>
                <d:propstat>
                  <d:prop>
                    <d:getcontentlength>9</d:getcontentlength>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val rows = WebDavMultistatusParser.parse(xml)
        assertEquals(1, rows.size)
        val row = rows.first()
        assertEquals(9L, row.sizeBytes)
        assertEquals(false, row.isDirectory)
    }

    @Test
    fun toleratesUnknownNamespacePrefixes() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <multistatus xmlns="DAV:">
              <response>
                <href>/Photos/b.png</href>
                <propstat>
                  <prop>
                    <resourcetype/>
                    <getcontenttype>image/png</getcontenttype>
                  </prop>
                  <status>HTTP/1.1 200 OK</status>
                </propstat>
              </response>
            </multistatus>
        """.trimIndent()

        val rows = WebDavMultistatusParser.parse(xml)
        assertEquals(1, rows.size)
        assertEquals("image/png", rows.first().mimeType)
    }

    @Test
    fun malformedXmlRaisesParseError() {
        assertThrows(WebDavError.ParseError::class.java) {
            WebDavMultistatusParser.parse("<<not xml")
        }
    }
}
