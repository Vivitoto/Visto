package app.visto.data.webdav

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebDavClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(basePath: String = "/dav"): WebDavClient = WebDavClient(
        credentials = WebDavCredentials(
            baseUrl = server.url(basePath).toString(),
            username = "alice",
            password = "secret",
        ),
        accountId = 42,
    )

    @Test
    fun listDirectoryEmitsPropfindWithDepth1AndBasicAuth() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(207).setBody(MULTISTATUS_PHOTOS))

        val entries = client().listDirectory("/Photos")

        val recorded = server.takeRequest()
        assertEquals("PROPFIND", recorded.method)
        assertEquals("1", recorded.getHeader("Depth"))
        assertEquals("Basic YWxpY2U6c2VjcmV0", recorded.getHeader("Authorization"))
        assertTrue(recorded.path?.startsWith("/dav/Photos") == true)
        val body = recorded.body.readUtf8()
        assertTrue("body should request resourcetype", body.contains("resourcetype"))
        assertTrue("body should request getcontentlength", body.contains("getcontentlength"))

        assertEquals(2, entries.size)
        val (travel, photo) = entries
        assertEquals("/Photos/Travel", travel.path)
        assertEquals(true, travel.isDirectory)
        assertEquals("/Photos/a.jpg", photo.path)
        assertEquals(false, photo.isDirectory)
    }

    @Test
    fun authFailureMapsToWebDavAuthFailed() {
        server.enqueue(MockResponse().setResponseCode(401))
        assertThrows(WebDavError.AuthFailed::class.java) {
            runBlocking { client().listDirectory("/Photos") }
        }
    }

    @Test
    fun notFoundMapsToWebDavNotFound() {
        server.enqueue(MockResponse().setResponseCode(404))
        assertThrows(WebDavError.NotFound::class.java) {
            runBlocking { client().listDirectory("/Missing") }
        }
    }

    @Test
    fun serverErrorMapsToWebDavServerError() {
        server.enqueue(MockResponse().setResponseCode(503))
        val error = assertThrows(WebDavError.ServerError::class.java) {
            runBlocking { client().listDirectory("/Photos") }
        }
        assertEquals(503, error.statusCode)
    }

    @Test
    fun mediaRequestHasGetMethodAndAuth() {
        val request = client().buildMediaRequest("/Photos/a.jpg")
        assertEquals("GET", request.method)
        assertEquals("Basic YWxpY2U6c2VjcmV0", request.header("Authorization"))
        assertTrue(request.url.toString().endsWith("/dav/Photos/a.jpg"))
    }

    @Test(expected = WebDavError.InvalidPath::class)
    fun listDirectoryRejectsDotDotSegments() {
        runBlocking { client().listDirectory("/Photos/../Secret") }
    }

    @Test(expected = WebDavError.InvalidPath::class)
    fun listDirectoryRejectsSingleDotSegments() {
        runBlocking { client().listDirectory("/Photos/./stuff") }
    }

    @Test
    fun listDirectoryEncodesDecodedSpecialPathSegmentsUnderBasePath() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(207).setBody(MULTISTATUS_EMPTY_SPECIAL))

        client(basePath = "/dav/photos root").listDirectory("/相册 A/a+b#c%raw")

        val recorded = server.takeRequest()
        assertEquals(
            "/dav/photos%20root/%E7%9B%B8%E5%86%8C%20A/a+b%23c%25raw/",
            recorded.requestUrl?.encodedPath,
        )
    }

    @Test
    fun mediaRequestEncodesSpecialPathSegmentsUnderBasePath() {
        val request = client(basePath = "/dav/photos root").buildMediaRequest("/相册 A/a+b#c%raw.jpg")

        assertEquals(
            "/dav/photos%20root/%E7%9B%B8%E5%86%8C%20A/a+b%23c%25raw.jpg",
            request.url.encodedPath,
        )
        assertEquals("Basic YWxpY2U6c2VjcmV0", request.header("Authorization"))
    }

    companion object {
        private val MULTISTATUS_PHOTOS = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/dav/Photos/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype><d:collection/></d:resourcetype>
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
                    <d:getcontenttype>image/jpeg</d:getcontenttype>
                    <d:getcontentlength>1024</d:getcontentlength>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        private val MULTISTATUS_EMPTY_SPECIAL = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/dav/photos%20root/%E7%9B%B8%E5%86%8C%20A/a%2Bb%23c%25raw/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype><d:collection/></d:resourcetype>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()
    }
}
