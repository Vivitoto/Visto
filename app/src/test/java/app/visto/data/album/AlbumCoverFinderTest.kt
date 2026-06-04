package app.visto.data.album

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import app.visto.data.webdav.WebDavClient
import app.visto.data.webdav.WebDavCredentials

class AlbumCoverFinderTest {

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

    private fun client(): WebDavClient = WebDavClient(
        credentials = WebDavCredentials(
            baseUrl = server.url("/dav").toString(),
            username = "alice",
            password = "secret",
        ),
        accountId = 42,
    )

    @Test
    fun returnsSmallestImageAtRootWhenPresent() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(207).setBody(MULTISTATUS_WITH_IMAGES))
        val cover = AlbumCoverFinder(client()).findCoverImage("/Picture")
        // small.jpg has the smallest sizeBytes (1024), big.jpg has 99999.
        assertEquals("/Picture/small.jpg", cover?.path)
    }

    @Test
    fun fallsBackToFirstSubdirectoryWhenRootHasNoImages() = runBlocking {
        // Root response: only subdirectories.
        server.enqueue(MockResponse().setResponseCode(207).setBody(MULTISTATUS_ONLY_DIRS))
        // First subdir response: one image.
        server.enqueue(MockResponse().setResponseCode(207).setBody(MULTISTATUS_SUBDIR_IMAGES))
        val cover = AlbumCoverFinder(client()).findCoverImage("/Picture")
        assertEquals("/Picture/Album1/a.jpg", cover?.path)
    }

    @Test
    fun coverIsStillReturnedEvenWhenImagesAreLargerThanLegacyCoverLimit() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(207).setBody(MULTISTATUS_WITH_IMAGES))
        // Legacy maxCoverBytes argument is now ignored: covers should
        // still surface so 16+ MB webp/png/heic can be used as a thumbnail.
        val cover = AlbumCoverFinder(client(), maxCoverBytes = 512).findCoverImage("/Picture")
        assertEquals("/Picture/small.jpg", cover?.path)
    }

    @Test
    fun returnsNullWhenProbeFindsNothing() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(207).setBody(MULTISTATUS_EMPTY))
        val cover = AlbumCoverFinder(client()).findCoverImage("/Picture")
        assertNull(cover)
    }

    companion object {
        private val MULTISTATUS_WITH_IMAGES = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/dav/Picture/</d:href>
                <d:propstat>
                  <d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/dav/Picture/big.jpg</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype/>
                    <d:getcontenttype>image/jpeg</d:getcontenttype>
                    <d:getcontentlength>99999</d:getcontentlength>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/dav/Picture/small.jpg</d:href>
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

        private val MULTISTATUS_ONLY_DIRS = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/dav/Picture/</d:href>
                <d:propstat>
                  <d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/dav/Picture/Album1/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype><d:collection/></d:resourcetype>
                    <d:displayname>Album1</d:displayname>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        private val MULTISTATUS_SUBDIR_IMAGES = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/dav/Picture/Album1/</d:href>
                <d:propstat>
                  <d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/dav/Picture/Album1/a.jpg</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype/>
                    <d:getcontenttype>image/jpeg</d:getcontenttype>
                    <d:getcontentlength>2048</d:getcontentlength>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        private val MULTISTATUS_EMPTY = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/dav/Picture/</d:href>
                <d:propstat>
                  <d:prop><d:resourcetype><d:collection/></d:resourcetype></d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()
    }
}
